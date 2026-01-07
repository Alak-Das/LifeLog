package com.al.lifelog.provider;

import com.al.lifelog.service.AppointmentService;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Appointment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AppointmentResourceProvider implements IResourceProvider {

    @Autowired
    private AppointmentService service;

    @Override
    public Class<Appointment> getResourceType() {
        return Appointment.class;
    }

    @Read
    public Appointment read(@IdParam IdType theId) {
        Appointment appointment = service.getAppointment(theId.getIdPart());
        if (appointment == null) {
            throw new ResourceNotFoundException(theId);
        }
        return appointment;
    }

    @Create
    public ca.uhn.fhir.rest.api.MethodOutcome create(@ResourceParam Appointment theAppointment) {
        Appointment created = service.createAppointment(theAppointment);
        return new ca.uhn.fhir.rest.api.MethodOutcome(new IdType(created.getId()));
    }

    @Search
    public List<Appointment> search(
            @OptionalParam(name = Appointment.SP_ACTOR) ReferenceParam actor,
            @OptionalParam(name = "_count") ca.uhn.fhir.rest.param.NumberParam count,
            @OptionalParam(name = "_offset") ca.uhn.fhir.rest.param.NumberParam offset) {

        String patientId = (actor != null) ? actor.getIdPart() : null;

        int countVal = (count != null) ? count.getValue().intValue() : 10;
        int offsetVal = (offset != null) ? offset.getValue().intValue() : 0;

        return service.searchAppointments(patientId, offsetVal, countVal);
    }
}
