package dev.mirodil.testing_system.exceptions;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends ResponseStatusException {
    public InvalidTokenException() {
        this("Invalid token");
    }

    public InvalidTokenException(String message) {
        this(message, HttpStatus.UNAUTHORIZED);
    }

    public InvalidTokenException(String message, HttpStatus statusCode) {
        super(message, statusCode);
    }
}
