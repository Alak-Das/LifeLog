package com.al.lifelog.service;

import com.al.lifelog.model.MongoObservation;
import com.al.lifelog.repository.ObservationRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Counter;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.stream.Collectors;
import java.util.Date;

@Service
public class ObservationService {

    private final ObservationRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;
    private final HistoryService historyService;

    private final MeterRegistry meterRegistry;
    private final Counter observationCreatedCounter;
    private final SubscriptionService subscriptionService;

    @Autowired
    public ObservationService(ObservationRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate,
            HistoryService historyService,
            MeterRegistry meterRegistry,
            SubscriptionService subscriptionService) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
        this.historyService = historyService;
        this.meterRegistry = meterRegistry;
        this.subscriptionService = subscriptionService;
        this.observationCreatedCounter = Counter.builder("fhir.observation.created")
                .description("Total number of observations created")
                .register(meterRegistry);
    }

    @Transactional
    public Observation createObservation(Observation observation) {
        // 1. Generate ID if missing
        String id;
        if (observation.hasIdElement() && !observation.getIdElement().isEmpty()) {
            id = observation.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            observation.setId(id);
        }

        long version = 1L;
        observation.getMeta().setVersionId(String.valueOf(version));
        observation.getMeta().setLastUpdated(new java.util.Date());

        // 2. Prepare Mongo Document
        MongoObservation mongoObs = new MongoObservation();
        mongoObs.setId(id);
        mongoObs.setVersionId(version);
        mongoObs.setLastUpdated(observation.getMeta().getLastUpdated());

        // Extract Search Fields
        if (observation.hasSubject() && observation.getSubject().hasReference()) {
            // E.g. "Patient/123" -> We store "Patient/123"
            mongoObs.setSubjectId(observation.getSubject().getReference());
        }
        if (observation.hasCode() && !observation.getCode().getCoding().isEmpty()) {
            // Take the first code (e.g., LOINC)
            mongoObs.setCode(observation.getCode().getCodingFirstRep().getCode());
        }

        if (observation.hasEffectiveDateTimeType()) {
            mongoObs.setEffectiveDateTime(observation.getEffectiveDateTimeType().getValue());
        } else if (observation.hasEffectivePeriod()) {
            mongoObs.setEffectiveDateTime(observation.getEffectivePeriod().getStart());
        }

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(observation);
        mongoObs.setFhirJson(json);

        // 4. Save
        repository.save(mongoObs);
        observationCreatedCounter.increment();
        subscriptionService.notifySubscribers("Observation", "CREATE", json);

        // 5. Save History
        historyService.saveHistory(id, "Observation", json, version, mongoObs.getLastUpdated());

        // 6. Cache
        redisTemplate.opsForValue().set("observation:" + id, json, Duration.ofMinutes(10));
        return observation;
    }

    @Transactional
    public Observation updateObservation(String id, Observation observation) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("ID cannot be null or empty for update");
        }

        // Check if exists
        Optional<MongoObservation> existingOpt = repository.findById(id);
        long newVersion = 1L;

        if (existingOpt.isPresent()) {
            MongoObservation existing = existingOpt.get();
            // Optimistic Locking Check
            if (observation.hasMeta() && observation.getMeta().hasVersionId()) {
                String clientVersionStr = observation.getMeta().getVersionId();
                try {
                    long clientVersion = Long.parseLong(clientVersionStr);
                    if (existing.getVersionId() != null && clientVersion != existing.getVersionId()) {
                        throw new ca.uhn.fhir.rest.server.exceptions.PreconditionFailedException(
                                "Version conflict: Client sent version " + clientVersion +
                                        " but current version is " + existing.getVersionId());
                    }
                } catch (NumberFormatException e) {
                    // Ignore
                }
            }

            if (existing.getVersionId() != null) {
                newVersion = existing.getVersionId() + 1;
            } else {
                newVersion = 2L;
            }
        }

        // Update Metadata
        observation.getMeta().setVersionId(String.valueOf(newVersion));
        observation.getMeta().setLastUpdated(new java.util.Date());

        // Ensure ID matches
        if (observation.getIdElement().isEmpty() || !observation.getIdElement().getIdPart().equals(id)) {
            observation.setId(id);
        }

        // Prepare Mongo Document
        MongoObservation mongoObs = new MongoObservation();
        mongoObs.setId(id);
        mongoObs.setVersionId(newVersion);
        mongoObs.setLastUpdated(observation.getMeta().getLastUpdated());

        // Extract Search Fields
        if (observation.hasSubject() && observation.getSubject().hasReference()) {
            mongoObs.setSubjectId(observation.getSubject().getReference());
        }
        if (observation.hasCode() && !observation.getCode().getCoding().isEmpty()) {
            mongoObs.setCode(observation.getCode().getCodingFirstRep().getCode());
        }
        if (observation.hasEffectiveDateTimeType()) {
            mongoObs.setEffectiveDateTime(observation.getEffectiveDateTimeType().getValue());
        } else if (observation.hasEffectivePeriod()) {
            mongoObs.setEffectiveDateTime(observation.getEffectivePeriod().getStart());
        }

        // Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(observation);
        mongoObs.setFhirJson(json);

        // Save
        repository.save(mongoObs);

        // Save History
        historyService.saveHistory(id, "Observation", json, newVersion, mongoObs.getLastUpdated());

        // Cache
        redisTemplate.opsForValue().set("observation:" + id, json, Duration.ofMinutes(10));

        return observation;
    }

    @Transactional
    public void deleteObservation(String id) {
        if (id == null || id.isEmpty())
            return;

        if (repository.existsById(id)) {
            repository.deleteById(id);
            redisTemplate.delete("observation:" + id);
        }
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

    public List<Observation> searchObservations(List<String> subjects, String code,
            ca.uhn.fhir.rest.param.DateRangeParam dateRange,
            int offset, int count) {
        Query query = new Query();

        if (subjects != null && !subjects.isEmpty()) {
            List<String> searchSubjects = subjects.stream()
                    .filter(java.util.Objects::nonNull)
                    .map(s -> s.startsWith("Patient/") ? s : "Patient/" + s)
                    .collect(Collectors.toList());
            if (!searchSubjects.isEmpty()) {
                query.addCriteria(Criteria.where("subjectId").in(searchSubjects));
            }
        }

        if (code != null && !code.isEmpty()) {
            query.addCriteria(Criteria.where("code").is(code));
        }

        if (dateRange != null) {
            if (dateRange.getLowerBound() != null) {
                Date from = dateRange.getLowerBound().getValue();
                switch (dateRange.getLowerBound().getPrefix()) {
                    case GREATERTHAN:
                        query.addCriteria(Criteria.where("effectiveDateTime").gt(from));
                        break;
                    case GREATERTHAN_OR_EQUALS:
                    default:
                        query.addCriteria(Criteria.where("effectiveDateTime").gte(from));
                        break;
                }
            }
            if (dateRange.getUpperBound() != null) {
                Date to = dateRange.getUpperBound().getValue();
                switch (dateRange.getUpperBound().getPrefix()) {
                    case LESSTHAN:
                        query.addCriteria(Criteria.where("effectiveDateTime").lt(to));
                        break;
                    case LESSTHAN_OR_EQUALS:
                    default:
                        query.addCriteria(Criteria.where("effectiveDateTime").lte(to));
                        break;
                }
            }
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
        int limit = (count > 0) ? count : 10;
        int skip = (offset >= 0) ? offset : 0;

        Pageable pageable = PageRequest.of(skip / limit, limit);
        query.with(pageable);

        List<MongoObservation> results = mongoTemplate.find(query, MongoObservation.class);

        return results.stream()
                .map(mp -> {
                    if (mp.getFhirJson() == null) {
                        return null;
                    }
                    Observation o = ctx.newJsonParser().parseResource(Observation.class, mp.getFhirJson());
                    if (o.getId() == null || o.getId().isEmpty()) {
                        o.setId(mp.getId());
                    }
                    return o;
                })
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toList());
    }

    public List<Observation> getHistory(String id) {
        List<com.al.lifelog.model.MongoResourceHistory> history = historyService.getHistory(id, "Observation");
        return history.stream()
                .map(mh -> {
                    Observation o = ctx.newJsonParser().parseResource(Observation.class, mh.getFhirJson());
                    o.setId(id);
                    o.getMeta().setVersionId(String.valueOf(mh.getVersionId()));
                    o.getMeta().setLastUpdated(mh.getLastUpdated());
                    return o;
                })
                .collect(Collectors.toList());
    }
}
