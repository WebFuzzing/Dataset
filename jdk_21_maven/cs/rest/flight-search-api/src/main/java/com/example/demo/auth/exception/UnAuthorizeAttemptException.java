package com.example.demo.auth.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Custom exception thrown when an unauthorized attempt is made to perform an action,
 * such as creating a to-do item without proper permissions.
 * This exception extends {@link RuntimeException} and is typically used in scenarios
 * where access control or authorization checks fail.
 */
public class UnAuthorizeAttemptException extends RuntimeException{

    @Serial
    private static final long serialVersionUID = 668466158918970622L;

    public static final HttpStatus STATUS = HttpStatus.UNAUTHORIZED;

    private static final String DEFAULT_MESSAGE = """
            You do not have permission to create a to-do item.
            """;

    /**
     * Constructs a new {@code UnAuthorizeAttemptException} with the default message.
     */
    public UnAuthorizeAttemptException() {
        super(DEFAULT_MESSAGE);
    }

}
