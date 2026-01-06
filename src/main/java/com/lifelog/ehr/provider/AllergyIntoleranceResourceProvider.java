package com.lifelog.ehr.provider;

import com.lifelog.ehr.service.AllergyIntoleranceService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.AllergyIntolerance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AllergyIntoleranceResourceProvider implements IResourceProvider {

    @Autowired
    private AllergyIntoleranceService service;

    @Override
    public Class<AllergyIntolerance> getResourceType() {
        return AllergyIntolerance.class;
    }

    @Read
    public AllergyIntolerance read(@IdParam IdType theId) {
        AllergyIntolerance allergy = service.getAllergyIntolerance(theId.getIdPart());
        if (allergy == null) {
            throw new ResourceNotFoundException(theId);
        }
        return allergy;
    }

    @Create
    public ca.uhn.fhir.rest.api.MethodOutcome create(@ResourceParam AllergyIntolerance theAllergy) {
        AllergyIntolerance created = service.createAllergyIntolerance(theAllergy);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Search
    public List<AllergyIntolerance> search(
            @OptionalParam(name = AllergyIntolerance.SP_PATIENT) ReferenceParam patient) {
        String patientId = (patient != null) ? patient.getIdPart() : null;
        return service.searchAllergyIntolerances(patientId);
    }
}
