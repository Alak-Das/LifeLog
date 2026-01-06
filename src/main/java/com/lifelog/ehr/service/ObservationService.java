package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoObservation;
import com.lifelog.ehr.repository.ObservationRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ObservationService {

    @Autowired
    private ObservationRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private FhirContext ctx;

    public Observation createObservation(Observation observation) {
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(observation);

        MongoObservation mongoObs = new MongoObservation();
        if (observation.hasIdElement() && !observation.getIdElement().isEmpty()) {
            mongoObs.setId(observation.getIdElement().getIdPart());
        }

        // Extract Search Fields
        if (observation.hasSubject() && observation.getSubject().hasReference()) {
            // E.g. "Patient/123" -> We store "Patient/123"
            mongoObs.setSubjectId(observation.getSubject().getReference());
        }
        if (observation.hasCode() && !observation.getCode().getCoding().isEmpty()) {
            // Take the first code (e.g., LOINC)
            mongoObs.setCode(observation.getCode().getCodingFirstRep().getCode());
        }

        mongoObs.setFhirJson(json);
        mongoObs = repository.save(mongoObs);

        observation.setId(mongoObs.getId());
        redisTemplate.opsForValue().set("observation:" + mongoObs.getId(), json, Duration.ofMinutes(10));
        return observation;
    }

    public Observation getObservation(String id) {
        String cached = redisTemplate.opsForValue().get("observation:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Observation.class, cached);
        }

        Optional<MongoObservation> result = repository.findById(id);
        if (result.isPresent()) {
            String json = result.get().getFhirJson();
            redisTemplate.opsForValue().set("observation:" + id, json, Duration.ofMinutes(10));
            return ctx.newJsonParser().parseResource(Observation.class, json);
        }
        return null;
    }

    public List<Observation> searchObservations(String subject, String code) {
        List<MongoObservation> results;

        if (subject != null) {
            // Handle "Patient/123" or just "123"
            String searchSubject = subject.startsWith("Patient/") ? subject : "Patient/" + subject;
            results = repository.findBySubjectId(searchSubject);
        } else if (code != null) {
            results = repository.findByCode(code);
        } else {
            return Collections.emptyList(); // Don't allow full table scan
        }

        return results.stream()
                .map(mp -> ctx.newJsonParser().parseResource(Observation.class, mp.getFhirJson()))
                .collect(Collectors.toList());
    }
}
