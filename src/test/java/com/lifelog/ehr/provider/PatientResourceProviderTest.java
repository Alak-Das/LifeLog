package com.lifelog.ehr.provider;

import com.lifelog.ehr.service.PatientService;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PatientResourceProviderTest {

    @Mock
    private PatientService patientService;

    @InjectMocks
    private PatientResourceProvider provider;

    @Test
    public void testRead_Found_ShouldReturnPatient() {
        // Setup
        String id = "123";
        Patient patient = new Patient();
        patient.setId(id);
        when(patientService.getPatient(id)).thenReturn(patient);

        // Execute
        Patient result = provider.read(new IdType(id));

        // Verify
        assertNotNull(result);
        assertEquals(id, result.getIdElement().getIdPart());
    }

    @Test
    public void testRead_NotFound_ShouldThrowException() {
        // Setup
        String id = "999";
        when(patientService.getPatient(id)).thenReturn(null);

        // Execute & Verify
        assertThrows(ResourceNotFoundException.class, () -> {
            provider.read(new IdType(id));
        });
    }

    @Test
    public void testCreate_ShouldCallService() {
        // Setup
        Patient input = new Patient();
        Patient created = new Patient();
        created.setId("123");
        when(patientService.createPatient(any(Patient.class))).thenReturn(created);

        // Execute
        var outcome = provider.create(input);

        // Verify
        assertEquals("123", outcome.getId().getIdPart());
        verify(patientService).createPatient(input);
    }

    @Test
    public void testSearch_ShouldCallService() {
        // Setup
        String name = "Doe";
        String gender = "male";
        when(patientService.searchPatients(name, gender)).thenReturn(Collections.emptyList());

        // Execute
        provider.search(new StringParam(name), new TokenParam("http://hl7.org/fhir/administrative-gender", gender));

        // Verify
        verify(patientService).searchPatients(name, gender);
    }
}
