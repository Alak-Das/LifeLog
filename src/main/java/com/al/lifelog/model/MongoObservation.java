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
@Document(collection = "observations")
@org.springframework.data.mongodb.core.index.CompoundIndex(name = "by_subject_code", def = "{'subjectId': 1, 'code': 1}")
@org.springframework.data.mongodb.core.index.CompoundIndex(name = "by_subject_date", def = "{'subjectId': 1, 'effectiveDateTime': -1}")
@org.springframework.data.mongodb.core.index.CompoundIndex(name = "obs_text_index", def = "{'code': 'text'}")
public class MongoObservation {
    @Id
    private String id;

    @Indexed
    private String subjectId; // Reference to Patient ID (e.g. "Patient/123")

    @Indexed
    private String code; // LOINC code or display name

    @Indexed
    private java.util.Date effectiveDateTime;

    // For Optimistic Locking & History
    private Long versionId;
    private java.util.Date lastUpdated;

    private String fhirJson; // Store full FHIR resource as JSON string

    public MongoObservation(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
