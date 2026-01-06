package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoEncounter;
import com.lifelog.ehr.repository.EncounterRepository;
import org.hl7.fhir.r4.model.Encounter;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EncounterServiceTest {

    @Mock
    private EncounterRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private EncounterService service;

    @Test
    public void testCreateEncounter() {
        Encounter enc = new Encounter();
        enc.setSubject(new Reference("Patient/123"));

        when(repository.save(any(MongoEncounter.class))).thenAnswer(i -> {
            MongoEncounter me = i.getArgument(0);
            me.setId("enc-1");
            return me;
        });
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Encounter result = service.createEncounter(enc);

        assertEquals("enc-1", result.getIdElement().getIdPart());
        verify(repository).save(any(MongoEncounter.class));
    }

    @Test
    public void testSearchEncounter() {
        MongoEncounter me = new MongoEncounter("enc-1", "{\"resourceType\":\"Encounter\", \"id\":\"enc-1\"}");
        when(repository.findBySubjectId("Patient/123")).thenReturn(List.of(me));

        List<Encounter> results = service.searchEncounters("Patient/123");

        assertEquals(1, results.size());
        verify(repository).findBySubjectId("Patient/123");
    }
}
