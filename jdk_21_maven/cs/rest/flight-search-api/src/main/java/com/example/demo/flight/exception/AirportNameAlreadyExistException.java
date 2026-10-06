package com.example.demo.flight.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Custom exception thrown when an airport with the same name already exists in the system.
 * This exception is typically used when attempting to create or update an airport with a name
 * that already exists, violating the uniqueness constraint.
 */
public class AirportNameAlreadyExistException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -3495060754159297663L;

    public static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

    private static final String DEFAULT_MESSAGE = """
            Airport with this name already exist
            """;

    /**
     * Constructs a new AirportNameAlreadyExistException with the default error message.
     * This constructor is used when an airport with the same name already exists, and no additional details are provided.
     */
    public AirportNameAlreadyExistException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new AirportNameAlreadyExistException with a custom error message.
     * This constructor is used when an airport with the same name already exists, and an additional message is provided
     * to further explain the context.
     *
     * @param message the custom message to be appended to the default error message.
     */
    public AirportNameAlreadyExistException(final String message) {
        super(DEFAULT_MESSAGE + " " + message);
    }

}
