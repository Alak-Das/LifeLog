package com.lifelog.ehr.provider;

import com.lifelog.ehr.service.PatientService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PatientResourceProvider implements IResourceProvider {

    @Autowired
    private PatientService patientService;

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
        Patient created = patientService.createPatient(thePatient);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Search
    public List<Patient> search(
            @OptionalParam(name = Patient.SP_NAME) StringParam name,
            @OptionalParam(name = Patient.SP_GENDER) TokenParam gender) {
        String nameVal = (name != null) ? name.getValue() : null;
        String genderVal = (gender != null) ? gender.getValue() : null;

        return patientService.searchPatients(nameVal, genderVal);
    }
}
