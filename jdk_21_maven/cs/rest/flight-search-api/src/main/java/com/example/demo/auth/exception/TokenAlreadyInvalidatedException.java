package com.example.demo.auth.exception;

import org.springframework.http.HttpStatus;

import java.io.Serial;

/**
 * Custom exception thrown when an attempt is made to invalidate a token that is already invalidated.
 * This exception extends {@link RuntimeException} and is typically used in token management
 * scenarios to indicate that a token cannot be invalidated again.
 */
public class TokenAlreadyInvalidatedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 66846615891590622L;

    public static final HttpStatus STATUS = HttpStatus.BAD_REQUEST;

    private static final String DEFAULT_MESSAGE = """
            Token is already invalidated!
            """;

    /**
     * Constructs a new {@code TokenAlreadyInvalidatedException} with the default message.
     */
    public TokenAlreadyInvalidatedException() {
        super(DEFAULT_MESSAGE);
    }

    /**
     * Constructs a new {@code TokenAlreadyInvalidatedException} with the default message
     * and includes the specified token ID in the message.
     *
     * @param tokenId the ID of the token that was already invalidated.
     */
    public TokenAlreadyInvalidatedException(final String tokenId) {
        super(DEFAULT_MESSAGE + " TokenID = " + tokenId);
    }

}
