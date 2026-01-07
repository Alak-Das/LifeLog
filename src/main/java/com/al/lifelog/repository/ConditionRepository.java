package com.al.lifelog.repository;

import com.al.lifelog.model.MongoCondition;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConditionRepository extends MongoRepository<MongoCondition, String> {
    List<MongoCondition> findBySubjectId(String subjectId);

    List<MongoCondition> findBySubjectIdAndCode(String subjectId, String code);

    List<MongoCondition> findByCode(String code);
}
