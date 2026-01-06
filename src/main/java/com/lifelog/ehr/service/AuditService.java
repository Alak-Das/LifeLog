package com.lifelog.ehr.service;

import com.lifelog.ehr.model.AuditLog;
import com.lifelog.ehr.repository.AuditLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class AuditService {

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Async
    public void saveAuditLog(AuditLog log) {
        auditLogRepository.save(log);
    }
}
