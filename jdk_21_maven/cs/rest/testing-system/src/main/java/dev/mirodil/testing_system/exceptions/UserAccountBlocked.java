package dev.mirodil.testing_system.exceptions;

import org.springframework.http.HttpStatus;

public class UserAccountBlocked extends ResponseStatusException {
    public UserAccountBlocked() {
        this("System administrator has restricted your account from using the system.");
    }

    public UserAccountBlocked(String message) {
        this(message, HttpStatus.FORBIDDEN);
    }

    public UserAccountBlocked(String message, HttpStatus statusCode) {
        super(message, statusCode);
    }
}
