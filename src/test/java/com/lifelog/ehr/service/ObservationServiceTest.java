package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoObservation;
import com.lifelog.ehr.repository.ObservationRepository;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ObservationServiceTest {

    @Mock
    private ObservationRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private ObservationService service;

    @Test
    public void testCreateObservation_ShouldIndexFields() {
        // Setup
        Observation obs = new Observation();
        obs.setSubject(new Reference("Patient/123"));
        obs.getCode().addCoding().setCode("8867-4"); // Heart Rate

        MongoObservation savedObs = new MongoObservation();
        savedObs.setId("obs-1");
        savedObs.setFhirJson("{\"resourceType\":\"Observation\",\"id\":\"obs-1\"}");

        when(repository.save(any(MongoObservation.class))).thenAnswer(invocation -> {
            MongoObservation mo = invocation.getArgument(0);
            assertEquals("Patient/123", mo.getSubjectId());
            assertEquals("8867-4", mo.getCode());
            mo.setId("obs-1");
            return mo;
        });
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Execute
        Observation result = service.createObservation(obs);

        // Verify
        assertEquals("obs-1", result.getIdElement().getIdPart());
        verify(repository).save(any(MongoObservation.class));
    }

    @Test
    public void testSearch_BySubject_ShouldCallRepository() {
        // Setup
        String subject = "Patient/123";
        MongoObservation mo = new MongoObservation("obs-1", "{\"resourceType\":\"Observation\",\"id\":\"obs-1\"}");
        when(repository.findBySubjectId(subject)).thenReturn(List.of(mo));

        // Execute
        List<Observation> results = service.searchObservations(subject, null);

        // Verify
        assertEquals(1, results.size());
        verify(repository).findBySubjectId(subject);
    }
}
