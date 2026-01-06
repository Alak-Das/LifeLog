package com.lifelog.ehr.repository;

import com.lifelog.ehr.model.MongoAppointment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends MongoRepository<MongoAppointment, String> {
    List<MongoAppointment> findByPatientId(String patientId);

    List<MongoAppointment> findByStatus(String status);
}
