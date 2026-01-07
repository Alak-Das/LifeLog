package com.lifelog.ehr.provider;

import com.lifelog.ehr.service.PractitionerService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Practitioner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import java.util.List;

@Component
public class PractitionerResourceProvider implements IResourceProvider {

    private final PractitionerService practitionerService;

    @Autowired
    public PractitionerResourceProvider(PractitionerService practitionerService) {
        this.practitionerService = practitionerService;
    }

    @Override
    public Class<Practitioner> getResourceType() {
        return Practitioner.class;
    }

    @Read
    public Practitioner read(@IdParam IdType theId) {
        Practitioner practitioner = practitionerService.getPractitioner(theId.getIdPart());
        if (practitioner == null) {
            throw new ResourceNotFoundException(theId);
        }
        return practitioner;
    }

    @Create
    public ca.uhn.fhir.rest.api.MethodOutcome create(@ResourceParam Practitioner thePractitioner) {
        Practitioner created = practitionerService.createPractitioner(thePractitioner);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Search
    public IBundleProvider search(
            @OptionalParam(name = Practitioner.SP_NAME) StringParam name,
            @OptionalParam(name = "_count") ca.uhn.fhir.rest.param.NumberParam count,
            @OptionalParam(name = "_offset") ca.uhn.fhir.rest.param.NumberParam offset) {

        String nameVal = (name != null) ? name.getValue() : null;

        int countVal = (count != null) ? count.getValue().intValue() : 10;
        int offsetVal = (offset != null) ? offset.getValue().intValue() : 0;

        List<Practitioner> practitioners = practitionerService.searchPractitioners(nameVal, offsetVal, countVal);
        return new SimpleBundleProvider(practitioners);
    }
}
