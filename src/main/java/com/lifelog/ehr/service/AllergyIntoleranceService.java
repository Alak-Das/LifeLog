package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoAllergyIntolerance;
import com.lifelog.ehr.repository.AllergyIntoleranceRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AllergyIntoleranceService {

    @Autowired
    private AllergyIntoleranceRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private FhirContext ctx;

    public AllergyIntolerance createAllergyIntolerance(AllergyIntolerance allergy) {
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(allergy);

        MongoAllergyIntolerance mongoAllergy = new MongoAllergyIntolerance();
        if (allergy.hasIdElement() && !allergy.getIdElement().isEmpty()) {
            mongoAllergy.setId(allergy.getIdElement().getIdPart());
        }

        if (allergy.hasPatient() && allergy.getPatient().hasReference()) {
            mongoAllergy.setSubjectId(allergy.getPatient().getReferenceElement().getIdPart());
        }

        mongoAllergy.setFhirJson(json);
        mongoAllergy = repository.save(mongoAllergy);

        allergy.setId(mongoAllergy.getId());
        String finalJson = ctx.newJsonParser().encodeResourceToString(allergy);

        redisTemplate.opsForValue().set("allergyintolerance:" + mongoAllergy.getId(), finalJson,
                Duration.ofMinutes(10));

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
            ai.setId(id);
            String jsonWithId = ctx.newJsonParser().encodeResourceToString(ai);
            redisTemplate.opsForValue().set("allergyintolerance:" + id, jsonWithId, Duration.ofMinutes(10));
            return ai;
        }
        return null;
    }

    public List<AllergyIntolerance> searchAllergyIntolerances(String subjectId) {
        List<MongoAllergyIntolerance> results;
        if (subjectId != null) {
            results = repository.findBySubjectId(subjectId);
        } else {
            results = repository.findAll();
        }

        return results.stream()
                .map(alg -> {
                    AllergyIntolerance ai = ctx.newJsonParser().parseResource(AllergyIntolerance.class,
                            alg.getFhirJson());
                    ai.setId(alg.getId());
                    return ai;
                })
                .collect(Collectors.toList());
    }
}
