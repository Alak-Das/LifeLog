package com.al.lifelog.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_events")
public class MongoAuditEvent {
    @Id
    private String id;

    @Indexed(expireAfter = "90d") // Purge after 90 days
    private Date timestamp;

    @Indexed
    private String type; // e.g., "read", "update", "delete", "create"

    @Indexed
    private String resourceType;

    @Indexed
    private String resourceId;

    private String outcome; // "Success", "Failure"
    private String practitionerId;
    private String remoteAddr;

    private String fhirJson; // The full AuditEvent FHIR resource
}
