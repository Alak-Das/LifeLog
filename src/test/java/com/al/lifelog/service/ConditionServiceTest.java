package com.al.lifelog.service;

import com.al.lifelog.model.MongoCondition;
import com.al.lifelog.repository.ConditionRepository;
import org.hl7.fhir.r4.model.Condition;
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
public class ConditionServiceTest {

    @Mock
    private ConditionRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HistoryService historyService;

    @Spy
    private FhirContext ctx = FhirContext.forR4();

    @InjectMocks
    private ConditionService service;

    @Test
    public void testCreateCondition() {
        Condition cond = new Condition();
        cond.setId("cond-1");
        cond.setSubject(new Reference("Patient/123"));
        cond.getCode().addCoding().setCode("E11"); // Diabetes

        when(repository.save(any(MongoCondition.class))).thenAnswer(i -> {
            MongoCondition mc = i.getArgument(0);
            mc.setId("cond-1");
            return mc;
        });
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        Condition result = service.createCondition(cond);

        assertEquals("cond-1", result.getIdElement().getIdPart());
        verify(repository).save(any(MongoCondition.class));
    }

    @Mock
    private org.springframework.data.mongodb.core.MongoTemplate mongoTemplate;

    @Test
    public void testSearchByCode() {
        MongoCondition mc = new MongoCondition("cond-1", "{\"resourceType\":\"Condition\", \"id\":\"cond-1\"}");

        when(mongoTemplate.find(any(org.springframework.data.mongodb.core.query.Query.class), eq(MongoCondition.class)))
                .thenReturn(List.of(mc));

        List<Condition> results = service.searchConditions(null, "E11", 0, 10);

        assertEquals(1, results.size());
        verify(mongoTemplate).find(any(org.springframework.data.mongodb.core.query.Query.class),
                eq(MongoCondition.class));
    }
}
