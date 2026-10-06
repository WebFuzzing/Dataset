package com.example.demo.flight.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Custom exception thrown when a flight is not found in the system.
 * This exception is typically used when a flight cannot be located based on the provided identifier.
 */
public class FlightNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -1042600589031710869L;

    public static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

    private static final String DEFAULT_MESSAGE = """
            Flight not found!
            """;

    /**
     * Constructs a new FlightNotFoundException with the default error message.
     * This constructor is used when a flight is not found, and no additional details are provided.
     */
    public FlightNotFoundException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new FlightNotFoundException with a custom error message.
     * This constructor is used when a flight is not found, and an additional message is provided to further explain the context.
     *
     * @param message the custom message to be appended to the default error message.
     */
    public FlightNotFoundException(final String message) {
        super(DEFAULT_MESSAGE + " " + message);
    }

}

