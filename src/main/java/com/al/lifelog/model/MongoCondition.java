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
@Document(collection = "conditions")
public class MongoCondition {
    @Id
    private String id;

    @Indexed
    private String subjectId; // Reference to Patient ID

    @Indexed
    private String code; // SNOMED/ICD code

    // For Optimistic Locking & History
    private Long versionId;
    private java.util.Date lastUpdated;

    private String fhirJson;

    public MongoCondition(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
