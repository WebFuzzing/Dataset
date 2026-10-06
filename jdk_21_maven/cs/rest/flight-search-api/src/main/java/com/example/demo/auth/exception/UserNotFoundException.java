package com.example.demo.auth.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Custom exception thrown when a requested user is not found in the system.
 * This exception extends {@link RuntimeException} and is typically used to indicate
 * that a user-related query has failed due to the absence of the user.
 */
public class UserNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 6032856035243860910L;

    public static final HttpStatus STATUS = HttpStatus.NOT_FOUND;

    private static final String DEFAULT_MESSAGE = """
            User not found!
            """;

    /**
     * Constructs a new {@code UserNotFoundException} with the default message.
     */
    public UserNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new {@code UserNotFoundException} with the default message
     * and appends a custom message.
     *
     * @param message the custom message to append to the default message.
     */
    public UserNotFoundException(final String message) {
        super(DEFAULT_MESSAGE + " " + message);
    }

}
