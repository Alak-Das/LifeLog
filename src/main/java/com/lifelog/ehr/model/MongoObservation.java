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
@Document(collection = "observations")
public class MongoObservation {
    @Id
    private String id;

    @Indexed
    private String subjectId; // Reference to Patient ID (e.g. "Patient/123")

    @Indexed
    private String code; // LOINC code or display name

    private String fhirJson; // Store full FHIR resource as JSON string

    public MongoObservation(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
