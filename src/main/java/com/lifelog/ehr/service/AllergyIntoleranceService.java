package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoAllergyIntolerance;
import com.lifelog.ehr.repository.AllergyIntoleranceRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.stream.Collectors;

@Service
public class AllergyIntoleranceService {

    private final AllergyIntoleranceRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public AllergyIntoleranceService(AllergyIntoleranceRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
    }

    public AllergyIntolerance createAllergyIntolerance(AllergyIntolerance allergy) {
        // 1. Generate ID if missing
        String id;
        if (allergy.hasIdElement() && !allergy.getIdElement().isEmpty()) {
            id = allergy.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            allergy.setId(id);
        }

        // 2. Prepare Mongo Document
        MongoAllergyIntolerance mongoAllergy = new MongoAllergyIntolerance();
        mongoAllergy.setId(id);

        if (allergy.hasPatient() && allergy.getPatient().hasReference()) {
            mongoAllergy.setSubjectId(allergy.getPatient().getReferenceElement().getIdPart());
        }

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(allergy);
        mongoAllergy.setFhirJson(json);

        // 4. Save
        repository.save(mongoAllergy);

        // 5. Cache
        redisTemplate.opsForValue().set("allergyintolerance:" + id, json, Duration.ofMinutes(10));

        return allergy;
    }

    public AllergyIntolerance getAllergyIntolerance(String id) {
        String cached = redisTemplate.opsForValue().get("allergyintolerance:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(AllergyIntolerance.class, cached);
        }

        Optional<MongoAllergyIntolerance> result = repository.findById(id);
        if (result.isPresent()) {
            AllergyIntolerance ai = ctx.newJsonParser().parseResource(AllergyIntolerance.class,
                    result.get().getFhirJson());
            if (!ai.hasId()) {
                ai.setId(id);
            }
            redisTemplate.opsForValue().set("allergyintolerance:" + id, result.get().getFhirJson(),
                    Duration.ofMinutes(10));
            return ai;
        }
        return null;
    }

    public List<AllergyIntolerance> searchAllergyIntolerances(String subjectId, int offset, int count) {
        Query query = new Query();

        if (subjectId != null && !subjectId.isEmpty()) {
            query.addCriteria(Criteria.where("subjectId").is(subjectId));
        }

        if (query.getQueryObject().isEmpty()) {
            if (offset == 0 && count <= 0) {
                // Match original behavior: findAll if no criteria
            } else {
                // If they provided offset/count but no criteria, we still paginate the full
                // scan.
            }
        }

        int limit = (count > 0) ? count : 10;
        int skip = (offset >= 0) ? offset : 0;

        Pageable pageable = PageRequest.of(skip / limit, limit);
        query.with(pageable);

        List<MongoAllergyIntolerance> results = mongoTemplate.find(query, MongoAllergyIntolerance.class);

        return results.stream()
                .map(alg -> {
                    AllergyIntolerance ai = ctx.newJsonParser().parseResource(AllergyIntolerance.class,
                            alg.getFhirJson());
                    if (ai.getId() == null || ai.getId().isEmpty()) {
                        ai.setId(alg.getId());
                    }
                    return ai;
                })
                .collect(Collectors.toList());
    }
}
