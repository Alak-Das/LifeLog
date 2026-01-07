package com.lifelog.ehr.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "immunizations")
public class MongoImmunization {
    @Id
    private String id;

    @Indexed
    private String patientId; // Reference to Patient

    @Indexed
    private String status;

    @Indexed
    private String vaccineCode; // CVX or SNOMED code

    private String fhirJson;

    public MongoImmunization(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
