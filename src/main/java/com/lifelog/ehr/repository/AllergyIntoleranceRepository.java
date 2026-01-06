package com.lifelog.ehr.repository;

import com.lifelog.ehr.model.MongoAllergyIntolerance;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AllergyIntoleranceRepository extends MongoRepository<MongoAllergyIntolerance, String> {
    List<MongoAllergyIntolerance> findBySubjectId(String subjectId);
}
