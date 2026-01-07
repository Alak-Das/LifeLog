package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoPatient;
import com.lifelog.ehr.repository.PatientRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    @Autowired
    public PatientService(PatientRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
    }

    public Patient createPatient(Patient patient) {
        // 1. Generate ID if missing
        String id;
        if (patient.hasIdElement() && !patient.getIdElement().isEmpty()) {
            id = patient.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            patient.setId(id);
        }

        // 2. Prepare Mongo Document
        MongoPatient mongoPatient = new MongoPatient();
        mongoPatient.setId(id);

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

        // 5. Cache
        redisTemplate.opsForValue().set("patient:" + id, json, Duration.ofMinutes(10));

        return patient;
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

    public List<Patient> searchPatients(String name, String gender, int offset, int count) {
        Query query = new Query();

        if (name != null && !name.isEmpty()) {
            // Search in family OR given
            Criteria nameCriteria = new Criteria().orOperator(
                    Criteria.where("family").regex(name, "i"),
                    Criteria.where("given").regex(name, "i"));
            query.addCriteria(nameCriteria);
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
                    Patient p = ctx.newJsonParser().parseResource(Patient.class, mp.getFhirJson());
                    // Ensure runtime ID is set if absent in JSON (legacy data support)
                    if (p.getId() == null || p.getId().isEmpty()) {
                        p.setId(mp.getId());
                    }
                    return p;
                })
                .collect(Collectors.toList());
    }
}
