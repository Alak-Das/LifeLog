package com.al.lifelog.service;

import com.al.lifelog.model.MongoEncounter;
import com.al.lifelog.repository.EncounterRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.stream.Collectors;

@Service
public class EncounterService {

    private final EncounterRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;
    private final HistoryService historyService;

    @Autowired
    public EncounterService(EncounterRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate,
            HistoryService historyService) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
        this.historyService = historyService;
    }

    @Transactional
    public Encounter createEncounter(Encounter encounter) {
        // 1. Generate ID if missing
        String id;
        if (encounter.hasIdElement() && !encounter.getIdElement().isEmpty()) {
            id = encounter.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            encounter.setId(id);
        }

        long version = 1L;
        encounter.getMeta().setVersionId(String.valueOf(version));
        encounter.getMeta().setLastUpdated(new java.util.Date());

        // 2. Prepare Mongo Document
        MongoEncounter mongoEnc = new MongoEncounter();
        mongoEnc.setId(id);
        mongoEnc.setVersionId(version);
        mongoEnc.setLastUpdated(encounter.getMeta().getLastUpdated());

        if (encounter.hasSubject() && encounter.getSubject().hasReference()) {
            mongoEnc.setSubjectId(encounter.getSubject().getReference());
        }

        if (encounter.hasPeriod() && encounter.getPeriod().hasStart()) {
            mongoEnc.setPeriodStart(encounter.getPeriod().getStart());
        }

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(encounter);
        mongoEnc.setFhirJson(json);

        // 4. Save
        repository.save(mongoEnc);

        // 5. Save History
        historyService.saveHistory(id, "Encounter", json, version, mongoEnc.getLastUpdated());

        // 6. Cache
        redisTemplate.opsForValue().set("encounter:" + id, json, Duration.ofMinutes(10));
        return encounter;
    }

    @Transactional
    public Encounter updateEncounter(String id, Encounter encounter) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty for update");
        }

        // Check if exists
        Optional<MongoEncounter> existingOpt = repository.findById(id);
        long newVersion = 1L;

        if (existingOpt.isPresent()) {
            MongoEncounter existing = existingOpt.get();
            // Optimistic Locking Check
            if (encounter.hasMeta() && encounter.getMeta().hasVersionId()) {
                String clientVersionStr = encounter.getMeta().getVersionId();
                try {
                    long clientVersion = Long.parseLong(clientVersionStr);
                    if (existing.getVersionId() != null && clientVersion != existing.getVersionId()) {
                        throw new ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException(
                                "Version conflict: Client sent version " + clientVersion +
                                        " but current version is " + existing.getVersionId());
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }
            if (existing.getVersionId() != null) {
                newVersion = existing.getVersionId() + 1;
            } else {
                newVersion = 2L;
            }
        }

        // Update Metadata
        encounter.getMeta().setVersionId(String.valueOf(newVersion));
        encounter.getMeta().setLastUpdated(new java.util.Date());

        // Ensure ID matches
        if (encounter.getIdElement().isEmpty() || !encounter.getIdElement().getIdPart().equals(id)) {
            encounter.setId(id);
        }

        // Prepare Mongo Document
        MongoEncounter mongoEnc = new MongoEncounter();
        mongoEnc.setId(id);
        mongoEnc.setVersionId(newVersion);
        mongoEnc.setLastUpdated(encounter.getMeta().getLastUpdated());

        if (encounter.hasSubject() && encounter.getSubject().hasReference()) {
            mongoEnc.setSubjectId(encounter.getSubject().getReference());
        }
        if (encounter.hasPeriod() && encounter.getPeriod().hasStart()) {
            mongoEnc.setPeriodStart(encounter.getPeriod().getStart());
        }

        // Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(encounter);
        mongoEnc.setFhirJson(json);

        // Save
        repository.save(mongoEnc);

        // Save History
        historyService.saveHistory(id, "Encounter", json, newVersion, mongoEnc.getLastUpdated());

        // Cache
        redisTemplate.opsForValue().set("encounter:" + id, json, Duration.ofMinutes(10));
        return encounter;
    }

    @Transactional
    public void deleteEncounter(String id) {
        if (id == null || id.isEmpty())
            return;

        if (repository.existsById(id)) {
            repository.deleteById(id);
            redisTemplate.delete("encounter:" + id);
        }
    }

    public Encounter getEncounter(String id) {
        String cached = redisTemplate.opsForValue().get("encounter:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Encounter.class, cached);
        }

        Optional<MongoEncounter> result = repository.findById(id);
        if (result.isPresent()) {
            Encounter e = ctx.newJsonParser().parseResource(Encounter.class, result.get().getFhirJson());
            if (!e.hasId()) {
                e.setId(id);
            }
            redisTemplate.opsForValue().set("encounter:" + id, result.get().getFhirJson(), Duration.ofMinutes(10));
            return e;
        }
        return null;
    }

    public List<Encounter> searchEncounters(String subject, java.util.Date from, java.util.Date to, int offset,
            int count) {
        Query query = new Query();

        if (subject != null && !subject.isEmpty()) {
            String searchSubject = subject.startsWith("Patient/") ? subject : "Patient/" + subject;
            query.addCriteria(Criteria.where("subjectId").is(searchSubject));
        }

        if (from != null) {
            query.addCriteria(Criteria.where("periodStart").gte(from));
        }
        if (to != null) {
            query.addCriteria(Criteria.where("periodStart").lte(to));
        }

        if (query.getQueryObject().isEmpty()) {
            if (offset == 0 && count <= 0)
                return Collections.emptyList();
        }

        int limit = (count > 0) ? count : 10;
        int skip = (offset >= 0) ? offset : 0;

        Pageable pageable = PageRequest.of(skip / limit, limit);
        query.with(pageable);

        List<MongoEncounter> results = mongoTemplate.find(query, MongoEncounter.class);

        return results.stream()
                .map(me -> {
                    Encounter e = ctx.newJsonParser().parseResource(Encounter.class, me.getFhirJson());
                    if (e.getId() == null || e.getId().isEmpty()) {
                        e.setId(me.getId());
                    }
                    return e;
                })
                .collect(Collectors.toList());
    }

    public List<Encounter> getHistory(String id) {
        List<com.al.lifelog.model.MongoResourceHistory> history = historyService.getHistory(id, "Encounter");
        return history.stream()
                .map(mh -> {
                    Encounter e = ctx.newJsonParser().parseResource(Encounter.class, mh.getFhirJson());
                    e.setId(id);
                    e.getMeta().setVersionId(String.valueOf(mh.getVersionId()));
                    e.getMeta().setLastUpdated(mh.getLastUpdated());
                    return e;
                })
                .collect(Collectors.toList());
    }
}
