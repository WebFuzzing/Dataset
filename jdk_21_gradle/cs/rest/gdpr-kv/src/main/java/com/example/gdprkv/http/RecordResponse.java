package com.example.gdprkv.http;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record RecordResponse(
        @JsonProperty("subject_id") String subjectId,
        @JsonProperty("record_key") String recordKey,
        @JsonProperty("purpose") String purpose,
        @JsonProperty("value") JsonNode value,
        @JsonProperty("version") Long version,
        @JsonProperty("created_at") Long createdAt,
        @JsonProperty("updated_at") Long updatedAt,
        @JsonProperty("retention_days") Integer retentionDays
) { }
