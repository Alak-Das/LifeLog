package com.lifelog.ehr.service;

import com.lifelog.ehr.model.AuditLog;
import com.lifelog.ehr.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    @Autowired
    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Async
    public void saveAuditLog(AuditLog log) {
        auditLogRepository.save(log);
    }
}
