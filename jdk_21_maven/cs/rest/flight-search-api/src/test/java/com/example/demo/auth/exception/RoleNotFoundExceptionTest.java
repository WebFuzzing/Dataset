package com.example.demo.auth.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test class for verifying the behavior of the {@link RoleNotFoundException}.
 * It checks the default constructor to ensure the correct error message is set.
 */
class RoleNotFoundExceptionTest {

    @Test
    void testConstructor_DefaultMessage() {
        RoleNotFoundException exception = new RoleNotFoundException();

        assertEquals("Role not found!\n", exception.getMessage());
    }

}