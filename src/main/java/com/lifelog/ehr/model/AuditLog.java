package com.lifelog.ehr.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Data
@Document(collection = "audit_logs")
public class AuditLog {
    @Id
    private String id;
    private Instant timestamp;
    private String username;
    private String operation; // e.g., READ, CREATE, SEARCH
    private String resourceType;
    private String resourceId;
    private String statusCode; // e.g., 200, 404
    private String clientIp;
    private String requestUrl;
}
