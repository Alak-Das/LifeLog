package com.al.lifelog.provider;

import com.al.lifelog.service.ObservationService;
import com.al.lifelog.service.ValidationService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Observation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.model.api.Include;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import com.al.lifelog.service.PatientService;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class ObservationResourceProvider implements IResourceProvider {

    @Autowired
    private ObservationService observationService;

    @Autowired
    private PatientService patientService;

    @Autowired
    private ValidationService validationService;

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
        validationService.validate(theObservation);
        Observation created = observationService.createObservation(theObservation);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Update
    public ca.uhn.fhir.rest.api.MethodOutcome update(@IdParam IdType theId, @ResourceParam Observation theObservation) {
        validationService.validate(theObservation);
        Observation updated = observationService.updateObservation(theId.getIdPart(), theObservation);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(updated.getId()));
    }

    @Delete
    public void delete(@IdParam IdType theId) {
        observationService.deleteObservation(theId.getIdPart());
    }

    @History
    public List<Observation> getHistory(@IdParam IdType theId) {
        return observationService.getHistory(theId.getIdPart());
    }

    @Search
    public IBundleProvider search(
            @OptionalParam(name = Observation.SP_SUBJECT) ReferenceParam subject,
            @OptionalParam(name = Observation.SP_CODE) TokenParam code,
            @OptionalParam(name = Observation.SP_DATE) ca.uhn.fhir.rest.param.DateRangeParam date,
            @IncludeParam Set<Include> includes,
            @OptionalParam(name = "_count") ca.uhn.fhir.rest.param.NumberParam count,
            @OptionalParam(name = "_offset") ca.uhn.fhir.rest.param.NumberParam offset) {

        List<String> subjectIds = null;

        if (subject != null) {
            String chain = subject.getChain();
            if ("name".equals(chain) || "patient.name".equals(chain)) {
                // Chained Search: Find patients by name first
                String nameVal = subject.getValue();
                List<org.hl7.fhir.r4.model.Patient> patients = patientService.searchPatients(null, nameVal, null, 0,
                        100);

                if (patients.isEmpty()) {
                    // If no patients match the name, no observations can match
                    return new SimpleBundleProvider(java.util.Collections.emptyList());
                }

                subjectIds = patients.stream()
                        .map(p -> p.getIdElement().getIdPart())
                        .collect(Collectors.toList());
            } else {
                // Normal Search: By Subject ID
                String idPart = subject.getIdPart();
                if (idPart != null) {
                    subjectIds = java.util.Collections.singletonList(idPart);
                }
            }
        }

        String codeVal = (code != null) ? code.getValue() : null;
        int countVal = (count != null) ? count.getValue().intValue() : 10;
        int offsetVal = (offset != null) ? offset.getValue().intValue() : 0;

        List<Observation> observations = observationService.searchObservations(
                subjectIds, codeVal, date,
                offsetVal, countVal);

        List<IBaseResource> resources = new java.util.ArrayList<>(observations);

        if (includes != null && !includes.isEmpty()) {
            for (Include include : includes) {
                if ("Observation:patient".equals(include.getValue())) {
                    List<String> patientIds = observations.stream()
                            .filter(o -> o.hasSubject() && o.getSubject().getReference().startsWith("Patient/"))
                            .map(o -> o.getSubject().getReference().split("/")[1])
                            .distinct()
                            .collect(Collectors.toList());
                    if (!patientIds.isEmpty()) {
                        for (String pId : patientIds) {
                            org.hl7.fhir.r4.model.Patient p = patientService.getPatient(pId);
                            if (p != null) {
                                resources.add(p);
                            }
                        }
                    }
                }
            }
        }

        return new SimpleBundleProvider(resources);
    }
}
