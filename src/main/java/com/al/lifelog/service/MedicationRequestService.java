package com.al.lifelog.service;

import com.al.lifelog.model.MongoMedicationRequest;
import com.al.lifelog.repository.MedicationRequestRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.MedicationRequest;
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
public class MedicationRequestService {

    private final MedicationRequestRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public MedicationRequestService(MedicationRequestRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
    }

    public MedicationRequest createMedicationRequest(MedicationRequest request) {
        // 1. Generate ID if missing
        String id;
        if (request.hasIdElement() && !request.getIdElement().isEmpty()) {
            id = request.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            request.setId(id);
        }

        // 2. Prepare Mongo Document
        MongoMedicationRequest mongoRequest = new MongoMedicationRequest();
        mongoRequest.setId(id);

        if (request.hasSubject() && request.getSubject().hasReference()) {
            mongoRequest.setSubjectId(request.getSubject().getReferenceElement().getIdPart());
        }

        if (request.hasStatus()) {
            mongoRequest.setStatus(request.getStatus().toCode());
        }

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(request);
        mongoRequest.setFhirJson(json);

        // 4. Save
        repository.save(mongoRequest);

        // 5. Cache
        redisTemplate.opsForValue().set("medicationrequest:" + id, json, Duration.ofMinutes(10));

        return request;
    }

    public MedicationRequest getMedicationRequest(String id) {
        String cached = redisTemplate.opsForValue().get("medicationrequest:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(MedicationRequest.class, cached);
        }

        Optional<MongoMedicationRequest> result = repository.findById(id);
        if (result.isPresent()) {
            MedicationRequest mr = ctx.newJsonParser().parseResource(MedicationRequest.class,
                    result.get().getFhirJson());
            if (!mr.hasId()) {
                mr.setId(id);
            }
            redisTemplate.opsForValue().set("medicationrequest:" + id, result.get().getFhirJson(),
                    Duration.ofMinutes(10));
            return mr;
        }
        return null;
    }

    public List<MedicationRequest> searchMedicationRequests(String subjectId, int offset, int count) {
        Query query = new Query();

        if (subjectId != null && !subjectId.isEmpty()) {
            query.addCriteria(Criteria.where("subjectId").is(subjectId));
        }

        if (query.getQueryObject().isEmpty()) {
            if (offset == 0 && count <= 0) {
                // Match original behavior: findAll if no criteria
                // Note: this is risky for large datasets, ideally we should default to paged
                // even here.
                // But for now, let's respect the "find all" intent if no pagination requested
                // (though client should ideally paginate).
                // Actually, let's enforce pagination if they provided count, else default to
                // all??
                // The safe bet is: if count > 0, we page. If count <= 0 (unspecified?), we
                // MIGHT risk finding all?
                // Let's stick to the pattern we used for others: return EMPTY if no criteria to
                // avoid full scan.
                // BUT, the original code did "findAll".
                // We can do: query using pagination on findAll.
            } else {
                // If they provided offset/count but no criteria, we still paginate the full
                // scan.
            }
            // Logic: if criteria is empty, we just run find(query) which finds all.
            // We just need to add pagination to the query.
        }

        int limit = (count > 0) ? count : 10;
        int skip = (offset >= 0) ? offset : 0;

        Pageable pageable = PageRequest.of(skip / limit, limit);
        query.with(pageable);

        List<MongoMedicationRequest> results = mongoTemplate.find(query, MongoMedicationRequest.class);

        return results.stream()
                .map(req -> {
                    MedicationRequest mr = ctx.newJsonParser().parseResource(MedicationRequest.class,
                            req.getFhirJson());
                    if (mr.getId() == null || mr.getId().isEmpty()) {
                        mr.setId(req.getId());
                    }
                    return mr;
                })
                .collect(Collectors.toList());
    }
}
