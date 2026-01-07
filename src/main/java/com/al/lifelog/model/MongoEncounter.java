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
@Document(collection = "encounters")
@org.springframework.data.mongodb.core.index.CompoundIndex(name = "by_subject_date", def = "{'subjectId': 1, 'periodStart': -1}")
public class MongoEncounter {
    @Id
    private String id;

    @Indexed
    private String subjectId; // Reference to Patient ID

    @Indexed
    private java.util.Date periodStart;

    // For Optimistic Locking & History
    private Long versionId;
    private java.util.Date lastUpdated;

    private String fhirJson;

    public MongoEncounter(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
