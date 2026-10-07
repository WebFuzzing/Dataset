package com.example.demo.auth.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for verifying the behavior of the {@link TokenAlreadyInvalidatedException}.
 * It checks the default constructor to ensure the correct error message is set.
 */
class TokenAlreadyInvalidatedExceptionTest {

    @Test
    void testConstructor_DefaultMessage() {
        TokenAlreadyInvalidatedException exception = new TokenAlreadyInvalidatedException();

        assertEquals("Token is already invalidated!\n", exception.getMessage());
    }

}