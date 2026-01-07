package com.al.lifelog.provider;

import com.al.lifelog.service.ImmunizationService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Immunization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import java.util.List;

@Component
public class ImmunizationResourceProvider implements IResourceProvider {

    private final ImmunizationService immunizationService;

    @Autowired
    public ImmunizationResourceProvider(ImmunizationService immunizationService) {
        this.immunizationService = immunizationService;
    }

    @Override
    public Class<Immunization> getResourceType() {
        return Immunization.class;
    }

    @Read
    public Immunization read(@IdParam IdType theId) {
        Immunization immunization = immunizationService.getImmunization(theId.getIdPart());
        if (immunization == null) {
            throw new ResourceNotFoundException(theId);
        }
        return immunization;
    }

    @Create
    public ca.uhn.fhir.rest.api.MethodOutcome create(@ResourceParam Immunization theImmunization) {
        Immunization created = immunizationService.createImmunization(theImmunization);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Search
    public IBundleProvider search(
            @OptionalParam(name = Immunization.SP_PATIENT) StringParam patient,
            @OptionalParam(name = Immunization.SP_VACCINE_CODE) StringParam vaccineCode,
            @OptionalParam(name = "_count") ca.uhn.fhir.rest.param.NumberParam count,
            @OptionalParam(name = "_offset") ca.uhn.fhir.rest.param.NumberParam offset) {

        String patientVal = (patient != null) ? patient.getValue() : null;
        String vaccineCodeVal = (vaccineCode != null) ? vaccineCode.getValue() : null;

        int countVal = (count != null) ? count.getValue().intValue() : 10;
        int offsetVal = (offset != null) ? offset.getValue().intValue() : 0;

        List<Immunization> immunizations = immunizationService.searchImmunizations(patientVal, vaccineCodeVal,
                offsetVal, countVal);
        return new SimpleBundleProvider(immunizations);
    }
}
