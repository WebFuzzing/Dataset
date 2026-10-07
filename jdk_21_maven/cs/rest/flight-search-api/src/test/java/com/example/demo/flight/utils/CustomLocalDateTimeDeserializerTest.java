package com.example.demo.flight.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link CustomLocalDateTimeDeserializer}.
 * This class is responsible for testing the functionality of the custom deserializer
 * for {@link LocalDateTime}. It uses {@link ObjectMapper} configured with the
 * {@link CustomLocalDateTimeDeserializer} to verify the deserialization logic.
 */
class CustomLocalDateTimeDeserializerTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        // Set up ObjectMapper with the custom deserializer
        objectMapper = new ObjectMapper();
        SimpleModule module = new SimpleModule();
        module.addDeserializer(LocalDateTime.class, new CustomLocalDateTimeDeserializer());
        objectMapper.registerModule(module);
    }

    @Test
    void testDeserializeValidDate() throws IOException {
        // Given a date string
        String dateString = "\"2025-01-20\""; // Representing "2025-01-20T00:00:00"

        // When deserialized
        LocalDateTime result = objectMapper.readValue(dateString, LocalDateTime.class);

        // Then the result should be a LocalDateTime at midnight (start of the day)
        assertNotNull(result);
        assertEquals(LocalDateTime.of(2025, 1, 20, 0, 0, 0, 0), result);
    }

    @Test
    void testDeserializeEmptyDate() throws IOException {
        // Given an empty date string
        String dateString = "\"\"";

        // When deserialized
        LocalDateTime result = objectMapper.readValue(dateString, LocalDateTime.class);

        // Then the result should be null
        assertNull(result);
    }

    @Test
    void testDeserializeNullDate() throws IOException {
        // Given a null date string
        String dateString = "null";

        // When deserialized
        LocalDateTime result = objectMapper.readValue(dateString, LocalDateTime.class);

        // Then the result should be null
        assertNull(result);
    }

    @Test
    void testDeserializeInvalidDate() {
        // Given an invalid date string
        String dateString = "\"invalid-date\"";

        // When deserialized
        assertThrows(DateTimeParseException.class, () -> objectMapper.readValue(dateString, LocalDateTime.class));
    }

}