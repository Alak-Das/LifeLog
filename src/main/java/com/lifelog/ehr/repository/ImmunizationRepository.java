package com.lifelog.ehr.repository;

import com.lifelog.ehr.model.MongoImmunization;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImmunizationRepository extends MongoRepository<MongoImmunization, String> {
}
