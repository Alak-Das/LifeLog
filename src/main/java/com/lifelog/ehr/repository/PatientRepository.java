package com.lifelog.ehr.repository;

import com.lifelog.ehr.model.MongoPatient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PatientRepository extends MongoRepository<MongoPatient, String> {

    List<MongoPatient> findByFamilyRegexIgnoreCase(String family);

    List<MongoPatient> findByGivenRegexIgnoreCase(String given);

    // Pagination support
    Page<MongoPatient> findByFamilyRegexIgnoreCaseOrGivenRegexIgnoreCase(String family, String given,
            Pageable pageable);

    Page<MongoPatient> findByGender(String gender, Pageable pageable);
}
