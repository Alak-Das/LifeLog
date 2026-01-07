package com.al.lifelog.repository;

import com.al.lifelog.model.MongoAuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface AuditRepository extends MongoRepository<MongoAuditEvent, String> {
}
