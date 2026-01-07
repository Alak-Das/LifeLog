package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoOrganization;
import com.lifelog.ehr.repository.OrganizationRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Organization;
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
public class OrganizationService {

    private final OrganizationRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public OrganizationService(OrganizationRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
    }

    public Organization createOrganization(Organization organization) {
        // 1. Generate ID if missing
        String id;
        if (organization.hasIdElement() && !organization.getIdElement().isEmpty()) {
            id = organization.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            organization.setId(id);
        }

        // 2. Prepare Mongo Document
        MongoOrganization mongoOrg = new MongoOrganization();
        mongoOrg.setId(id);

        if (organization.hasName()) {
            mongoOrg.setName(organization.getName());
        }
        if (organization.hasIdentifier()) {
            // Just take the first identifier value
            mongoOrg.setIdentifier(organization.getIdentifierFirstRep().getValue());
        }

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(organization);
        mongoOrg.setFhirJson(json);

        // 4. Save
        repository.save(mongoOrg);

        // 5. Cache
        redisTemplate.opsForValue().set("organization:" + id, json, Duration.ofMinutes(10));

        return organization;
    }

    public Organization getOrganization(String id) {
        String cached = redisTemplate.opsForValue().get("organization:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Organization.class, cached);
        }

        Optional<MongoOrganization> result = repository.findById(id);
        if (result.isPresent()) {
            Organization o = ctx.newJsonParser().parseResource(Organization.class, result.get().getFhirJson());
            if (!o.hasId()) {
                o.setId(id);
            }
            redisTemplate.opsForValue().set("organization:" + id, result.get().getFhirJson(), Duration.ofMinutes(10));
            return o;
        }
        return null;
    }

    public List<Organization> searchOrganizations(String name, int offset, int count) {
        Query query = new Query();

        if (name != null && !name.isEmpty()) {
            query.addCriteria(Criteria.where("name").regex(name, "i"));
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

        List<MongoOrganization> results = mongoTemplate.find(query, MongoOrganization.class);

        return results.stream()
                .map(org -> {
                    Organization o = ctx.newJsonParser().parseResource(Organization.class, org.getFhirJson());
                    if (o.getId() == null || o.getId().isEmpty()) {
                        o.setId(org.getId());
                    }
                    return o;
                })
                .collect(Collectors.toList());
    }
}
