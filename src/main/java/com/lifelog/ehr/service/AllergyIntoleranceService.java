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
        redisTemplate.opsForValue().set("allergyintolerance:" + mongoAllergy.getId(), json, Duration.ofMinutes(10));

        return allergy;
    }

    public AllergyIntolerance getAllergyIntolerance(String id) {
        String cached = redisTemplate.opsForValue().get("allergyintolerance:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(AllergyIntolerance.class, cached);
        }

        Optional<MongoAllergyIntolerance> result = repository.findById(id);
        if (result.isPresent()) {
            String json = result.get().getFhirJson();
            redisTemplate.opsForValue().set("allergyintolerance:" + id, json, Duration.ofMinutes(10));
            return ctx.newJsonParser().parseResource(AllergyIntolerance.class, json);
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
                .map(alg -> ctx.newJsonParser().parseResource(AllergyIntolerance.class, alg.getFhirJson()))
                .collect(Collectors.toList());
    }
}
