package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoCondition;
import com.lifelog.ehr.repository.ConditionRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Condition;
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
public class ConditionService {

    private final ConditionRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public ConditionService(ConditionRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
    }

    public Condition createCondition(Condition condition) {
        // 1. Generate ID if missing
        String id;
        if (condition.hasIdElement() && !condition.getIdElement().isEmpty()) {
            id = condition.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            condition.setId(id);
        }

        // 2. Prepare Mongo Document
        MongoCondition mongoCond = new MongoCondition();
        mongoCond.setId(id);

        if (condition.hasSubject() && condition.getSubject().hasReference()) {
            mongoCond.setSubjectId(condition.getSubject().getReference());
        }
        if (condition.hasCode() && !condition.getCode().getCoding().isEmpty()) {
            mongoCond.setCode(condition.getCode().getCodingFirstRep().getCode());
        }

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(condition);
        mongoCond.setFhirJson(json);

        // 4. Save
        repository.save(mongoCond);

        // 5. Cache
        redisTemplate.opsForValue().set("condition:" + id, json, Duration.ofMinutes(10));
        return condition;
    }

    public Condition getCondition(String id) {
        String cached = redisTemplate.opsForValue().get("condition:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Condition.class, cached);
        }

        Optional<MongoCondition> result = repository.findById(id);
        if (result.isPresent()) {
            Condition c = ctx.newJsonParser().parseResource(Condition.class, result.get().getFhirJson());
            if (!c.hasId()) {
                c.setId(id);
            }
            redisTemplate.opsForValue().set("condition:" + id, result.get().getFhirJson(), Duration.ofMinutes(10));
            return c;
        }
        return null;
    }

    public List<Condition> searchConditions(String subject, String code, int offset, int count) {
        Query query = new Query();

        if (subject != null && !subject.isEmpty()) {
            String searchSubject = subject.startsWith("Patient/") ? subject : "Patient/" + subject;
            query.addCriteria(Criteria.where("subjectId").is(searchSubject));
        }

        if (code != null && !code.isEmpty()) {
            query.addCriteria(Criteria.where("code").is(code));
        }

        if (query.getQueryObject().isEmpty()) {
            if (offset == 0 && count <= 0)
                return Collections.emptyList();
        }

        int limit = (count > 0) ? count : 10;
        int skip = (offset >= 0) ? offset : 0;

        Pageable pageable = PageRequest.of(skip / limit, limit);
        query.with(pageable);

        List<MongoCondition> results = mongoTemplate.find(query, MongoCondition.class);

        return results.stream()
                .map(mc -> {
                    Condition c = ctx.newJsonParser().parseResource(Condition.class, mc.getFhirJson());
                    if (c.getId() == null || c.getId().isEmpty()) {
                        c.setId(mc.getId());
                    }
                    return c;
                })
                .collect(Collectors.toList());
    }
}
