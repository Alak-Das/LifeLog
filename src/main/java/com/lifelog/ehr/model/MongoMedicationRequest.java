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
@Document(collection = "medication_requests")
public class MongoMedicationRequest {
    @Id
    private String id;

    @Indexed
    private String subjectId; // Reference to Patient

    @Indexed
    private String status;

    private String fhirJson;

    public MongoMedicationRequest(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
