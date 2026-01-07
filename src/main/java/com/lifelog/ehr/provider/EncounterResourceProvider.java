package com.lifelog.ehr.provider;

import com.lifelog.ehr.service.EncounterService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Encounter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EncounterResourceProvider implements IResourceProvider {

    @Autowired
    private EncounterService encounterService;

    @Override
    public Class<Encounter> getResourceType() {
        return Encounter.class;
    }

    @Read
    public Encounter read(@IdParam IdType theId) {
        Encounter encounter = encounterService.getEncounter(theId.getIdPart());
        if (encounter == null) {
            throw new ResourceNotFoundException(theId);
        }
        return encounter;
    }

    @Create
    public ca.uhn.fhir.rest.api.MethodOutcome create(@ResourceParam Encounter theEncounter) {
        Encounter created = encounterService.createEncounter(theEncounter);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Search
    public List<Encounter> search(
            @OptionalParam(name = Encounter.SP_SUBJECT) ReferenceParam subject,
            @OptionalParam(name = "_count") ca.uhn.fhir.rest.param.NumberParam count,
            @OptionalParam(name = "_offset") ca.uhn.fhir.rest.param.NumberParam offset) {

        String subjectVal = (subject != null) ? subject.getIdPart() : null;

        int countVal = (count != null) ? count.getValue().intValue() : 10;
        int offsetVal = (offset != null) ? offset.getValue().intValue() : 0;

        return encounterService.searchEncounters(subjectVal, offsetVal, countVal);
    }
}
