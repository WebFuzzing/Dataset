package org.movies.xml.service;

/**
 * Thrown when a title/year pair is already taken. Rendered as 409.
 */
public class DuplicateMovieException extends RuntimeException {

    public DuplicateMovieException(String title, Integer year) {
        super("a movie titled '" + title + "' from " + year + " already exists");
    }
}
