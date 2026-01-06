package com.lifelog.ehr.repository;

import com.lifelog.ehr.model.MongoPatient;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends MongoRepository<MongoPatient, String> {

    List<MongoPatient> findByFamilyRegexIgnoreCase(String family);

    List<MongoPatient> findByGivenRegexIgnoreCase(String given);

    List<MongoPatient> findByGender(String gender);

    // Fallback for name search on either given or family
    List<MongoPatient> findByFamilyRegexIgnoreCaseOrGivenRegexIgnoreCase(String family, String given);
}
