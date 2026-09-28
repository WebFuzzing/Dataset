package com.example.gdprkv.requests;

import java.util.Objects;
import java.util.UUID;

/**
 * Service-layer command for deleting a subject (marking for erasure and tombstoning all records).
 */
public record DeleteSubjectServiceRequest(
        String subjectId,
        String requestId
) {

    public DeleteSubjectServiceRequest {
        Objects.requireNonNull(subjectId, "subjectId");
        if (subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be non-blank");
        }

        requestId = (requestId == null || requestId.isBlank()) ? UUID.randomUUID().toString() : requestId;
    }
}
