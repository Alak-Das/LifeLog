package com.al.lifelog.service;

import com.al.lifelog.model.MongoAuditEvent;
import com.al.lifelog.repository.AuditRepository;
import ca.uhn.fhir.context.FhirContext;
import org.hl7.fhir.r4.model.AuditEvent;
import org.hl7.fhir.r4.model.Reference;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.Date;
import java.util.UUID;

@Service
public class AuditService {

    private final AuditRepository repository;
    private final FhirContext ctx;

    @Autowired
    public AuditService(AuditRepository repository, FhirContext ctx) {
        this.repository = repository;
        this.ctx = ctx;
    }

    @Async
    public void log(String type, String resourceType, String resourceId, String outcome, String practitionerId,
            String remoteAddr) {
        AuditEvent audit = new AuditEvent();
        audit.setId(UUID.randomUUID().toString());
        audit.setRecorded(new Date());

        // Map to FHIR AuditEvent fields
        audit.getType().setSystem("http://terminology.hl7.org/CodeSystem/audit-event-type").setCode(type);

        if ("Success".equalsIgnoreCase(outcome)) {
            audit.setOutcome(AuditEvent.AuditEventOutcome._0);
        } else {
            audit.setOutcome(AuditEvent.AuditEventOutcome._4); // Generic failure
        }

        if (practitionerId != null) {
            audit.addAgent().setWho(new Reference("Practitioner/" + practitionerId));
        }

        MongoAuditEvent mongoAudit = new MongoAuditEvent();
        mongoAudit.setId(audit.getId());
        mongoAudit.setTimestamp(audit.getRecorded());
        mongoAudit.setType(type);
        mongoAudit.setResourceType(resourceType);
        mongoAudit.setResourceId(resourceId);
        mongoAudit.setOutcome(outcome);
        mongoAudit.setPractitionerId(practitionerId);
        mongoAudit.setRemoteAddr(remoteAddr);
        mongoAudit.setFhirJson(ctx.newJsonParser().encodeResourceToString(audit));

        repository.save(mongoAudit);
    }
}
