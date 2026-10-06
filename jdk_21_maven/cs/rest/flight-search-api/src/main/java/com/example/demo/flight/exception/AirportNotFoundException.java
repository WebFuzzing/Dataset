package com.example.demo.flight.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Custom exception thrown when an airport is not found in the system.
 * This exception is typically used when an airport cannot be located based on the provided identifier.
 */
public class AirportNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 7222351960801029610L;

    public static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

    private static final String DEFAULT_MESSAGE = """
            Airport not found!
            """;

    /**
     * Constructs a new AirportNotFoundException with the default error message.
     * This constructor is used when an airport is not found, and no additional details are provided.
     */
    public AirportNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new AirportNotFoundException with a custom error message.
     * This constructor is used when an airport is not found, and an additional message is provided to further explain the context.
     *
     * @param message the custom message to be appended to the default error message.
     */
    public AirportNotFoundException(final String message) {
        super(DEFAULT_MESSAGE + " " + message);
    }

}
