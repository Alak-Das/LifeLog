package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoImmunization;
import com.lifelog.ehr.repository.ImmunizationRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Immunization;
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
public class ImmunizationService {

    private final ImmunizationRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public ImmunizationService(ImmunizationRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
    }

    public Immunization createImmunization(Immunization immunization) {
        // 1. Generate ID if missing
        String id;
        if (immunization.hasIdElement() && !immunization.getIdElement().isEmpty()) {
            id = immunization.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            immunization.setId(id);
        }

        // 2. Prepare Mongo Document
        MongoImmunization mongoImm = new MongoImmunization();
        mongoImm.setId(id);

        if (immunization.hasPatient() && immunization.getPatient().hasReference()) {
            mongoImm.setPatientId(immunization.getPatient().getReferenceElement().getIdPart());
        }

        if (immunization.hasStatus()) {
            mongoImm.setStatus(immunization.getStatus().toCode());
        }

        if (immunization.hasVaccineCode() && !immunization.getVaccineCode().getCoding().isEmpty()) {
            mongoImm.setVaccineCode(immunization.getVaccineCode().getCodingFirstRep().getCode());
        }

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(immunization);
        mongoImm.setFhirJson(json);

        // 4. Save
        repository.save(mongoImm);

        // 5. Cache
        redisTemplate.opsForValue().set("immunization:" + id, json, Duration.ofMinutes(10));

        return immunization;
    }

    public Immunization getImmunization(String id) {
        String cached = redisTemplate.opsForValue().get("immunization:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Immunization.class, cached);
        }

        Optional<MongoImmunization> result = repository.findById(id);
        if (result.isPresent()) {
            Immunization i = ctx.newJsonParser().parseResource(Immunization.class, result.get().getFhirJson());
            if (!i.hasId()) {
                i.setId(id);
            }
            redisTemplate.opsForValue().set("immunization:" + id, result.get().getFhirJson(), Duration.ofMinutes(10));
            return i;
        }
        return null;
    }

    public List<Immunization> searchImmunizations(String patientId, String vaccineCode, int offset, int count) {
        Query query = new Query();

        if (patientId != null && !patientId.isEmpty()) {
            query.addCriteria(Criteria.where("patientId").is(patientId));
        }

        if (vaccineCode != null && !vaccineCode.isEmpty()) {
            query.addCriteria(Criteria.where("vaccineCode").is(vaccineCode));
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

        List<MongoImmunization> results = mongoTemplate.find(query, MongoImmunization.class);

        return results.stream()
                .map(imm -> {
                    Immunization i = ctx.newJsonParser().parseResource(Immunization.class, imm.getFhirJson());
                    if (i.getId() == null || i.getId().isEmpty()) {
                        i.setId(imm.getId());
                    }
                    return i;
                })
                .collect(Collectors.toList());
    }
}
