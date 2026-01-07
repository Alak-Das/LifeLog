package com.al.lifelog.repository;

import com.al.lifelog.model.MongoImmunization;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ImmunizationRepository extends MongoRepository<MongoImmunization, String> {
}
