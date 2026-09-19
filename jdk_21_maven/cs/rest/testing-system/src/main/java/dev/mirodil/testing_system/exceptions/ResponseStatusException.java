package dev.mirodil.testing_system.exceptions;

import org.springframework.http.HttpStatus;

public abstract class ResponseStatusException extends RuntimeException {
    private final HttpStatus statusCode;

    public ResponseStatusException(String message, HttpStatus statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public HttpStatus getStatusCode() {
        return statusCode;
    }

    public int getStatusCodeValue() {
        return statusCode.value();
    }
}
