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
@Document(collection = "encounters")
public class MongoEncounter {
    @Id
    private String id;

    @Indexed
    private String subjectId; // Reference to Patient ID

    private String fhirJson;

    public MongoEncounter(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
