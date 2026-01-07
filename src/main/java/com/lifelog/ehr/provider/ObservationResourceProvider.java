package com.lifelog.ehr.provider;

import com.lifelog.ehr.service.ObservationService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ObservationResourceProvider implements IResourceProvider {

    @Autowired
    private ObservationService observationService;

    @Override
    public Class<Observation> getResourceType() {
        return Observation.class;
    }

    @Read
    public Observation read(@IdParam IdType theId) {
        Observation observation = observationService.getObservation(theId.getIdPart());
        if (observation == null) {
            throw new ResourceNotFoundException(theId);
        }
        return observation;
    }

    @Create
    public ca.uhn.fhir.rest.api.MethodOutcome create(@ResourceParam Observation theObservation) {
        Observation created = observationService.createObservation(theObservation);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Search
    public List<Observation> search(
            @OptionalParam(name = Observation.SP_SUBJECT) ReferenceParam subject,
            @OptionalParam(name = Observation.SP_CODE) TokenParam code,
            @OptionalParam(name = "_count") ca.uhn.fhir.rest.param.NumberParam count,
            @OptionalParam(name = "_offset") ca.uhn.fhir.rest.param.NumberParam offset) {

        String subjectVal = (subject != null) ? subject.getIdPart() : null;
        String codeVal = (code != null) ? code.getValue() : null;

        int countVal = (count != null) ? count.getValue().intValue() : 10;
        int offsetVal = (offset != null) ? offset.getValue().intValue() : 0;

        return observationService.searchObservations(subjectVal, codeVal, offsetVal, countVal);
    }
}
