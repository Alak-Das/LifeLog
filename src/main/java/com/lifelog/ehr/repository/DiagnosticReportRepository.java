package com.lifelog.ehr.repository;

import com.lifelog.ehr.model.MongoDiagnosticReport;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiagnosticReportRepository extends MongoRepository<MongoDiagnosticReport, String> {
}
