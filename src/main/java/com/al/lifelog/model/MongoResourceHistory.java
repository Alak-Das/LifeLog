package com.al.lifelog.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "resource_history")
public class MongoResourceHistory {
    @Id
    private String id;

    @Indexed
    private String resourceId;

    @Indexed
    private String resourceType;

    private Long versionId;

    @Indexed(expireAfter = "90d") // Purge after 90 days
    private Date lastUpdated;

    private String fhirJson;
}
