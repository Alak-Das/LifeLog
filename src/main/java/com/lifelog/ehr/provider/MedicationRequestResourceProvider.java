package com.lifelog.ehr.provider;

import com.lifelog.ehr.service.MedicationRequestService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.MedicationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MedicationRequestResourceProvider implements IResourceProvider {

    @Autowired
    private MedicationRequestService service;

    @Override
    public Class<MedicationRequest> getResourceType() {
        return MedicationRequest.class;
    }

    @Read
    public MedicationRequest read(@IdParam IdType theId) {
        MedicationRequest request = service.getMedicationRequest(theId.getIdPart());
        if (request == null) {
            throw new ResourceNotFoundException(theId);
        }
        return request;
    }

    @Create
    public ca.uhn.fhir.rest.api.MethodOutcome create(@ResourceParam MedicationRequest theRequest) {
        MedicationRequest created = service.createMedicationRequest(theRequest);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Search
    public List<MedicationRequest> search(
            @OptionalParam(name = MedicationRequest.SP_SUBJECT) ReferenceParam subject) {
        String subjectId = (subject != null) ? subject.getIdPart() : null;
        return service.searchMedicationRequests(subjectId);
    }
}
