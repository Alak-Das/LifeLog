package com.al.lifelog.provider;

import com.al.lifelog.service.PatientService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Observation;
import com.al.lifelog.service.ObservationService;
import com.al.lifelog.service.ValidationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import ca.uhn.fhir.model.api.Include;
import org.hl7.fhir.instance.model.api.IBaseResource;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PatientResourceProvider implements IResourceProvider {

    @Autowired
    private PatientService patientService;

    @Autowired
    private ObservationService observationService;

    @Autowired
    private ValidationService validationService;

    @Override
    public Class<Patient> getResourceType() {
        return Patient.class;
    }

    @Read
    public Patient read(@IdParam IdType theId) {
        Patient patient = patientService.getPatient(theId.getIdPart());
        if (patient == null) {
            throw new ResourceNotFoundException(theId);
        }
        return patient;
    }

    @Create
    public ca.uhn.fhir.rest.api.MethodOutcome create(@ResourceParam Patient thePatient) {
        validationService.validate(thePatient);
        Patient created = patientService.createPatient(thePatient);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Update
    public ca.uhn.fhir.rest.api.MethodOutcome update(@IdParam IdType theId, @ResourceParam Patient thePatient) {
        validationService.validate(thePatient);
        Patient updated = patientService.updatePatient(theId.getIdPart(), thePatient);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(updated.getId()));
    }

    @Delete
    public ca.uhn.fhir.rest.api.MethodOutcome delete(@IdParam IdType theId) {
        patientService.deletePatient(theId.getIdPart());
        return new ca.uhn.fhir.rest.api.MethodOutcome(theId);
    }

    @History
    public List<Patient> getHistory(@IdParam IdType theId) {
        return patientService.getHistory(theId.getIdPart());
    }

    @Search
    public IBundleProvider search(
            @OptionalParam(name = Patient.SP_RES_ID) TokenParam id,
            @OptionalParam(name = Patient.SP_NAME) StringParam name,
            @OptionalParam(name = Patient.SP_GENDER) TokenParam gender,
            @IncludeParam Set<Include> includes,
            @IncludeParam(reverse = true) Set<Include> revIncludes,
            @Offset Integer offset,
            @Count Integer count) {
        String idVal = (id != null) ? id.getValue() : null;
        String nameVal = (name != null) ? name.getValue() : null;
        String genderVal = (gender != null) ? gender.getValue() : null;

        // Defaults
        int offsetVal = (offset != null) ? offset : 0;
        int countVal = (count != null) ? count : 10;

        List<Patient> patients = patientService.searchPatients(idVal, nameVal, genderVal, offsetVal, countVal);
        List<IBaseResource> resources = new java.util.ArrayList<>(patients);

        // Handle _include (Forward)
        if (includes != null && !includes.isEmpty()) {
            for (Include include : includes) {
                if ("Patient:observation".equals(include.getValue())) {
                    List<String> patientIds = patients.stream()
                            .map(p -> p.getIdElement().getIdPart())
                            .collect(Collectors.toList());
                    if (!patientIds.isEmpty()) {
                        List<Observation> observations = observationService.searchObservations(patientIds, null, null,
                                0, 100);
                        resources.addAll(observations);
                    }
                }
            }
        }

        // Handle _revinclude (Reverse)
        if (revIncludes != null && !revIncludes.isEmpty()) {
            for (Include revInclude : revIncludes) {
                if ("Observation:patient".equals(revInclude.getValue())) {
                    List<String> patientIds = patients.stream()
                            .map(p -> p.getIdElement().getIdPart())
                            .collect(Collectors.toList());
                    if (!patientIds.isEmpty()) {
                        List<Observation> observations = observationService.searchObservations(patientIds, null, null,
                                0, 100);
                        // Filter out if already added by _include? HAPI handles duplicates usually if
                        // we used a Bundle but here we are just adding to a list.
                        // For simplicity, let's just add.
                        resources.addAll(observations);
                    }
                }
            }
        }
        return new SimpleBundleProvider(resources);
    }
}
