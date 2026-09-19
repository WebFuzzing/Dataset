package dev.mirodil.testing_system.exceptions;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ResponseStatusException {
    public ResourceNotFoundException() {
        this("Resource not found");
    }

    public ResourceNotFoundException(String message) {
        this(message, HttpStatus.NOT_FOUND);
    }

    public ResourceNotFoundException(String message, HttpStatus statusCode) {
        super(message, statusCode);
    }
}
