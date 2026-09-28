package com.example.gdprkv.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

class PolicyTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private static final String FIXTURE = "/fixtures/policy.json";

    private static ValidatorFactory factory;
    private static Validator validator;

    private static String readFixture(String path) throws IOException {
        try (InputStream in = PolicyTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing test fixture: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @BeforeAll
    static void initValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (factory != null) {
            factory.close();
        }
    }

    @Test
    @DisplayName("Builder produces valid Policy and JSON round-trip succeeds")
    void builderAndJsonRoundTrip() throws IOException {
        String fixtureJson = readFixture(FIXTURE);
        Policy policy = MAPPER.readValue(fixtureJson, Policy.class);

        String json = MAPPER.writeValueAsString(policy);
        JsonNode node = MAPPER.readTree(json);
        assertEquals(policy.getPurpose(), node.get("purpose").asText());
        assertEquals(policy.getRetentionDays(), node.get("retention_days").asInt());
        assertEquals(policy.getDescription(), node.get("description").asText());
        assertEquals(policy.getLastUpdatedAt(), node.get("last_updated_at").asLong());

        Policy roundTrip = MAPPER.readValue(json, Policy.class);
        assertEquals(policy.getPurpose(), roundTrip.getPurpose());
        assertEquals(policy.getRetentionDays(), roundTrip.getRetentionDays());
        assertEquals(policy.getDescription(), roundTrip.getDescription());
        assertEquals(policy.getLastUpdatedAt(), roundTrip.getLastUpdatedAt());
    }

    @Test
    @DisplayName("Validation rejects retentionDays <= 0")
    void validationRejectsNonPositiveRetentionDays() {
        Policy invalid = Policy.builder()
                .purpose("MARKETING")
                .retentionDays(0)
                .lastUpdatedAt(1L)
                .build();

        Set<ConstraintViolation<Policy>> violations = validator.validate(invalid);
        assertEquals(1, violations.size());
        ConstraintViolation<Policy> violation = violations.iterator().next();
        assertEquals("retentionDays", violation.getPropertyPath().toString());
    }

    @Test
    @DisplayName("Builder enforces @NonNull fields")
    void builderNullChecks() {
        assertThrows(NullPointerException.class, () -> Policy.builder().build());
        assertThrows(NullPointerException.class, () -> Policy.builder().purpose("P").build());
    }

    @Test
    @DisplayName("DynamoDb annotations set correct attribute names")
    void dynamoAnnotations() throws Exception {
        Method getPurpose = Policy.class.getMethod("getPurpose");
        Method getRetentionDays = Policy.class.getMethod("getRetentionDays");
        Method getDescription = Policy.class.getMethod("getDescription");
        Method getLastUpdatedAt = Policy.class.getMethod("getLastUpdatedAt");

        assertNotNull(getPurpose.getAnnotation(DynamoDbPartitionKey.class));
        assertEquals("purpose", getPurpose.getAnnotation(DynamoDbAttribute.class).value());
        assertEquals("retention_days", getRetentionDays.getAnnotation(DynamoDbAttribute.class).value());
        assertEquals("description", getDescription.getAnnotation(DynamoDbAttribute.class).value());
        assertEquals("last_updated_at", getLastUpdatedAt.getAnnotation(DynamoDbAttribute.class).value());
    }
}
