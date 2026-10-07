package com.example.demo.auth.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Custom exception thrown when a requested role is not found.
 * This exception extends {@link RuntimeException} and is typically used in scenarios
 * where a role lookup operation fails. It provides a default message and allows
 * for an additional custom message to be appended.
 */
public class RoleNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -4353606558549954783L;

    public static final HttpStatus STATUS = HttpStatus.NOT_FOUND;

    private static final String DEFAULT_MESSAGE = """
            Role not found!
            """;

    /**
     * Constructs a new {@code RoleNotFoundException} with the default message.
     */
    public RoleNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new {@code RoleNotFoundException} with the default message
     * and appends a custom message.
     *
     * @param message the custom message to append to the default message.
     */
    public RoleNotFoundException(final String message) {
        super(DEFAULT_MESSAGE + " " + message);
    }

}
