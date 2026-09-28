package com.example.gdprkv.models;


import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;

class SubjectTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private static final String FIXTURE_PATH = "/fixtures/subject.json";

    private static String readFixture(String path) throws IOException {
        try (InputStream in = SubjectTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing test fixture: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Deserialize fixture → Subject populated correctly")
    void deserializeFixture() throws Exception {
        String json = readFixture(FIXTURE_PATH);
        Subject s = MAPPER.readValue(json, Subject.class);

        assertEquals("sub_123", s.getSubjectId());
        assertEquals(1725412345000L, s.getCreatedAt());
        assertEquals("test-req", s.getRequestId());
        assertEquals("EU", s.getResidency());
        assertTrue(Boolean.TRUE.equals(s.getErasureInProgress()));
        assertEquals(1725419999000L, s.getErasureRequestedAt());
    }

    @Test
    @DisplayName("Serialize → matches fixture semantically")
    void serializeMatchesFixture() throws Exception {
        String expectedJson = readFixture(FIXTURE_PATH);
        JsonNode expected = MAPPER.readTree(expectedJson);

        Subject s = Subject.builder()
                .subjectId("sub_123")
                .createdAt(1725412345000L)
                .residency("EU")
                .erasureInProgress(true)
                .erasureRequestedAt(1725419999000L)
                .requestId("test-req")
                .build();

        JsonNode actual = MAPPER.readTree(MAPPER.writeValueAsString(s));
        assertEquals(expected, actual);
    }

    @Test
    @DisplayName("Builder enforces @NonNull on required fields")
    void builderEnforcesNonNull() {
        assertThrows(NullPointerException.class, () -> Subject.builder().build());
        assertThrows(NullPointerException.class, () -> Subject.builder().subjectId("x").build());
        assertThrows(NullPointerException.class, () -> Subject.builder().subjectId("x").createdAt(1L).build());
        assertDoesNotThrow(() -> Subject.builder().subjectId("x").createdAt(1L).requestId("req").build());
    }

    @Test
    @DisplayName("No-arg constructor + setters path works")
    void noArgCtorAndSetters() {
        Subject s = new Subject();
        assertDoesNotThrow(() -> {
            s.setSubjectId("id");
            s.setCreatedAt(42L);
        });
        assertEquals("id", s.getSubjectId());
        assertEquals(42L, s.getCreatedAt());
    }

    @Test
    @DisplayName("DynamoDB annotations present with expected attribute names")
    void dynamoAnnotationsPresent() throws Exception {
        Method getSubjectId = Subject.class.getMethod("getSubjectId");
        Method getCreatedAt = Subject.class.getMethod("getCreatedAt");
        assertNotNull(getSubjectId.getAnnotation(DynamoDbPartitionKey.class));
        assertEquals("subject_id",
                getSubjectId.getAnnotation(DynamoDbAttribute.class).value());
        assertEquals("created_at",
                getCreatedAt.getAnnotation(DynamoDbAttribute.class).value());
    }
}
