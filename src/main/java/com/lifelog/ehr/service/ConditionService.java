package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoCondition;
import com.lifelog.ehr.repository.ConditionRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConditionService {

    @Autowired
    private ConditionRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final FhirContext ctx = FhirContext.forR4();

    public Condition createCondition(Condition condition) {
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(condition);

        MongoCondition mongoCond = new MongoCondition();
        if (condition.hasIdElement() && !condition.getIdElement().isEmpty()) {
            mongoCond.setId(condition.getIdElement().getIdPart());
        }

        if (condition.hasSubject() && condition.getSubject().hasReference()) {
            mongoCond.setSubjectId(condition.getSubject().getReference());
        }
        if (condition.hasCode() && !condition.getCode().getCoding().isEmpty()) {
            mongoCond.setCode(condition.getCode().getCodingFirstRep().getCode());
        }

        mongoCond.setFhirJson(json);
        mongoCond = repository.save(mongoCond);

        condition.setId(mongoCond.getId());
        redisTemplate.opsForValue().set("condition:" + mongoCond.getId(), json, Duration.ofMinutes(10));
        return condition;
    }

    public Condition getCondition(String id) {
        String cached = redisTemplate.opsForValue().get("condition:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Condition.class, cached);
        }

        Optional<MongoCondition> result = repository.findById(id);
        if (result.isPresent()) {
            String json = result.get().getFhirJson();
            redisTemplate.opsForValue().set("condition:" + id, json, Duration.ofMinutes(10));
            return ctx.newJsonParser().parseResource(Condition.class, json);
        }
        return null;
    }

    public List<Condition> searchConditions(String subject, String code) {
        if (subject == null && code == null)
            return Collections.emptyList();

        List<MongoCondition> results;
        String searchSubject = (subject != null && !subject.startsWith("Patient/")) ? "Patient/" + subject : subject;

        if (subject != null && code != null) {
            results = repository.findBySubjectIdAndCode(searchSubject, code);
        } else if (subject != null) {
            results = repository.findBySubjectId(searchSubject);
        } else {
            results = repository.findByCode(code);
        }

        return results.stream()
                .map(mc -> ctx.newJsonParser().parseResource(Condition.class, mc.getFhirJson()))
                .collect(Collectors.toList());
    }
}
