package dev.mirodil.testing_system.exceptions;

import dev.mirodil.testing_system.dtos.GenericErrorResponse;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;


@Component
public class ExceptionHandlerFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            filterChain.doFilter(request, response);
        } catch (JwtException e) {
            handleException(request, response, HttpStatus.UNAUTHORIZED, "Invalid JWT Token");
        } catch (InvalidTokenException | UserAccountBlocked | ResourceNotFoundException e) {
            handleException(request, response, e.getStatusCode(), e.getMessage());
        }
    }

    private void handleException(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpStatus status,
            String message
    ) throws IOException {
        response.setContentType("application/json");
        response.setStatus(status.value());
        URI path = URI.create(request.getRequestURL().toString());
        String jsonResponse = GenericErrorResponse.getErrorDetailsJson(message, status, path);
        response.getWriter().write(jsonResponse);
    }
}
