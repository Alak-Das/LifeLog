package com.al.lifelog.repository;

import com.al.lifelog.model.MongoMedicationRequest;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationRequestRepository extends MongoRepository<MongoMedicationRequest, String> {
    List<MongoMedicationRequest> findBySubjectId(String subjectId);

    List<MongoMedicationRequest> findByStatus(String status);
}
