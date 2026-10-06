package com.example.demo.auth.security;

import com.example.demo.common.model.CustomError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.text.DateFormat;

/**
 * Custom implementation of the {@link AuthenticationEntryPoint} interface to handle unauthorized access (HTTP 401).
 * This class provides a custom entry point for authentication exceptions. When an authentication exception occurs,
 * it generates a custom error response with a 401 Unauthorized status code and a specific error message format.
 */
@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    static {
        OBJECT_MAPPER.registerModule(new JavaTimeModule());
    }

    /**
     * Handles authentication errors by sending a custom error response to the client.
     * This method is invoked when an authentication exception occurs. It sets the response content type to
     * JSON, status code to 401 (Unauthorized), and writes a custom error response with error details.
     *
     * @param httpServletRequest The {@link HttpServletRequest} containing the client request data.
     * @param httpServletResponse The {@link HttpServletResponse} used to send the error response.
     * @param authenticationException The {@link AuthenticationException} that caused the entry point to be triggered.
     * @throws IOException If an I/O error occurs while writing the response.
     */
    @Override
    public void commence(final HttpServletRequest httpServletRequest,
                         final HttpServletResponse httpServletResponse,
                         final AuthenticationException authenticationException) throws IOException {

        httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
        httpServletResponse.setStatus(HttpStatus.UNAUTHORIZED.value());

        final CustomError customError = CustomError.builder()
                .header(CustomError.Header.AUTH_ERROR.getName())
                .httpStatus(HttpStatus.UNAUTHORIZED)
                .isSuccess(false)
                .build();

        final String responseBody = OBJECT_MAPPER
                .writer(DateFormat.getDateInstance())
                .writeValueAsString(customError);

        httpServletResponse.getOutputStream()
                .write(responseBody.getBytes());

    }

}
