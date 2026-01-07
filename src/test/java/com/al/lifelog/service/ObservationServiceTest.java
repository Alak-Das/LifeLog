package com.al.lifelog.service;

import com.al.lifelog.model.MongoObservation;
import com.al.lifelog.repository.ObservationRepository;
import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
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
public class ObservationServiceTest {

    @Mock
    private ObservationRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HistoryService historyService;

    @Mock
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Spy
    private FhirContext ctx = FhirContext.forR4();

    @InjectMocks
    private ObservationService service;

    @Test
    public void testCreateObservation_ShouldIndexFields() {
        // Setup
        Observation obs = new Observation();
        obs.setId("obs-1");
        obs.setSubject(new Reference("Patient/123"));
        obs.getCode().addCoding().setCode("8867-4"); // Heart Rate

        MongoObservation savedObs = new MongoObservation();
        savedObs.setId("obs-1");
        savedObs.setFhirJson("{\"resourceType\":\"Observation\",\"id\":\"obs-1\"}");

        when(repository.save(any(MongoObservation.class))).thenAnswer(invocation -> {
            MongoObservation mo = invocation.getArgument(0);
            assertEquals("Patient/123", mo.getSubjectId());
            assertEquals("8867-4", mo.getCode());
            assertEquals("obs-1", mo.getId());
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
    public void testUpdateObservation_ShouldUpdateAndReturnObservation() {
        // Setup
        String id = "obs-1";
        Observation obs = new Observation();
        obs.setId(id);
        obs.setSubject(new Reference("Patient/123"));

        when(repository.save(any(MongoObservation.class))).thenReturn(new MongoObservation());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // Execute
        Observation result = service.updateObservation(id, obs);

        // Verify
        assertEquals(id, result.getIdElement().getIdPart());
        verify(repository).save(any(MongoObservation.class));
    }

    @Test
    public void testSearch_BySubject_ShouldCallMongoTemplate() {
        // Setup
        String subject = "Patient/123";
        MongoObservation mo = new MongoObservation("obs-1", "{\"resourceType\":\"Observation\",\"id\":\"obs-1\"}");

        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class),
                eq(MongoObservation.class)))
                .thenReturn(List.of(mo));

        // Execute
        List<Observation> results = service.searchObservations(java.util.Collections.singletonList(subject), null, null,
                0, 10);

        // Verify
        assertEquals(1, results.size());
        verify(mongoTemplate).find(any(org.springframework.data.mongodb.core.query.Query.class),
                eq(MongoObservation.class));
    }
}
