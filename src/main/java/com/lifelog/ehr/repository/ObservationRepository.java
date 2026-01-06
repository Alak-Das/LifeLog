package com.lifelog.ehr.repository;

import com.lifelog.ehr.model.MongoObservation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ObservationRepository extends MongoRepository<MongoObservation, String> {
    List<MongoObservation> findBySubjectId(String subjectId);

    List<MongoObservation> findByCode(String code);
}
