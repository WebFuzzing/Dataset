package com.example.gdprkv.http;

import com.example.gdprkv.service.GdprKvException;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badReq(IllegalArgumentException ex) {
        String code = ex.getMessage() != null ? ex.getMessage() : "BAD_REQUEST";
        return ResponseEntity.badRequest().body(Map.of("code", code, "message", ex.getMessage()));
    }

    @ExceptionHandler(GdprKvException.class)
    public ResponseEntity<Map<String, Object>> domainError(GdprKvException ex) {
        HttpStatus status;
        switch (ex.getCode()) {
            case INVALID_PURPOSE -> status = HttpStatus.BAD_REQUEST;
            case SUBJECT_NOT_FOUND -> status = HttpStatus.NOT_FOUND;
            case RECORD_NOT_FOUND -> status = HttpStatus.NOT_FOUND;
            case SUBJECT_ALREADY_EXISTS -> status = HttpStatus.CONFLICT;
            default -> status = HttpStatus.INTERNAL_SERVER_ERROR;
        }

        return ResponseEntity.status(status)
                .body(Map.of(
                        "code", ex.getCode().name(),
                        "message", ex.getMessage()
                ));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> boom(Exception ex) {
        log.error("Unexpected error handling request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("code", "INTERNAL_ERROR", "message", ex.getMessage()));
    }
}
