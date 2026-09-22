package com.example.gdprkv.http;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response for subject deletion requests, containing the updated subject metadata
 * and statistics about the erasure operation.
 */
public record SubjectDeletionResponse(
        @JsonProperty("subject") SubjectResponse subject,
        @JsonProperty("records_deleted") int recordsDeleted,
        @JsonProperty("total_records") int totalRecords
) {
}
