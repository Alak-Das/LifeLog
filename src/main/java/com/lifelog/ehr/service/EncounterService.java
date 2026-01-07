package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoEncounter;
import com.lifelog.ehr.repository.EncounterRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    @Autowired
    public EncounterService(EncounterRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
    }

    public Encounter createEncounter(Encounter encounter) {
        // 1. Generate ID if missing
        String id;
        if (encounter.hasIdElement() && !encounter.getIdElement().isEmpty()) {
            id = encounter.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            encounter.setId(id);
        }

        // 2. Prepare Mongo Document
        MongoEncounter mongoEnc = new MongoEncounter();
        mongoEnc.setId(id);

        if (encounter.hasSubject() && encounter.getSubject().hasReference()) {
            mongoEnc.setSubjectId(encounter.getSubject().getReference());
        }

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(encounter);
        mongoEnc.setFhirJson(json);

        // 4. Save
        repository.save(mongoEnc);

        // 5. Cache
        redisTemplate.opsForValue().set("encounter:" + id, json, Duration.ofMinutes(10));
        return encounter;
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

    public List<Encounter> searchEncounters(String subject, int offset, int count) {
        Query query = new Query();

        if (subject != null && !subject.isEmpty()) {
            String searchSubject = subject.startsWith("Patient/") ? subject : "Patient/" + subject;
            query.addCriteria(Criteria.where("subjectId").is(searchSubject));
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
}
