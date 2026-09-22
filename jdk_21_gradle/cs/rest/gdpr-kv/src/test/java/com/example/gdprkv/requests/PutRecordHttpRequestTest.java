package com.example.gdprkv.requests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PutRecordHttpRequestTest {

    private static Validator validator;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static JsonNode fixture;

    @BeforeAll
    static void setUpValidator() throws IOException {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            validator = factory.getValidator();
        }
        fixture = loadFixture();
    }

    @Test
    @DisplayName("valid request passes bean validation")
    void validRequest() {
        PutRecordHttpRequest request = new PutRecordHttpRequest(
                fixture.get("purpose").asText(),
                fixture.get("value")
        );

        Set<ConstraintViolation<PutRecordHttpRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty(), "Expected no validation errors for valid payload");
    }

    @Test
    @DisplayName("blank purpose fails bean validation")
    void blankPurposeFailsValidation() {
        PutRecordHttpRequest request = new PutRecordHttpRequest("  ", MAPPER.createObjectNode());

        Set<ConstraintViolation<PutRecordHttpRequest>> violations = validator.validate(request);

        assertEquals(1, violations.size(), "Expected a single violation for blank purpose");
        assertEquals("must not be blank", violations.iterator().next().getMessage());
    }

    @Test
    @DisplayName("JSON serialization uses expected field names")
    void jsonSerializationUsesExpectedFields() throws Exception {
        PutRecordHttpRequest request = new PutRecordHttpRequest(
                fixture.get("purpose").asText(),
                fixture.get("value")
        );

        String json = MAPPER.writeValueAsString(request);
        JsonNode node = MAPPER.readTree(json);

        assertEquals(fixture.get("purpose").asText(), node.get("purpose").asText());
        assertEquals(fixture.get("value").get("email").asText(), node.get("value").get("email").asText());
    }

    private static JsonNode loadFixture() throws IOException {
        try (InputStream stream = PutRecordHttpRequestTest.class.getClassLoader()
                .getResourceAsStream("fixtures/put_record_request.json")) {
            return MAPPER.readTree(Objects.requireNonNull(stream, "put_record_request fixture not found"));
        }
    }
}
