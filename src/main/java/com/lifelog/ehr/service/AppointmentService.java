package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoAppointment;
import com.lifelog.ehr.repository.AppointmentRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private FhirContext ctx;

    public Appointment createAppointment(Appointment appointment) {
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(appointment);

        MongoAppointment mongoAppt = new MongoAppointment();
        if (appointment.hasIdElement() && !appointment.getIdElement().isEmpty()) {
            mongoAppt.setId(appointment.getIdElement().getIdPart());
        }

        if (appointment.hasStatus()) {
            mongoAppt.setStatus(appointment.getStatus().toCode());
        }

        // Extract Patient ID from participants
        if (appointment.hasParticipant()) {
            for (AppointmentParticipantComponent p : appointment.getParticipant()) {
                if (p.hasActor() && p.getActor().hasReference() && p.getActor().getReference().startsWith("Patient/")) {
                    mongoAppt.setPatientId(p.getActor().getReferenceElement().getIdPart());
                    break; // Just grab first patient for now
                }
            }
        }

        mongoAppt.setFhirJson(json);
        mongoAppt = repository.save(mongoAppt);

        appointment.setId(mongoAppt.getId());
        String finalJson = ctx.newJsonParser().encodeResourceToString(appointment);

        redisTemplate.opsForValue().set("appointment:" + mongoAppt.getId(), finalJson, Duration.ofMinutes(10));

        return appointment;
    }

    public Appointment getAppointment(String id) {
        String cached = redisTemplate.opsForValue().get("appointment:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Appointment.class, cached);
        }

        Optional<MongoAppointment> result = repository.findById(id);
        if (result.isPresent()) {
            Appointment a = ctx.newJsonParser().parseResource(Appointment.class, result.get().getFhirJson());
            a.setId(id);
            String jsonWithId = ctx.newJsonParser().encodeResourceToString(a);
            redisTemplate.opsForValue().set("appointment:" + id, jsonWithId, Duration.ofMinutes(10));
            return a;
        }
        return null;
    }

    public List<Appointment> searchAppointments(String patientId) {
        List<MongoAppointment> results;
        if (patientId != null) {
            results = repository.findByPatientId(patientId);
        } else {
            results = repository.findAll();
        }

        return results.stream()
                .map(appt -> {
                    Appointment a = ctx.newJsonParser().parseResource(Appointment.class, appt.getFhirJson());
                    a.setId(appt.getId());
                    return a;
                })
                .collect(Collectors.toList());
    }
}
