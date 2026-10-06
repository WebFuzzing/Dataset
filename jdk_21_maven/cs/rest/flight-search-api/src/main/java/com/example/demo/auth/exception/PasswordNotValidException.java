package com.example.demo.auth.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Custom exception thrown when a password is not valid.
 * This exception extends {@link RuntimeException} and provides a default message
 * for invalid passwords. It also allows for custom messages to be appended to the default message.
 */
public class PasswordNotValidException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -8180169011759975985L;

    public static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

    private static final String DEFAULT_MESSAGE = """
            Password is not valid!
            """;

    /**
     * Constructs a new {@code PasswordNotValidException} with the default message.
     */
    public PasswordNotValidException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new {@code PasswordNotValidException} with the default message
     * and appends a custom message.
     *
     * @param message the custom message to append to the default message.
     */
    public PasswordNotValidException(final String message) {
        super(DEFAULT_MESSAGE + " " + message);
    }

}
