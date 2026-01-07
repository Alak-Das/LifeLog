package com.al.lifelog.service;

import com.al.lifelog.model.MongoEncounter;
import com.al.lifelog.repository.EncounterRepository;
import org.hl7.fhir.r4.model.Encounter;
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

    @Mock
    private HistoryService historyService;

    @Spy
    private FhirContext ctx = FhirContext.forR4();

    @InjectMocks
    private EncounterService service;

    @Test
    public void testCreateEncounter() {
        Encounter enc = new Encounter();
        enc.setId("enc-1");
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

    @Mock
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Test
    public void testSearchEncounter() {
        MongoEncounter me = new MongoEncounter("enc-1", "{\"resourceType\":\"Encounter\", \"id\":\"enc-1\"}");

        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class), eq(MongoEncounter.class)))
                .thenReturn(List.of(me));

        List<Encounter> results = service.searchEncounters("Patient/123", null, null, 0, 10);

        assertEquals(1, results.size());
        verify(mongoTemplate).find(any(org.springframework.data.mongodb.core.query.Query.class),
                eq(MongoEncounter.class));
    }
}
