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
@Document(collection = "organizations")
public class MongoOrganization {
    @Id
    private String id;

    @Indexed
    private String name;

    @Indexed
    private String identifier; // e.g. NPI or Tax ID

    private String fhirJson;

    public MongoOrganization(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
