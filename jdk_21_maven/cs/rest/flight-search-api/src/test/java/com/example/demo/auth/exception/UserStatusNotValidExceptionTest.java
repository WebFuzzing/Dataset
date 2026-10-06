package com.example.demo.auth.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for verifying the behavior of the {@link UserStatusNotValidException}.
 * It checks the default constructor to ensure the correct error message is set.
 */
class UserStatusNotValidExceptionTest {

    @Test
    void testConstructor_DefaultMessage() {
        UserStatusNotValidException exception = new UserStatusNotValidException();

        assertEquals("User status is not valid!\n", exception.getMessage());
    }

}