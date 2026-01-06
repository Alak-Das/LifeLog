package com.lifelog.ehr.repository;

import com.lifelog.ehr.model.MongoEncounter;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EncounterRepository extends MongoRepository<MongoEncounter, String> {
    List<MongoEncounter> findBySubjectId(String subjectId);
}
