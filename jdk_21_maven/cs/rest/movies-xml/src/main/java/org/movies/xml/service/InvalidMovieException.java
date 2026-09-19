package org.movies.xml.service;

/**
 * Thrown when a request breaks a business rule that bean validation cannot express
 * on its own, or when a PATCH field is invalid. Rendered as 400.
 */
public class InvalidMovieException extends RuntimeException {

    public InvalidMovieException(String message) {
        super(message);
    }
}
