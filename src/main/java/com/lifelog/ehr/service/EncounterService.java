package com.lifelog.ehr.service;

import com.lifelog.ehr.model.MongoEncounter;
import com.lifelog.ehr.repository.EncounterRepository;
import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EncounterService {

    @Autowired
    private EncounterRepository repository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private FhirContext ctx;

    public Encounter createEncounter(Encounter encounter) {
        IParser parser = ctx.newJsonParser();
        String json = parser.encodeResourceToString(encounter);

        MongoEncounter mongoEnc = new MongoEncounter();
        if (encounter.hasIdElement() && !encounter.getIdElement().isEmpty()) {
            mongoEnc.setId(encounter.getIdElement().getIdPart());
        }

        if (encounter.hasSubject() && encounter.getSubject().hasReference()) {
            mongoEnc.setSubjectId(encounter.getSubject().getReference());
        }

        mongoEnc.setFhirJson(json);
        mongoEnc = repository.save(mongoEnc);

        encounter.setId(mongoEnc.getId());
        redisTemplate.opsForValue().set("encounter:" + mongoEnc.getId(), json, Duration.ofMinutes(10));
        return encounter;
    }

    public Encounter getEncounter(String id) {
        String cached = redisTemplate.opsForValue().get("encounter:" + id);
        if (cached != null) {
            return ctx.newJsonParser().parseResource(Encounter.class, cached);
        }

        Optional<MongoEncounter> result = repository.findById(id);
        if (result.isPresent()) {
            String json = result.get().getFhirJson();
            redisTemplate.opsForValue().set("encounter:" + id, json, Duration.ofMinutes(10));
            return ctx.newJsonParser().parseResource(Encounter.class, json);
        }
        return null;
    }

    public List<Encounter> searchEncounters(String subject) {
        if (subject == null)
            return Collections.emptyList();

        String searchSubject = subject.startsWith("Patient/") ? subject : "Patient/" + subject;
        List<MongoEncounter> results = repository.findBySubjectId(searchSubject);

        return results.stream()
                .map(me -> ctx.newJsonParser().parseResource(Encounter.class, me.getFhirJson()))
                .collect(Collectors.toList());
    }
}
