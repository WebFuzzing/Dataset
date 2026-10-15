package com.example.demo.auth.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Custom exception thrown when an attempt is made to create a user that already exists.
 * This exception extends {@link RuntimeException} and is typically used to indicate
 * a conflict during user registration or creation.
 */
public class UserAlreadyExistException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -6595921257346957788L;

    public static final HttpStatus STATUS = HttpStatus.CONFLICT;

    private static final String DEFAULT_MESSAGE = """
            User already exist!
            """;

    /**
     * Constructs a new {@code UserAlreadyExistException} with the default message.
     */
    public UserAlreadyExistException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new {@code UserAlreadyExistException} with the default message
     * and appends a custom message.
     *
     * @param message the custom message to append to the default message.
     */
    public UserAlreadyExistException(final String message) {
        super(DEFAULT_MESSAGE + " " + message);
    }

}
