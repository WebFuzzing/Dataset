package com.arcane.dam.entity;

// MODIFIED: migrated from AWS SDK v1 DynamoDB annotations to AWS SDK v2 Enhanced Client annotations
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@DynamoDbBean
public class Team {
    private String id;
    private String name;
    private String description;
    private List<String> tags;
    private Instant createdAt;
    private Instant updatedAt;
    private Boolean isEnabled;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("id")
    public String getId() {
        return id;
    }

    @DynamoDbAttribute("name")
    public String getName() {
        return name;
    }

    @DynamoDbAttribute("description")
    public String getDescription() {
        return description;
    }

    @DynamoDbAttribute("tags")
    public List<String> getTags() {
        return tags;
    }

    @DynamoDbAttribute("created_at")
    public Instant getCreatedAt() {
        return createdAt;
    }

    @DynamoDbAttribute("updated_at")
    public Instant getUpdatedAt() {
        return updatedAt;
    }

    @DynamoDbAttribute("is_enabled")
    public Boolean getIsEnabled() {
        return isEnabled;
    }
}
