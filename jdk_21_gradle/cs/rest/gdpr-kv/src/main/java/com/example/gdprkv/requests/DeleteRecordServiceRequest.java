package com.example.gdprkv.requests;

import java.util.Objects;
import java.util.UUID;

/**
 * Service-layer command for deleting a record (tombstoning it for eventual purge).
 */
public record DeleteRecordServiceRequest(
        String subjectId,
        String recordKey,
        String requestId
) {

    public DeleteRecordServiceRequest(String subjectId, String recordKey) {
        this(subjectId, recordKey, null);
    }

    public DeleteRecordServiceRequest {
        Objects.requireNonNull(subjectId, "subjectId");
        if (subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be non-blank");
        }

        Objects.requireNonNull(recordKey, "recordKey");
        if (recordKey.isBlank()) {
            throw new IllegalArgumentException("recordKey must be non-blank");
        }

        requestId = (requestId == null || requestId.isBlank()) ? UUID.randomUUID().toString() : requestId;
    }
}
