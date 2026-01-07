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
@Document(collection = "practitioners")
public class MongoPractitioner {
    @Id
    private String id;

    @Indexed
    private String family;

    @Indexed
    private String given;

    @Indexed
    private String identifier; // e.g. License Number or NPI

    private String fhirJson;

    public MongoPractitioner(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
