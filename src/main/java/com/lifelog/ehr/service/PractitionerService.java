package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoPractitioner;
import com.lifelog.ehr.repository.PractitionerRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Practitioner;
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
public class PractitionerService {

    private final PractitionerRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public PractitionerService(PractitionerRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
    }

    public Practitioner createPractitioner(Practitioner practitioner) {
        // 1. Generate ID if missing
        String id;
        if (practitioner.hasIdElement() && !practitioner.getIdElement().isEmpty()) {
            id = practitioner.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            practitioner.setId(id);
        }

        // 2. Prepare Mongo Document
        MongoPractitioner mongoPrac = new MongoPractitioner();
        mongoPrac.setId(id);

        if (practitioner.hasName()) {
            if (practitioner.getNameFirstRep().hasFamily()) {
                mongoPrac.setFamily(practitioner.getNameFirstRep().getFamily());
            }
            if (practitioner.getNameFirstRep().hasGiven()) {
                mongoPrac.setGiven(practitioner.getNameFirstRep().getGivenAsSingleString());
            }
        }

        if (practitioner.hasIdentifier()) {
            // Just take the first identifier value
            mongoPrac.setIdentifier(practitioner.getIdentifierFirstRep().getValue());
        }

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(practitioner);
        mongoPrac.setFhirJson(json);

        // 4. Save
        repository.save(mongoPrac);

        // 5. Cache
        redisTemplate.opsForValue().set("practitioner:" + id, json, Duration.ofMinutes(10));

        return practitioner;
    }

    public Practitioner getPractitioner(String id) {
        String cached = redisTemplate.opsForValue().get("practitioner:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Practitioner.class, cached);
        }

        Optional<MongoPractitioner> result = repository.findById(id);
        if (result.isPresent()) {
            Practitioner p = ctx.newJsonParser().parseResource(Practitioner.class, result.get().getFhirJson());
            if (!p.hasId()) {
                p.setId(id);
            }
            redisTemplate.opsForValue().set("practitioner:" + id, result.get().getFhirJson(), Duration.ofMinutes(10));
            return p;
        }
        return null;
    }

    public List<Practitioner> searchPractitioners(String name, int offset, int count) {
        Query query = new Query();

        if (name != null && !name.isEmpty()) {
            Criteria nameCriteria = new Criteria().orOperator(
                    Criteria.where("family").regex(name, "i"),
                    Criteria.where("given").regex(name, "i"));
            query.addCriteria(nameCriteria);
        }

        if (query.getQueryObject().isEmpty()) {
            if (offset == 0 && count <= 0) {
                // Match original behavior: findAll if no criteria
            } else {
                // Pagination provided with no criteria
            }
        }

        int limit = (count > 0) ? count : 10;
        int skip = (offset >= 0) ? offset : 0;

        Pageable pageable = PageRequest.of(skip / limit, limit);
        query.with(pageable);

        List<MongoPractitioner> results = mongoTemplate.find(query, MongoPractitioner.class);

        return results.stream()
                .map(prac -> {
                    Practitioner p = ctx.newJsonParser().parseResource(Practitioner.class, prac.getFhirJson());
                    if (p.getId() == null || p.getId().isEmpty()) {
                        p.setId(prac.getId());
                    }
                    return p;
                })
                .collect(Collectors.toList());
    }
}
