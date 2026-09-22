package com.example.gdprkv.requests;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

/**
 * HTTP-layer payload captured from client PUT /subjects/{id}/records/{key} requests.
 * It is limited to validated JSON fields coming off the wire before service enrichment.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record PutRecordHttpRequest(
        @JsonProperty("purpose") @NotBlank String purpose,
        @JsonProperty("value") JsonNode value
) { }
