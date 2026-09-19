package dev.mirodil.testing_system.dtos;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.web.servlet.error.BasicErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.net.URI;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class GenericErrorResponse {
    /**
     * Creates and returns a {@link ResponseEntity} containing a JSON-formatted error message.
     *
     * @param message    value for the json object
     * @param statusCode http status code
     * @param path       request path of which error occurred
     * @return ResponseEntity - same structure as default error response in {@link BasicErrorController}
     */
    public static ResponseEntity<Map<String, Object>> returnResponse(String message, HttpStatus statusCode, URI path) {
        if (statusCode == HttpStatus.NO_CONTENT) {
            return new ResponseEntity<>(statusCode);
        }

        Map<String, Object> errorDetails = getErrorDetailsMap(message, statusCode, path);
        return new ResponseEntity<>(errorDetails, statusCode);
    }

    public static Map<String, Object> getErrorDetailsMap(String message, HttpStatus statusCode, URI path) {
        Map<String, Object> errorDetails = new HashMap<>();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now().truncatedTo(ChronoUnit.SECONDS));

        errorDetails.put("timestamp", timestamp);
        errorDetails.put("status", statusCode.value());
        errorDetails.put("error", statusCode.getReasonPhrase());
        errorDetails.put("message", message);
        errorDetails.put("path", path);
        return errorDetails;
    }

    public static String getErrorDetailsJson(String message, HttpStatus statusCode, URI path) throws JsonProcessingException {
        Map<String, Object> errorDetailsMap = getErrorDetailsMap(message, statusCode, path);
        ObjectMapper objectMapper = new ObjectMapper();
        return objectMapper.writeValueAsString(errorDetailsMap);
    }

    /**
     * Creates and returns a {@link ResponseEntity} containing a JSON-formatted error message.
     *
     * @param message    value for the json object
     * @param statusCode http status code
     * @param request    HttpServletRequest to set "error" key in response
     * @return ResponseEntity - same structure as default error response in {@link BasicErrorController}
     */
    public static ResponseEntity<Map<String, Object>> returnResponse(String message, HttpStatus statusCode, HttpServletRequest request) {
        URI path = URI.create(request.getRequestURL().toString());
        return returnResponse(message, statusCode, path);
    }
}
