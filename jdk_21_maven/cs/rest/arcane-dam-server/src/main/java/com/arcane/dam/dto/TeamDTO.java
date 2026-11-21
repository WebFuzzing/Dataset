package com.arcane.dam.dto;

// MODIFIED: removed unused AWS SDK v1 DynamoDBAttribute import
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TeamDTO {
    private String id;
    private String name;
    private String description;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isEnabled;
}
