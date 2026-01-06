package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoPatient;
import com.lifelog.ehr.repository.PatientRepository;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Enumerations.AdministrativeGender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.mockito.Spy;
import ca.uhn.fhir.context.FhirContext;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PatientServiceTest {

    @Mock
    private PatientRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Spy
    private FhirContext ctx = FhirContext.forR4();

    @InjectMocks
    private PatientService service;

    @Test
    public void testCreatePatient_ShouldIndexFields() {
        // Setup
        Patient patient = new Patient();
        patient.addName().setFamily("Doe").addGiven("John");
        patient.setGender(AdministrativeGender.MALE);

        MongoPatient savedMongoPatient = new MongoPatient();
        savedMongoPatient.setId("123");
        savedMongoPatient.setFhirJson("{\"resourceType\":\"Patient\",\"id\":\"123\"}");

        when(repository.save(any(MongoPatient.class))).thenAnswer(invocation -> {
            MongoPatient mp = invocation.getArgument(0);
            assertEquals("Doe", mp.getFamily()); // Verify Indexing
            assertEquals("John", mp.getGiven());
            assertEquals("male", mp.getGender());
            mp.setId("123");
            return mp;
        });
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Execute
        Patient result = service.createPatient(patient);

        // Verify
        assertEquals("123", result.getIdElement().getIdPart());
        verify(repository).save(any(MongoPatient.class));
    }

    @Test
    public void testSearchPatient_ByName_ShouldCallRepository() {
        // Setup
        String name = "Doe";
        MongoPatient mp = new MongoPatient("123", "{\"resourceType\":\"Patient\",\"id\":\"123\"}");
        int offset = 0;
        int count = 10;

        org.springframework.data.domain.Page<MongoPatient> page = new org.springframework.data.domain.PageImpl<>(
                List.of(mp));

        when(repository.findByFamilyRegexIgnoreCaseOrGivenRegexIgnoreCase(eq(name), eq(name),
                any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(page);

        // Execute
        List<Patient> results = service.searchPatients(name, null, offset, count);

        // Verify
        assertEquals(1, results.size());
        verify(repository).findByFamilyRegexIgnoreCaseOrGivenRegexIgnoreCase(eq(name), eq(name),
                any(org.springframework.data.domain.Pageable.class));
    }
}
