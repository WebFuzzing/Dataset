package com.example.demo.auth.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Custom exception thrown when a user's status is deemed invalid for a specific operation.
 * This exception extends {@link RuntimeException} and is typically used in scenarios
 * where user-related validations fail due to an invalid status.
 */
public class UserStatusNotValidException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -4774378445348925475L;

    public static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

    private static final String DEFAULT_MESSAGE = """
            User status is not valid!
            """;

    /**
     * Constructs a new {@code UserStatusNotValidException} with the default message.
     */
    public UserStatusNotValidException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new {@code UserStatusNotValidException} with the default message
     * and appends a custom message.
     *
     * @param message the custom message to append to the default message.
     */
    public UserStatusNotValidException(final String message) {
        super(DEFAULT_MESSAGE + " " + message);
    }

}
