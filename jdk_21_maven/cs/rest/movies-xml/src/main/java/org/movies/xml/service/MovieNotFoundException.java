package org.movies.xml.service;

/**
 * Thrown when an id does not match any stored movie. Rendered as 404.
 */
public class MovieNotFoundException extends RuntimeException {

    public MovieNotFoundException(Long id) {
        super(message(id));
    }

    /**
     * The one wording for "no such movie", shared with the handler that turns an id
     * which is not even a number into the same 404.
     */
    public static String message(Object id) {
        return "movie " + id + " not found";
    }
}
