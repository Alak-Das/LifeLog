package com.al.lifelog.provider;

import com.al.lifelog.service.EncounterService;
import com.al.lifelog.service.ValidationService;
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

    @Autowired
    private ValidationService validationService;

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
        validationService.validate(theEncounter);
        Encounter created = encounterService.createEncounter(theEncounter);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Update
    public ca.uhn.fhir.rest.api.MethodOutcome update(@IdParam IdType theId, @ResourceParam Encounter theEncounter) {
        validationService.validate(theEncounter);
        Encounter updated = encounterService.updateEncounter(theId.getIdPart(), theEncounter);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(updated.getId()));
    }

    @Delete
    public ca.uhn.fhir.rest.api.MethodOutcome delete(@IdParam IdType theId) {
        encounterService.deleteEncounter(theId.getIdPart());
        return new ca.uhn.fhir.rest.api.MethodOutcome(theId);
    }

    @History
    public List<Encounter> getHistory(@IdParam IdType theId) {
        return encounterService.getHistory(theId.getIdPart());
    }

    @Search
    public List<Encounter> search(
            @OptionalParam(name = Encounter.SP_SUBJECT) ReferenceParam subject,
            @OptionalParam(name = Encounter.SP_DATE) ca.uhn.fhir.rest.param.DateRangeParam date,
            @OptionalParam(name = "_count") ca.uhn.fhir.rest.param.NumberParam count,
            @OptionalParam(name = "_offset") ca.uhn.fhir.rest.param.NumberParam offset) {

        String subjectVal = (subject != null) ? subject.getIdPart() : null;

        java.util.Date from = (date != null) ? date.getLowerBoundAsInstant() : null;
        java.util.Date to = (date != null) ? date.getUpperBoundAsInstant() : null;

        int countVal = (count != null) ? count.getValue().intValue() : 10;
        int offsetVal = (offset != null) ? offset.getValue().intValue() : 0;

        return encounterService.searchEncounters(subjectVal, from, to, offsetVal, countVal);
    }
}
