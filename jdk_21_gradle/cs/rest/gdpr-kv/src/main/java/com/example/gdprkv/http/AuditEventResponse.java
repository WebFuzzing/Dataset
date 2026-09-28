package com.example.gdprkv.http;

import com.example.gdprkv.models.AuditEvent;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditEventResponse(
        @JsonProperty("subject_id") String subjectId,
        @JsonProperty("ts_ulid") String tsUlid,
        @JsonProperty("event_type") AuditEvent.EventType eventType,
        @JsonProperty("request_id") String requestId,
        @JsonProperty("timestamp") Long timestamp,
        @JsonProperty("prev_hash") String prevHash,
        @JsonProperty("hash") String hash,
        @JsonProperty("item_key") String itemKey,
        @JsonProperty("purpose") String purpose,
        @JsonProperty("details") Map<String, Object> details
) { }
