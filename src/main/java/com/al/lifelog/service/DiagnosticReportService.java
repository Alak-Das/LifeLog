package com.al.lifelog.service;

import com.al.lifelog.model.MongoDiagnosticReport;
import com.al.lifelog.repository.DiagnosticReportRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.DiagnosticReport;
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
public class DiagnosticReportService {

    private final DiagnosticReportRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public DiagnosticReportService(DiagnosticReportRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
    }

    public DiagnosticReport createDiagnosticReport(DiagnosticReport report) {
        // 1. Generate ID if missing
        String id;
        if (report.hasIdElement() && !report.getIdElement().isEmpty()) {
            id = report.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            report.setId(id);
        }

        // 2. Prepare Mongo Document
        MongoDiagnosticReport mongoReport = new MongoDiagnosticReport();
        mongoReport.setId(id);

        if (report.hasSubject() && report.getSubject().hasReference()) {
            mongoReport.setSubjectId(report.getSubject().getReferenceElement().getIdPart());
        }

        if (report.hasStatus()) {
            mongoReport.setStatus(report.getStatus().toCode());
        }

        if (report.hasCode() && !report.getCode().getCoding().isEmpty()) {
            mongoReport.setCode(report.getCode().getCodingFirstRep().getCode());
        }

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(report);
        mongoReport.setFhirJson(json);

        // 4. Save
        repository.save(mongoReport);

        // 5. Cache
        redisTemplate.opsForValue().set("diagnosticreport:" + id, json, Duration.ofMinutes(10));

        return report;
    }

    public DiagnosticReport getDiagnosticReport(String id) {
        String cached = redisTemplate.opsForValue().get("diagnosticreport:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(DiagnosticReport.class, cached);
        }

        Optional<MongoDiagnosticReport> result = repository.findById(id);
        if (result.isPresent()) {
            DiagnosticReport r = ctx.newJsonParser().parseResource(DiagnosticReport.class, result.get().getFhirJson());
            if (!r.hasId()) {
                r.setId(id);
            }
            redisTemplate.opsForValue().set("diagnosticreport:" + id, result.get().getFhirJson(),
                    Duration.ofMinutes(10));
            return r;
        }
        return null;
    }

    public List<DiagnosticReport> searchDiagnosticReports(String subjectId, String code, int offset, int count) {
        Query query = new Query();

        if (subjectId != null && !subjectId.isEmpty()) {
            query.addCriteria(Criteria.where("subjectId").is(subjectId));
        }

        if (code != null && !code.isEmpty()) {
            query.addCriteria(Criteria.where("code").is(code));
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

        List<MongoDiagnosticReport> results = mongoTemplate.find(query, MongoDiagnosticReport.class);

        return results.stream()
                .map(rep -> {
                    DiagnosticReport r = ctx.newJsonParser().parseResource(DiagnosticReport.class, rep.getFhirJson());
                    if (r.getId() == null || r.getId().isEmpty()) {
                        r.setId(rep.getId());
                    }
                    return r;
                })
                .collect(Collectors.toList());
    }
}
