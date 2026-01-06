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
@Document(collection = "allergy_intolerances")
public class MongoAllergyIntolerance {
    @Id
    private String id;

    @Indexed
    private String subjectId;

    private String fhirJson;

    public MongoAllergyIntolerance(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
