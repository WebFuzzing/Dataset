package com.arcane.dam.entity;

// MODIFIED: migrated from AWS SDK v1 DynamoDB annotations to AWS SDK v2 Enhanced Client annotations
import lombok.Data;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;
import java.util.UUID;

@DynamoDbBean
@Data
@NoArgsConstructor
public class Space {
    private String id;
    private String name;
    private String description;
    private List<String> tags;

    public Space(String id, String name, String description, List<String> tags) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.description = description;
        this.tags = tags;
    }

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
}
