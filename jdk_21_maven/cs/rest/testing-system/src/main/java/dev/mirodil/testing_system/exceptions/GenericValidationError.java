package dev.mirodil.testing_system.exceptions;

import org.springframework.http.HttpStatus;

public class GenericValidationError extends ResponseStatusException {
    public GenericValidationError(String message) {
        this(message, HttpStatus.BAD_REQUEST);
    }

    public GenericValidationError(String message, HttpStatus statusCode) {
        super(message, statusCode);
    }
}
