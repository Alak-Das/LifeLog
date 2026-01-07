package com.al.lifelog.repository;

import com.al.lifelog.model.MongoOrganization;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrganizationRepository extends MongoRepository<MongoOrganization, String> {
    List<MongoOrganization> findByNameRegexIgnoreCase(String name);
}
