package com.al.lifelog.model;

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
@org.springframework.data.mongodb.core.index.CompoundIndex(name = "by_name", def = "{'family': 1, 'given': 1}")
@org.springframework.data.mongodb.core.index.CompoundIndex(name = "name_text_index", def = "{'family': 'text', 'given': 'text'}")
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

    // For Optimistic Locking & History
    private Long versionId;
    private java.util.Date lastUpdated;

    private String fhirJson; // Store full FHIR resource as JSON string

    public MongoPatient(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
