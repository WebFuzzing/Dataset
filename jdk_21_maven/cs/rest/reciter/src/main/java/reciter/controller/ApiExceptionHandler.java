package reciter.controller;

import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Centralized mapping of Bean Validation failures and write-once conditional-put
 * conflicts to RFC 7807 problem bodies. Scoped to these three exception types
 * only; pre-existing conditional writers catch ConditionalCheckFailedException
 * themselves, so their behavior is unchanged.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ProblemDetail> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		String detail = ex.getBindingResult().getFieldErrors().stream()
				.map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
				.collect(Collectors.joining("; "));
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
	}

	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ProblemDetail> handleHandlerMethodValidation(HandlerMethodValidationException ex) {
		String detail = ex.getAllErrors().stream().map(error -> error.getDefaultMessage())
				.collect(Collectors.joining("; "));
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problemDetail);
	}

	@ExceptionHandler(ConditionalCheckFailedException.class)
	public ResponseEntity<ProblemDetail> handleConditionalCheckFailed(ConditionalCheckFailedException ex) {
		ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"A record with this key already exists");
		return ResponseEntity.status(HttpStatus.CONFLICT).body(problemDetail);
	}
}
