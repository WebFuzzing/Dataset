package com.example.gdprkv.requests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PutRecordServiceRequestTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static RequestFixture fixture;

    @BeforeAll
    static void loadFixture() throws IOException {
        fixture = RequestFixture.load();
    }

    @Test
    @DisplayName("subjectId validation rejects null/blank")
    void subjectIdValidation() {
        assertThrows(NullPointerException.class, () -> new PutRecordServiceRequest(
                null, fixture.recordKey(), fixture.purpose(), fixture.value(), false, null, "req"));
        assertThrows(IllegalArgumentException.class, () -> new PutRecordServiceRequest(
                "  ", fixture.recordKey(), fixture.purpose(), fixture.value(), false, null, "req"));
    }

    @Test
    @DisplayName("recordKey validation rejects null/blank")
    void recordKeyValidation() {
        assertThrows(NullPointerException.class, () -> new PutRecordServiceRequest(
                fixture.subjectId(), null, fixture.purpose(), fixture.value(), false, null, "req"));
        assertThrows(IllegalArgumentException.class, () -> new PutRecordServiceRequest(
                fixture.subjectId(), " ", fixture.purpose(), fixture.value(), false, null, "req"));
    }

    @Test
    @DisplayName("purpose validation rejects null/blank")
    void purposeValidation() {
        assertThrows(NullPointerException.class, () -> new PutRecordServiceRequest(
                fixture.subjectId(), fixture.recordKey(), null, fixture.value(), false, null, "req"));
        assertThrows(IllegalArgumentException.class, () -> new PutRecordServiceRequest(
                fixture.subjectId(), fixture.recordKey(), " ", fixture.value(), false, null, "req"));
    }

    @Test
    @DisplayName("requestId auto-generates when null or blank")
    void requestIdAutoGeneration() {
        PutRecordServiceRequest fromNull = new PutRecordServiceRequest(
                fixture.subjectId(), fixture.recordKey(), fixture.purpose(), fixture.value(), false, null, null);
        PutRecordServiceRequest fromBlank = new PutRecordServiceRequest(
                fixture.subjectId(), fixture.recordKey(), fixture.purpose(), fixture.value(), false, null, "  ");

        assertDoesNotThrow(() -> new PutRecordServiceRequest(
                fixture.subjectId(), fixture.recordKey(), fixture.purpose(), fixture.value(), false, null, "explicit"));

        assertNotNull(fromNull.requestId());
        assertFalse(fromNull.requestId().isBlank());
        assertNotNull(fromBlank.requestId());
        assertFalse(fromBlank.requestId().isBlank());
    }

    @Test
    @DisplayName("valid request passes validation")
    void happyPath() {
        assertDoesNotThrow(() -> new PutRecordServiceRequest(
                fixture.subjectId(), fixture.recordKey(), fixture.purpose(), fixture.value()));
    }

    private record RequestFixture(String subjectId,
                                  String recordKey,
                                  String purpose,
                                  JsonNode value) {

        private static RequestFixture load() throws IOException {
            try (InputStream stream = PutRecordServiceRequestTest.class.getClassLoader()
                    .getResourceAsStream("fixtures/put_record_request.json")) {
                JsonNode node = MAPPER.readTree(Objects.requireNonNull(stream, "put_record_request fixture not found"));
                return new RequestFixture(
                        node.get("subject_id").asText(),
                        node.get("record_key").asText(),
                        node.get("purpose").asText(),
                        node.get("value")
                );
            }
        }
    }
}
