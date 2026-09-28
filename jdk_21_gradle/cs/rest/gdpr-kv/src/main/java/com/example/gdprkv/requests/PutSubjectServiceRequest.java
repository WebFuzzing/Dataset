package com.example.gdprkv.requests;

import java.util.Objects;

public record PutSubjectServiceRequest(
        String subjectId,
        String residency,
        String requestId
) {
    public PutSubjectServiceRequest {
        Objects.requireNonNull(subjectId, "subjectId");
        if (subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be non-blank");
        }

        Objects.requireNonNull(requestId, "requestId");
        if (requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must be non-blank");
        }
    }
}
