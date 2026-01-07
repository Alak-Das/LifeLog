package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoObservation;
import com.lifelog.ehr.repository.ObservationRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Observation;
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
public class ObservationService {

    private final ObservationRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public ObservationService(ObservationRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
    }

    public Observation createObservation(Observation observation) {
        // 1. Generate ID if missing
        String id;
        if (observation.hasIdElement() && !observation.getIdElement().isEmpty()) {
            id = observation.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            observation.setId(id);
        }

        // 2. Prepare Mongo Document
        MongoObservation mongoObs = new MongoObservation();
        mongoObs.setId(id);

        // Extract Search Fields
        if (observation.hasSubject() && observation.getSubject().hasReference()) {
            // E.g. "Patient/123" -> We store "Patient/123"
            mongoObs.setSubjectId(observation.getSubject().getReference());
        }
        if (observation.hasCode() && !observation.getCode().getCoding().isEmpty()) {
            // Take the first code (e.g., LOINC)
            mongoObs.setCode(observation.getCode().getCodingFirstRep().getCode());
        }

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(observation);
        mongoObs.setFhirJson(json);

        // 4. Save
        repository.save(mongoObs);

        // 5. Cache
        redisTemplate.opsForValue().set("observation:" + id, json, Duration.ofMinutes(10));
        return observation;
    }

    public Observation getObservation(String id) {
        String cached = redisTemplate.opsForValue().get("observation:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Observation.class, cached);
        }

        Optional<MongoObservation> result = repository.findById(id);
        if (result.isPresent()) {
            Observation o = ctx.newJsonParser().parseResource(Observation.class, result.get().getFhirJson());
            if (!o.hasId()) {
                o.setId(id);
            }
            redisTemplate.opsForValue().set("observation:" + id, result.get().getFhirJson(), Duration.ofMinutes(10));
            return o;
        }
        return null;
    }

    public List<Observation> searchObservations(String subject, String code, int offset, int count) {
        Query query = new Query();

        if (subject != null && !subject.isEmpty()) {
            // Handle "Patient/123" or just "123"
            String searchSubject = subject.startsWith("Patient/") ? subject : "Patient/" + subject;
            query.addCriteria(Criteria.where("subjectId").is(searchSubject));
        }

        if (code != null && !code.isEmpty()) {
            query.addCriteria(Criteria.where("code").is(code));
        }

        if (query.getQueryObject().isEmpty()) {
            // Return empty list if no criteria, or handle differently if needed
            // Ideally we shouldn't allow unbounded wildcard searches in production
            if (offset == 0 && count <= 0)
                return Collections.emptyList();
            // If pagination is provided but no criteria, we technically could return all,
            // but let's stick to safe default of empty unless explicit "find all" intent is
            // clear.
            // For now, let's allow "find all" WITH pagination, but safe-guard against
            // accidental full table scan.
        }

        // Apply Pagination
        // Ensure count is positive to avoid errors
        int limit = (count > 0) ? count : 10;
        // Ensure offset is non-negative
        int skip = (offset >= 0) ? offset : 0;

        Pageable pageable = PageRequest.of(skip / limit, limit);
        query.with(pageable);

        List<MongoObservation> results = mongoTemplate.find(query, MongoObservation.class);

        return results.stream()
                .map(mp -> {
                    Observation o = ctx.newJsonParser().parseResource(Observation.class, mp.getFhirJson());
                    if (o.getId() == null || o.getId().isEmpty()) {
                        o.setId(mp.getId());
                    }
                    return o;
                })
                .collect(Collectors.toList());
    }
}
