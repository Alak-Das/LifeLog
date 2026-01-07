package com.al.lifelog.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "diagnostic_reports")
public class MongoDiagnosticReport {
    @Id
    private String id;

    @Indexed
    private String subjectId; // Reference to Patient

    @Indexed
    private String status;

    @Indexed
    private String code; // LOINC code for the panel/report

    private String fhirJson;

    public MongoDiagnosticReport(String id, String fhirJson) {
        this.id = id;
        this.fhirJson = fhirJson;
    }
}
