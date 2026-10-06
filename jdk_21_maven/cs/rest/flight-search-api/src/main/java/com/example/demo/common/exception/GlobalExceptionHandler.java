package com.example.demo.common.exception;

import com.example.demo.auth.exception.*;
import com.example.demo.common.model.CustomError;
import com.example.demo.flight.exception.AirportNameAlreadyExistException;
import com.example.demo.flight.exception.AirportNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Global exception handler for handling various types of exceptions across the application.
 * This class provides centralized error handling for the entire application and returns custom error responses.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    /**
     * Handles MethodArgumentNotValidException, which is thrown when method arguments are not valid.
     * The response contains details about the validation errors for each field.
     *
     * @param ex The MethodArgumentNotValidException that is thrown.
     * @return ResponseEntity containing the custom error message and validation details.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(final MethodArgumentNotValidException ex) {

        List<CustomError.CustomSubError> subErrors = new ArrayList<>();

        ex.getBindingResult().getAllErrors().forEach(error -> {
            // Determine the field name, checking if it's a FieldError or not
            String fieldName = error instanceof FieldError
                    ? ((FieldError) error).getField()
                    : error.getObjectName();  // Fallback to object name if not FieldError

            String message = error.getDefaultMessage();
            subErrors.add(
                    CustomError.CustomSubError.builder()
                            .field(fieldName)
                            .message(message)
                            .build()
            );
        });

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Validation failed")
                .subErrors(subErrors)
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);

    }

    /**
     * Handles ConstraintViolationException, which is thrown when there are constraint violations in method parameters.
     * The response contains details about the constraint violations, including the field, message, invalid value, and type.
     *
     * @param constraintViolationException The ConstraintViolationException that is thrown.
     * @return ResponseEntity containing the custom error message and constraint violation details.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    protected ResponseEntity<Object> handlePathVariableErrors(final ConstraintViolationException constraintViolationException) {

        List<CustomError.CustomSubError> subErrors = new ArrayList<>();
        constraintViolationException.getConstraintViolations()
                .forEach(constraintViolation ->
                        subErrors.add(
                                CustomError.CustomSubError.builder()
                                        .message(constraintViolation.getMessage())
                                        .field(StringUtils.substringAfterLast(constraintViolation.getPropertyPath().toString(), "."))
                                        .value(constraintViolation.getInvalidValue() != null ? constraintViolation.getInvalidValue().toString() : null)
                                        .type(constraintViolation.getInvalidValue().getClass().getSimpleName())
                                        .build()
                        )
                );

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message("Constraint violation")
                .subErrors(subErrors)
                .build();

        return new ResponseEntity<>(customError, HttpStatus.BAD_REQUEST);

    }

    /**
     * Handles general RuntimeException. It responds with an error message and a 404 NOT_FOUND status.
     *
     * @param runtimeException The RuntimeException that is thrown.
     * @return ResponseEntity containing the custom error message.
     */
    @ExceptionHandler(RuntimeException.class)
    protected ResponseEntity<?> handleRuntimeException(final RuntimeException runtimeException) {
        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.NOT_FOUND)
                .header(CustomError.Header.API_ERROR.getName())
                .message(runtimeException.getMessage())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles AccessDeniedException, which is thrown when a user does not have permission to access a resource.
     * The response contains an access denied message and a 403 FORBIDDEN status.
     *
     * @param accessDeniedException The AccessDeniedException that is thrown.
     * @return ResponseEntity containing the custom error message and access denied status.
     */
    @ExceptionHandler(AccessDeniedException.class)
    protected ResponseEntity<?> handleAccessDeniedException(final AccessDeniedException accessDeniedException) {
        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.FORBIDDEN)
                .header(CustomError.Header.AUTH_ERROR.getName())
                .message(accessDeniedException.getMessage())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.FORBIDDEN);
    }

    /**
     * Handles PasswordNotValidException, which is thrown when a provided password does not meet validation criteria.
     * The response contains the validation error message and a 400 BAD_REQUEST status.
     *
     * @param ex The PasswordNotValidException that is thrown.
     * @return ResponseEntity containing the custom error message.
     */
    @ExceptionHandler(PasswordNotValidException.class)
    protected ResponseEntity<CustomError> handlePasswordNotValidException(final PasswordNotValidException ex) {

        CustomError error = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message(ex.getMessage())
                .isSuccess(false)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles RoleNotFoundException, which is thrown when a role cannot be found in the system.
     * The response contains the error message and a 404 NOT_FOUND status.
     *
     * @param ex The RoleNotFoundException that is thrown.
     * @return ResponseEntity containing the custom error message.
     */
    @ExceptionHandler(RoleNotFoundException.class)
    protected ResponseEntity<CustomError> handleRoleNotFoundException(final RoleNotFoundException ex) {

        CustomError error = CustomError.builder()
                .time(LocalDateTime.now())
                .httpStatus(HttpStatus.NOT_FOUND)
                .header(CustomError.Header.NOT_FOUND.getName())
                .message(ex.getMessage())
                .isSuccess(false)
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles TokenAlreadyInvalidatedException, which is thrown when a token has already been invalidated.
     * The response contains the error message and a 400 BAD_REQUEST status.
     *
     * @param ex The TokenAlreadyInvalidatedException that is thrown.
     * @return ResponseEntity containing the custom error message.
     */
    @ExceptionHandler(TokenAlreadyInvalidatedException.class)
    protected ResponseEntity<CustomError> handleTokenAlreadyInvalidatedException(final TokenAlreadyInvalidatedException ex) {
        CustomError error = CustomError.builder()
                .time(LocalDateTime.now())
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message(ex.getMessage())
                .isSuccess(false)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles UserAlreadyExistException, which is thrown when a user already exists in the system.
     * The response contains the error message and a 409 CONFLICT status.
     *
     * @param ex The UserAlreadyExistException that is thrown.
     * @return ResponseEntity containing the custom error message.
     */
    @ExceptionHandler(UserAlreadyExistException.class)
    protected ResponseEntity<CustomError> handleUserAlreadyExistException(final UserAlreadyExistException ex) {
        CustomError error = CustomError.builder()
                .httpStatus(HttpStatus.CONFLICT)
                .header(CustomError.Header.ALREADY_EXIST.getName())
                .message(ex.getMessage())
                .isSuccess(false)
                .build();
        return new ResponseEntity<>(error, HttpStatus.CONFLICT);
    }

    /**
     * Handles UserNotFoundException, which is thrown when a user cannot be found in the system.
     * The response contains the error message and a 404 NOT_FOUND status.
     *
     * @param ex The UserNotFoundException that is thrown.
     * @return ResponseEntity containing the custom error message.
     */
    @ExceptionHandler(UserNotFoundException.class)
    protected ResponseEntity<CustomError> handleUserNotFoundException(final UserNotFoundException ex) {
        CustomError error = CustomError.builder()
                .httpStatus(HttpStatus.NOT_FOUND)
                .header(CustomError.Header.NOT_FOUND.getName())
                .message(ex.getMessage())
                .isSuccess(false)
                .build();
        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);
    }

    /**
     * Handles UserStatusNotValidException, which is thrown when a user's status is invalid.
     * The response contains the error message and a 400 BAD_REQUEST status.
     *
     * @param ex The UserStatusNotValidException that is thrown.
     * @return ResponseEntity containing the custom error message.
     */
    @ExceptionHandler(UserStatusNotValidException.class)
    protected ResponseEntity<CustomError> handleUserStatusNotValidException(final UserStatusNotValidException ex) {
        CustomError error = CustomError.builder()
                .httpStatus(HttpStatus.BAD_REQUEST)
                .header(CustomError.Header.VALIDATION_ERROR.getName())
                .message(ex.getMessage())
                .isSuccess(false)
                .build();
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles UnauthorizedAttemptException, which is thrown when an unauthorized access attempt is detected.
     * The response contains an "Unauthorized" message and a 401 UNAUTHORIZED status.
     *
     * @param ex The UnauthorizedAttemptException that was thrown.
     * @return ResponseEntity containing the custom error message and UNAUTHORIZED status.
     */
    @ExceptionHandler(UnAuthorizeAttemptException.class)
    protected ResponseEntity<Object> handleUnAuthorizeAttempt(final UnAuthorizeAttemptException ex){

        CustomError customError = CustomError.builder()
                .httpStatus(HttpStatus.UNAUTHORIZED)
                .header(CustomError.Header.AUTH_ERROR.getName())
                .message(ex.getMessage())
                .build();

        return new ResponseEntity<>(customError, HttpStatus.UNAUTHORIZED);
    }

    /**
     * Handles AirportNotFoundException, which is thrown when a requested airport cannot be found.
     * The response contains the error message and a 404 NOT_FOUND status.
     *
     * @param ex The AirportNotFoundException that was thrown.
     * @return ResponseEntity containing the custom error message and NOT_FOUND status.
     */
    @ExceptionHandler(AirportNotFoundException.class)
    protected ResponseEntity<CustomError> handleAirportNotFoundException(final AirportNotFoundException ex) {

        CustomError error = CustomError.builder()
                .time(LocalDateTime.now())
                .httpStatus(HttpStatus.NOT_FOUND)
                .header(CustomError.Header.NOT_FOUND.getName())
                .message(ex.getMessage())
                .isSuccess(false)
                .build();

        return new ResponseEntity<>(error, HttpStatus.NOT_FOUND);

    }

    /**
     * Handles AirportNameAlreadyExistException, which is thrown when an airport with the same name already exists.
     * The response contains the error message and a custom status code provided by the exception.
     *
     * @param ex The AirportNameAlreadyExistException that was thrown.
     * @return ResponseEntity containing the custom error message and the custom status code.
     */
    @ExceptionHandler(AirportNameAlreadyExistException.class)
    protected ResponseEntity<CustomError> handleAirportNameAlreadyExistException(final AirportNameAlreadyExistException ex) {

        CustomError error = CustomError.builder()
                .time(LocalDateTime.now())
                .httpStatus(AirportNameAlreadyExistException.STATUS)
                .header(CustomError.Header.BAD_REQUEST.getName())
                .message(ex.getMessage())
                .isSuccess(false)
                .build();

        return new ResponseEntity<>(error, AirportNameAlreadyExistException.STATUS);

    }

}
