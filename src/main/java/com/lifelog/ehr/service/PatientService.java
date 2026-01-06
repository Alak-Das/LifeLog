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
        redisTemplate.opsForValue().set("patient:" + mongoPatient.getId(), json, Duration.ofMinutes(10));

        return patient;
    }

    public Patient getPatient(String id) {
        String cached = redisTemplate.opsForValue().get("patient:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Patient.class, cached);
        }

        Optional<MongoPatient> result = repository.findById(id);
        if (result.isPresent()) {
            String json = result.get().getFhirJson();
            redisTemplate.opsForValue().set("patient:" + id, json, Duration.ofMinutes(10));
            return ctx.newJsonParser().parseResource(Patient.class, json);
        }
        return null;
    }

    public List<Patient> searchPatients(String name, String gender) {
        List<MongoPatient> results;

        if (name != null) {
            // Simple logic: If name is provided, search family or given.
            // If gender is ALSO provided, we should ideally filter, but for MVP standard
            // compliance:
            // Let's do a basic name search first. (Real FHIR engines have complex criteria
            // builders)
            results = repository.findByFamilyRegexIgnoreCaseOrGivenRegexIgnoreCase(name, name);
        } else if (gender != null) {
            results = repository.findByGender(gender);
        } else {
            results = repository.findAll();
        }

        return results.stream()
                .map(mp -> ctx.newJsonParser().parseResource(Patient.class, mp.getFhirJson()))
                .collect(Collectors.toList());
    }
}
