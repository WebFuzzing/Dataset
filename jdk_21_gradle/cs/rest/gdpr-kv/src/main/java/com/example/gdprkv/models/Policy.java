package com.example.gdprkv.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

@JsonInclude(Include.NON_NULL)
@DynamoDbBean
@NoArgsConstructor                     // required by DynamoDB Enhanced Client
@AllArgsConstructor(access = AccessLevel.PRIVATE) // used by Lombok @Builder
@Builder(toBuilder = true)
@Getter @Setter
public class Policy {

    @NonNull
    @NotBlank
    private String purpose;

    @Min(1)
    private int retentionDays;

    private String description;

    @NonNull
    private Long lastUpdatedAt;

    @DynamoDbPartitionKey
    @DynamoDbAttribute("purpose")
    public String getPurpose() {
        return purpose;
    }

    @DynamoDbAttribute("retention_days")
    public int getRetentionDays() {
        return retentionDays;
    }

    @DynamoDbAttribute("description")
    public String getDescription() {
        return description;
    }

    @DynamoDbAttribute("last_updated_at")
    public Long getLastUpdatedAt() {
        return lastUpdatedAt;
    }
}
