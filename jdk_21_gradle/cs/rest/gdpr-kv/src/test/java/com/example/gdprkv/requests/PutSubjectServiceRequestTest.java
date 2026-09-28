package com.example.gdprkv.requests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PutSubjectServiceRequestTest {

    @Test
    @DisplayName("subjectId must be non-blank")
    void subjectIdValidation() {
        assertThrows(NullPointerException.class, () -> new PutSubjectServiceRequest(null, "US", "req-123"));
        assertThrows(IllegalArgumentException.class, () -> new PutSubjectServiceRequest("  ", "US", "req-123"));
    }

    @Test
    @DisplayName("requestId must be non-blank")
    void requestIdValidation() {
        assertThrows(NullPointerException.class, () -> new PutSubjectServiceRequest("sub", "US", null));
        assertThrows(IllegalArgumentException.class, () -> new PutSubjectServiceRequest("sub", "US", "  "));
    }

    @Test
    @DisplayName("allows optional residency")
    void allowsOptionalResidency() {
        assertDoesNotThrow(() -> new PutSubjectServiceRequest("sub", null, "req-123"));
        assertDoesNotThrow(() -> new PutSubjectServiceRequest("sub", "GB", "req-456"));
    }
}
