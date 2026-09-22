package com.example.gdprkv.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SubjectResponse(
        @JsonProperty("subject_id") String subjectId,
        @JsonProperty("created_at") Long createdAt,
        @JsonProperty("residency") String residency,
        @JsonProperty("erasure_in_progress") Boolean erasureInProgress,
        @JsonProperty("erasure_requested_at") Long erasureRequestedAt
) {
    public SubjectResponse {
        if (createdAt != null && createdAt < 0) {
            throw new IllegalArgumentException("createdAt must be a positive epoch millis");
        }
        if (erasureRequestedAt != null && erasureRequestedAt < 0) {
            throw new IllegalArgumentException("erasureRequestedAt must be a positive epoch millis");
        }
    }
}
