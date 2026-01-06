package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoPatient;
import com.lifelog.ehr.repository.PatientRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

@Service
public class PatientService {

    @Autowired
    private PatientRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private FhirContext ctx;

    public Patient createPatient(Patient patient) {
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(patient);

        MongoPatient mongoPatient = new MongoPatient();
        if (patient.hasIdElement() && !patient.getIdElement().isEmpty()) {
            mongoPatient.setId(patient.getIdElement().getIdPart());
        }

        // Populate Index Fields
        if (patient.hasName()) {
            if (patient.getNameFirstRep().hasFamily()) {
                mongoPatient.setFamily(patient.getNameFirstRep().getFamily());
            }
            if (patient.getNameFirstRep().hasGiven()) {
                // Just grab the first given name for simple indexing
                mongoPatient.setGiven(patient.getNameFirstRep().getGivenAsSingleString());
            }
        }
        if (patient.hasGender()) {
            mongoPatient.setGender(patient.getGender().toCode());
        }

        mongoPatient.setFhirJson(json);
        mongoPatient = repository.save(mongoPatient);

        // Update ID in the FHIR object and cache it
        patient.setId(mongoPatient.getId());
        String finalJson = ctx.newJsonParser().encodeResourceToString(patient);

        redisTemplate.opsForValue().set("patient:" + mongoPatient.getId(), finalJson, Duration.ofMinutes(10));

        return patient;
    }

    public Patient getPatient(String id) {
        String cached = redisTemplate.opsForValue().get("patient:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Patient.class, cached);
        }

        Optional<MongoPatient> result = repository.findById(id);
        if (result.isPresent()) {
            Patient p = ctx.newJsonParser().parseResource(Patient.class, result.get().getFhirJson());
            p.setId(id);
            String jsonWithId = ctx.newJsonParser().encodeResourceToString(p);
            redisTemplate.opsForValue().set("patient:" + id, jsonWithId, Duration.ofMinutes(10));
            return p;
        }
        return null;
    }

    public List<Patient> searchPatients(String name, String gender, int offset, int count) {
        Page<MongoPatient> pageResult;
        Pageable pageable = PageRequest.of(offset / count, count);

        if (name != null) {
            pageResult = repository.findByFamilyRegexIgnoreCaseOrGivenRegexIgnoreCase(name, name, pageable);
        } else if (gender != null) {
            pageResult = repository.findByGender(gender, pageable);
        } else {
            pageResult = repository.findAll(pageable);
        }

        return pageResult.stream()
                .map(mp -> {
                    Patient p = ctx.newJsonParser().parseResource(Patient.class, mp.getFhirJson());
                    p.setId(mp.getId());
                    return p;
                })
                .collect(Collectors.toList());
    }
}
