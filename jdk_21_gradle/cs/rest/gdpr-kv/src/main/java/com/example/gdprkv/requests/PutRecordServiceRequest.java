package com.example.gdprkv.requests;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import java.util.UUID;

/**
 * Service-layer command constructed from {@link PutRecordHttpRequest} plus server-side context
 * (subject, record key, request id, tombstone metadata) needed to execute the write.
 */
public record PutRecordServiceRequest(
        String subjectId,
        String recordKey,
        String purpose,
        JsonNode value,
        boolean tombstoned,
        Long tombstonedAt,
        String requestId
) {

    public PutRecordServiceRequest(String subjectId,
                                   String recordKey,
                                   String purpose,
                                   JsonNode value) {
        this(subjectId, recordKey, purpose, value, false, null, null);
    }

    public PutRecordServiceRequest {
        Objects.requireNonNull(subjectId, "subjectId");
        if (subjectId.isBlank()) {
            throw new IllegalArgumentException("subjectId must be non-blank");
        }

        Objects.requireNonNull(recordKey, "recordKey");
        if (recordKey.isBlank()) {
            throw new IllegalArgumentException("recordKey must be non-blank");
        }

        Objects.requireNonNull(purpose, "purpose");
        if (purpose.isBlank()) {
            throw new IllegalArgumentException("purpose must be non-blank");
        }

        requestId = (requestId == null || requestId.isBlank()) ? UUID.randomUUID().toString() : requestId;
    }
}
