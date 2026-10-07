package com.example.demo.flight.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for verifying the behavior of the {@link AirportNameAlreadyExistException}.
 * It checks the default constructor to ensure the correct error message is set.
 */
class AirportNameAlreadyExistExceptionTest {

    @Test
    void testConstructor_DefaultMessage() {

        AirportNameAlreadyExistException exception = new AirportNameAlreadyExistException();

        assertEquals("Airport with this name already exist\n", exception.getMessage());

    }

}