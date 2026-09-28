package com.example.gdprkv.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
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
@NoArgsConstructor                     // needed for DynamoDB Enhanced Client reflection
@AllArgsConstructor(access = AccessLevel.PRIVATE) // used by Lombok @Builder
@Builder(toBuilder = true)
@Getter @Setter
public class Subject {

    // Required fields — Lombok @NonNull enforces runtime null checks in builder
    @NonNull
    private String subjectId;

    @NonNull
    private Long createdAt;

    @NonNull
    private String requestId;

    // Optional fields
    private String residency;
    private Boolean erasureInProgress;
    private Long erasureRequestedAt;

    // ----- DynamoDB Enhanced annotations on getters -----

    @DynamoDbPartitionKey
    @DynamoDbAttribute("subject_id")
    public String getSubjectId() { return subjectId; }

    @DynamoDbAttribute("created_at")
    public Long getCreatedAt() { return createdAt; }

    @DynamoDbAttribute("residency")
    public String getResidency() { return residency; }

    @DynamoDbAttribute("erasure_in_progress")
    public Boolean getErasureInProgress() { return erasureInProgress; }

    @DynamoDbAttribute("erasure_requested_at")
    public Long getErasureRequestedAt() { return erasureRequestedAt; }

    @DynamoDbAttribute("request_id")
    public String getRequestId() { return requestId; }
}
