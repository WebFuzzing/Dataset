package dev.mirodil.testing_system.exceptions;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends ResponseStatusException {
    public UnauthorizedException() {
        this("Unauthorized");
    }

    public UnauthorizedException(String message) {
        this(message, HttpStatus.UNAUTHORIZED);
    }

    public UnauthorizedException(String message, HttpStatus statusCode) {
        super(message, statusCode);
    }
}
