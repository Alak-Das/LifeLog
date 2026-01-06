package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoMedicationRequest;
import com.lifelog.ehr.repository.MedicationRequestRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MedicationRequestService {

    @Autowired
    private MedicationRequestRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private FhirContext ctx;

    public MedicationRequest createMedicationRequest(MedicationRequest request) {
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(request);

        MongoMedicationRequest mongoRequest = new MongoMedicationRequest();
        if (request.hasIdElement() && !request.getIdElement().isEmpty()) {
            mongoRequest.setId(request.getIdElement().getIdPart());
        }

        if (request.hasSubject() && request.getSubject().hasReference()) {
            mongoRequest.setSubjectId(request.getSubject().getReferenceElement().getIdPart());
        }

        if (request.hasStatus()) {
            mongoRequest.setStatus(request.getStatus().toCode());
        }

        mongoRequest.setFhirJson(json);
        mongoRequest = repository.save(mongoRequest);

        request.setId(mongoRequest.getId());
        redisTemplate.opsForValue().set("medicationrequest:" + mongoRequest.getId(), json, Duration.ofMinutes(10));

        return request;
    }

    public MedicationRequest getMedicationRequest(String id) {
        String cached = redisTemplate.opsForValue().get("medicationrequest:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(MedicationRequest.class, cached);
        }

        Optional<MongoMedicationRequest> result = repository.findById(id);
        if (result.isPresent()) {
            String json = result.get().getFhirJson();
            redisTemplate.opsForValue().set("medicationrequest:" + id, json, Duration.ofMinutes(10));
            return ctx.newJsonParser().parseResource(MedicationRequest.class, json);
        }
        return null;
    }

    public List<MedicationRequest> searchMedicationRequests(String subjectId) {
        List<MongoMedicationRequest> results;
        if (subjectId != null) {
            results = repository.findBySubjectId(subjectId);
        } else {
            results = repository.findAll();
        }

        return results.stream()
                .map(req -> ctx.newJsonParser().parseResource(MedicationRequest.class, req.getFhirJson()))
                .collect(Collectors.toList());
    }
}
