package com.example.gdprkv.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PutSubjectHttpRequestTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    @DisplayName("serializes residency field when provided")
    void serializesResidency() throws Exception {
        PutSubjectHttpRequest request = new PutSubjectHttpRequest("US");

        String json = MAPPER.writeValueAsString(request);
        assertEquals("US", MAPPER.readTree(json).get("residency").asText());
    }

    @Test
    @DisplayName("omits residency when null")
    void omitsNullResidency() throws Exception {
        PutSubjectHttpRequest request = new PutSubjectHttpRequest(null);

        String json = MAPPER.writeValueAsString(request);
        assertFalse(MAPPER.readTree(json).has("residency"));
    }
}
