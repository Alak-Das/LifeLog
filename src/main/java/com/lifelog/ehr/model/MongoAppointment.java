package com.lifelog.ehr.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "appointments")
public class MongoAppointment {
    @Id
    private String id;

    // In FHIR, Appointment has participants (Patient, Practitioner, Location).
    // For MVP, we'll index one 'actorId' which we assume is the Patient for now,
    // or we could store a list of actor IDs.
    // Let's store the Patient ID specifically if referenced.
    @Indexed
    private String patientId;

    @Indexed
    private String status;

    private String fhirJson;

    public MongoAppointment(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
