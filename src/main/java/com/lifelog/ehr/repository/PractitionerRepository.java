package com.lifelog.ehr.repository;

import com.lifelog.ehr.model.MongoPractitioner;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PractitionerRepository extends MongoRepository<MongoPractitioner, String> {
}
