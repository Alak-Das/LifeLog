package com.al.lifelog.service;

import com.al.lifelog.model.MongoPatient;
import com.al.lifelog.repository.PatientRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

import java.time.Duration;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import java.util.Optional;

@Service
public class PatientService {

    private final PatientRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;
    private final HistoryService historyService;
    private final MeterRegistry meterRegistry;
    private final Counter patientCreatedCounter;
    private final AuditService auditService;
    private final SubscriptionService subscriptionService;

    @Autowired
    public PatientService(PatientRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate,
            HistoryService historyService,
            AuditService auditService,
            MeterRegistry meterRegistry,
            SubscriptionService subscriptionService) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
        this.historyService = historyService;
        this.auditService = auditService;
        this.meterRegistry = meterRegistry;
        this.subscriptionService = subscriptionService;
        this.patientCreatedCounter = Counter.builder("fhir.patient.created")
                .description("Total number of patients created")
                .register(meterRegistry);
    }

    @Transactional
    public Patient createPatient(Patient patient) {
        // 1. Generate ID if missing
        String id;
        if (patient.hasIdElement() && !patient.getIdElement().isEmpty()) {
            id = patient.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            patient.setId(id);
        }

        // Initialize Version
        long version = 1L;
        if (patient.getMeta().hasVersionId()) {
            // If client tries to set version on create, we generally ignore or respect
            // depending on policy.
            // Here, let's enforce 1 for new creates.
        }
        patient.getMeta().setVersionId(String.valueOf(version));
        patient.getMeta().setLastUpdated(new java.util.Date());

        // 2. Prepare Mongo Document
        MongoPatient mongoPatient = new MongoPatient();
        mongoPatient.setId(id);
        mongoPatient.setVersionId(version);
        mongoPatient.setLastUpdated(patient.getMeta().getLastUpdated());

        // Populate Index Fields
        if (patient.hasName()) {
            if (patient.getNameFirstRep().hasFamily()) {
                mongoPatient.setFamily(patient.getNameFirstRep().getFamily());
            }
            if (patient.getNameFirstRep().hasGiven()) {
                mongoPatient.setGiven(patient.getNameFirstRep().getGivenAsSingleString());
            }
        }
        if (patient.hasGender()) {
            mongoPatient.setGender(patient.getGender().toCode());
        }

        // 3. Serialize with ID included
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(patient);
        mongoPatient.setFhirJson(json);

        // 4. Save
        repository.save(mongoPatient);
        patientCreatedCounter.increment();
        subscriptionService.notifySubscribers("Patient", "CREATE", json);

        // 5. Save History
        historyService.saveHistory(id, "Patient", json, version, mongoPatient.getLastUpdated());

        // 6. Cache
        redisTemplate.opsForValue().set("patient:" + id, json, Duration.ofMinutes(10));

        return patient;
    }

    @Transactional
    public Patient updatePatient(String id, Patient patient) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty for update");
        }

        // Check if exists
        Optional<MongoPatient> existingOpt = repository.findById(id);
        long newVersion = 1L;

        if (existingOpt.isPresent()) {
            MongoPatient existing = existingOpt.get();
            // Optimistic Locking Check
            // If the incoming resource has a version ID, verify it matches
            if (patient.hasMeta() && patient.getMeta().hasVersionId()) {
                String clientVersionStr = patient.getMeta().getVersionId();
                try {
                    long clientVersion = Long.parseLong(clientVersionStr);
                    if (existing.getVersionId() != null && clientVersion != existing.getVersionId()) {
                        throw new ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException(
                                "Version conflict: Client sent version " + clientVersion +
                                        " but current version is " + existing.getVersionId());
                    }
                } catch (NumberFormatException e) {
                    // Ignore malformed version
                }
            }

            // Increment version
            if (existing.getVersionId() != null) {
                newVersion = existing.getVersionId() + 1;
            } else {
                newVersion = 2L; // Fallback if legacy data had no version
            }
        }

        // Update Metadata
        patient.getMeta().setVersionId(String.valueOf(newVersion));
        patient.getMeta().setLastUpdated(new java.util.Date());

        // Ensure ID matches
        if (patient.getIdElement().isEmpty() || !patient.getIdElement().getIdPart().equals(id)) {
            patient.setId(id);
        }

        // Prepare Mongo Document (similar to create but preserving ID)
        MongoPatient mongoPatient = new MongoPatient();
        mongoPatient.setId(id);
        mongoPatient.setVersionId(newVersion);
        mongoPatient.setLastUpdated(patient.getMeta().getLastUpdated());

        // Populate Index Fields (Duplicate logic for now, ideally refactor to private
        // helper)
        if (patient.hasName()) {
            if (patient.getNameFirstRep().hasFamily()) {
                mongoPatient.setFamily(patient.getNameFirstRep().getFamily());
            }
            if (patient.getNameFirstRep().hasGiven()) {
                mongoPatient.setGiven(patient.getNameFirstRep().getGivenAsSingleString());
            }
        }
        if (patient.hasGender()) {
            mongoPatient.setGender(patient.getGender().toCode());
        }

        // Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(patient);
        mongoPatient.setFhirJson(json);

        // Save
        repository.save(mongoPatient);

        // Save History
        historyService.saveHistory(id, "Patient", json, newVersion, mongoPatient.getLastUpdated());

        // Cache
        redisTemplate.opsForValue().set("patient:" + id, json, Duration.ofMinutes(10));

        return patient;
    }

    @Transactional
    public void deletePatient(String id) {
        if (id == null || id.isEmpty()) {
            return;
        }

        Optional<MongoPatient> existing = repository.findById(id);
        if (existing.isPresent()) {
            // Delete from DB
            repository.deleteById(id);
            // Invalidate Cache
            redisTemplate.delete("patient:" + id);
            // Optionally: Create a "Deleted" Audit log or keep a "Tombstone" record in a
            // History table
        } else {
            // FHIR says delete should return 200/204 even if not found (idempotent)
            // But we might want to throw ResourceNotFound to return 404 in some contexts?
            // HAPI usually handles this. Let's just return.
        }
    }

    public Patient getPatient(String id) {
        String cached = redisTemplate.opsForValue().get("patient:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Patient.class, cached);
        }

        Optional<MongoPatient> result = repository.findById(id);
        if (result.isPresent()) {
            Patient p = ctx.newJsonParser().parseResource(Patient.class, result.get().getFhirJson());
            // Double check ID
            if (!p.hasId()) {
                p.setId(id);
            }

            // Re-cache if needed (though now we store correct JSON)
            String json = ctx.newJsonParser().encodeResourceToString(p);
            redisTemplate.opsForValue().set("patient:" + id, json, Duration.ofMinutes(10));
            return p;
        }
        return null;
    }

    public List<Patient> searchPatients(String id, String name, String gender, int offset, int count) {
        Query query = new Query();

        if (id != null && !id.isEmpty()) {
            query.addCriteria(Criteria.where("_id").is(id));
        }

        if (name != null && !name.isEmpty()) {
            // Use regex for name search as it's more flexible for partial matches in FHIR
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("family").regex(name, "i"),
                    Criteria.where("given").regex(name, "i")));
        }

        if (gender != null && !gender.isEmpty()) {
            query.addCriteria(Criteria.where("gender").is(gender));
        }

        // Pagination
        Pageable pageable = PageRequest.of(offset / count, count);
        query.with(pageable);

        List<MongoPatient> results = mongoTemplate.find(query, MongoPatient.class);

        return results.stream()
                .map(mp -> {
                    if (mp.getFhirJson() == null) {
                        return null;
                    }
                    Patient p = ctx.newJsonParser().parseResource(Patient.class, mp.getFhirJson());
                    // Ensure runtime ID is set if absent in JSON (legacy data support)
                    if (p.getId() == null || p.getId().isEmpty()) {
                        p.setId(mp.getId());
                    }
                    return p;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Patient> getHistory(String id) {
        List<com.al.lifelog.model.MongoResourceHistory> history = historyService.getHistory(id, "Patient");
        return history.stream()
                .map(mh -> {
                    Patient p = ctx.newJsonParser().parseResource(Patient.class, mh.getFhirJson());
                    p.setId(id);
                    p.getMeta().setVersionId(String.valueOf(mh.getVersionId()));
                    p.getMeta().setLastUpdated(mh.getLastUpdated());
                    return p;
                })
                .collect(Collectors.toList());
    }
}
