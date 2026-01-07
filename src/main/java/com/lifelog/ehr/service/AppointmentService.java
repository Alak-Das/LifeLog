package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoAppointment;
import com.lifelog.ehr.repository.AppointmentRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Appointment;
import org.hl7.fhir.r4.model.Appointment.AppointmentParticipantComponent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.stream.Collectors;

@Service
public class AppointmentService {

    private final AppointmentRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final FhirContext ctx;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public AppointmentService(AppointmentRepository repository,
            StringRedisTemplate redisTemplate,
            FhirContext ctx,
            MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
        this.ctx = ctx;
        this.mongoTemplate = mongoTemplate;
    }

    public Appointment createAppointment(Appointment appointment) {
        // 1. Generate ID if missing
        String id;
        if (appointment.hasIdElement() && !appointment.getIdElement().isEmpty()) {
            id = appointment.getIdElement().getIdPart();
        } else {
            id = UUID.randomUUID().toString();
            appointment.setId(id);
        }

        // 2. Prepare Mongo Document
        MongoAppointment mongoAppt = new MongoAppointment();
        mongoAppt.setId(id);

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

        // 3. Serialize
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(appointment);
        mongoAppt.setFhirJson(json);

        // 4. Save
        repository.save(mongoAppt);

        // 5. Cache
        redisTemplate.opsForValue().set("appointment:" + id, json, Duration.ofMinutes(10));

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
            if (!a.hasId()) {
                a.setId(id);
            }
            redisTemplate.opsForValue().set("appointment:" + id, result.get().getFhirJson(), Duration.ofMinutes(10));
            return a;
        }
        return null;
    }

    public List<Appointment> searchAppointments(String patientId, int offset, int count) {
        Query query = new Query();

        if (patientId != null && !patientId.isEmpty()) {
            query.addCriteria(Criteria.where("patientId").is(patientId));
        }

        if (query.getQueryObject().isEmpty()) {
            if (offset == 0 && count <= 0) {
                // Match original behavior: findAll if no criteria
            } else {
                // If pagination provided, use it
            }
        }

        int limit = (count > 0) ? count : 10;
        int skip = (offset >= 0) ? offset : 0;

        Pageable pageable = PageRequest.of(skip / limit, limit);
        query.with(pageable);

        List<MongoAppointment> results = mongoTemplate.find(query, MongoAppointment.class);

        return results.stream()
                .map(appt -> {
                    Appointment a = ctx.newJsonParser().parseResource(Appointment.class, appt.getFhirJson());
                    if (a.getId() == null || a.getId().isEmpty()) {
                        a.setId(appt.getId());
                    }
                    return a;
                })
                .collect(Collectors.toList());
    }
}
