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
@Document(collection = "patients")
public class MongoPatient {
    @Id
    private String id;

    // Search Fields
    @Indexed
    private String family;

    @Indexed
    private String given;

    @Indexed
    private String gender;

    private String fhirJson; // Store full FHIR resource as JSON string

    public MongoPatient(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
