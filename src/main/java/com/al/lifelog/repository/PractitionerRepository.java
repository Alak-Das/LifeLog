package com.al.lifelog.repository;

import com.al.lifelog.model.MongoPractitioner;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PractitionerRepository extends MongoRepository<MongoPractitioner, String> {
}
