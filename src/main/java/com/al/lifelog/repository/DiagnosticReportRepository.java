package com.al.lifelog.repository;

import com.al.lifelog.model.MongoDiagnosticReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiagnosticReportRepository extends MongoRepository<MongoDiagnosticReport, String> {
}
