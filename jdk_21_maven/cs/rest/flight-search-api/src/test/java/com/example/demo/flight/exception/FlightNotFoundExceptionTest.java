package com.example.demo.flight.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Test class for verifying the behavior of the {@link FlightNotFoundException}.
 * This class contains unit tests to validate that the exception behaves as expected
 * when constructed with default and custom messages.
 */
class FlightNotFoundExceptionTest {

    @Test
    void testDefaultConstructor() {

        // Act
        FlightNotFoundException exception = new FlightNotFoundException();

        // Assert
        assertNotNull(exception);
        assertEquals("Flight not found!\n", exception.getMessage());

    }

}