package com.example.gdprkv.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * HTTP-layer payload for subject creation/upsert operations. Residency is optional; omitting the
 * body creates a subject with default metadata.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PutSubjectHttpRequest(
        @JsonProperty("residency") String residency
) {
}
