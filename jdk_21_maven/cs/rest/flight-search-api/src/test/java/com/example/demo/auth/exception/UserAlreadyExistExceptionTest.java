package com.example.demo.auth.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for verifying the behavior of the {@link UserAlreadyExistException}.
 * It checks the default constructor to ensure the correct error message is set.
 */
class UserAlreadyExistExceptionTest {

    @Test
    void testConstructor_DefaultMessage() {
        UserAlreadyExistException exception = new UserAlreadyExistException();

        assertEquals("User already exist!\n", exception.getMessage());
    }

}