package org.movies.xml.controller;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.databind.JsonMappingException;
import org.movies.xml.dto.ErrorDto;
import org.movies.xml.service.DuplicateMovieException;
import org.movies.xml.service.InvalidMovieException;
import org.movies.xml.service.MovieNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every failure into the one error document this API knows how to speak.
 * There is deliberately no JSON fallback here either.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorDto> onBeanValidation(MethodArgumentNotValidException e) {
        return error(HttpStatus.BAD_REQUEST, firstViolation(e));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorDto> onUnreadableBody(HttpMessageNotReadableException e) {
        return error(HttpStatus.BAD_REQUEST, unreadableBodyMessage(e));
    }

    @ExceptionHandler(InvalidMovieException.class)
    public ResponseEntity<ErrorDto> onInvalidMovie(InvalidMovieException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorDto> onBadParameter(Exception e) {
        if (e instanceof MethodArgumentTypeMismatchException mismatch) {
            // An id that is not even a number names no stored movie, so it gets the same
            // 404 as a well formed id that is absent from the database. Only query
            // parameters, which are part of the request rather than of the path, are a 400.
            if (mismatch.getParameter().hasParameterAnnotation(PathVariable.class)) {
                return error(HttpStatus.NOT_FOUND, MovieNotFoundException.message(mismatch.getValue()));
            }
            return error(HttpStatus.BAD_REQUEST,
                    mismatch.getName() + " has an unacceptable value: " + mismatch.getValue());
        }
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MovieNotFoundException.class)
    public ResponseEntity<ErrorDto> onNotFound(MovieNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /**
     * A blank path segment, as in /movies/%20. Spring calls this a server error because it
     * normally means the mapping and the method disagree, but here it is the client naming
     * a movie that cannot exist, which is the same 404 as any other unknown id.
     */
    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorDto> onBlankPathVariable() {
        return error(HttpStatus.NOT_FOUND, "a blank id names no movie");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorDto> onUnknownResource(NoResourceFoundException e) {
        return error(HttpStatus.NOT_FOUND, "no resource at " + e.getResourcePath());
    }

    @ExceptionHandler(DuplicateMovieException.class)
    public ResponseEntity<ErrorDto> onDuplicate(DuplicateMovieException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorDto> onMethodNotAllowed(HttpRequestMethodNotSupportedException e) {
        return error(HttpStatus.METHOD_NOT_ALLOWED, e.getMessage());
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorDto> onUnsupportedMediaType(HttpMediaTypeNotSupportedException e) {
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "this API only accepts application/xml");
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorDto> onNotAcceptable(HttpMediaTypeNotAcceptableException e) {
        return error(HttpStatus.NOT_ACCEPTABLE, "this API only produces application/xml");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorDto> onUnexpected(Exception e) {
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "unexpected failure: " + e.getClass().getSimpleName());
    }

    /**
     * Messages from StrictXmlRequestBodyAdvice pass through. Jackson's are replaced: they name
     * internal classes and change between versions, which makes tests asserting on them brittle.
     */
    private static String unreadableBodyMessage(HttpMessageNotReadableException e) {
        if (!(e.getCause() instanceof JacksonException)) {
            return e.getMessage();
        }
        String field = mappedField(e.getCause());
        if (field == null) {
            return "the document could not be read as a movie";
        }
        return field + " does not carry a value of the expected type";
    }

    /** Dotted path of the offending field, or null when Jackson did not report one. */
    private static String mappedField(Throwable cause) {
        if (!(cause instanceof JsonMappingException mapping)) {
            return null;
        }
        StringBuilder path = new StringBuilder();
        for (JsonMappingException.Reference reference : mapping.getPath()) {
            String name = reference.getFieldName();
            if (name == null) {
                continue;
            }
            if (path.length() > 0) {
                path.append('.');
            }
            path.append(name);
        }
        return path.length() == 0 ? null : path.toString();
    }

    private static String firstViolation(MethodArgumentNotValidException e) {
        ObjectError first = e.getBindingResult().getAllErrors().stream().findFirst().orElse(null);
        if (first == null) {
            return "the document is not valid";
        }
        if (first instanceof FieldError fieldError) {
            return fieldError.getField() + " " + fieldError.getDefaultMessage();
        }
        return first.getDefaultMessage();
    }

    private static ResponseEntity<ErrorDto> error(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .contentType(MediaType.APPLICATION_XML)
                .body(new ErrorDto(status.value(), message));
    }
}
