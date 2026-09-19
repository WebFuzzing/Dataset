package dev.mirodil.testing_system.exceptions;

import dev.mirodil.testing_system.dtos.GenericErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.security.auth.login.AccountLockedException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalControllerExceptionHandler {
    private final HttpServletRequest servletRequest;

    public GlobalControllerExceptionHandler(HttpServletRequest servletRequest) {
        this.servletRequest = servletRequest;
    }

    private static List<Map<String, String>> getValidationErrorReasons(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        for (ObjectError objectError : ex.getBindingResult().getAllErrors()) {
            FieldError fieldError = (FieldError) objectError;
            Map<String, String> error = new HashMap<>();

            error.put("field", fieldError.getField());
            error.put("rejectedValue", (String) fieldError.getRejectedValue());
            error.put("defaultMessage", objectError.getDefaultMessage());
            error.put("code", objectError.getCode());
            errors.add(error);
        }
        return errors;
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(ResponseStatusException e) {
        return GenericErrorResponse.returnResponse(e.getMessage(), e.getStatusCode(), servletRequest);
    }

    @ExceptionHandler({
            InvalidTokenException.class,
            GenericValidationError.class,
            UnauthorizedException.class
    })
    public ResponseEntity<Map<String, Object>> handleUserExceptions(ResponseStatusException e) {
        return GenericErrorResponse.returnResponse(e.getMessage(), e.getStatusCode(), servletRequest);
    }

    @ExceptionHandler({
            BadCredentialsException.class,
            AccountLockedException.class
    })
    public ResponseEntity<Map<String, Object>> handleUserExceptionsWithDefaultStatusCode(Exception e) {
        return GenericErrorResponse.returnResponse(e.getMessage(), HttpStatus.BAD_REQUEST, servletRequest);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = getValidationErrorReasons(ex);

        URI path = URI.create(servletRequest.getRequestURL().toString());
        Map<String, Object> response = GenericErrorResponse.getErrorDetailsMap("Invalid request - validation error. Error count: " + errors.size(), HttpStatus.BAD_REQUEST, path);
        response.put("errors", errors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
}
