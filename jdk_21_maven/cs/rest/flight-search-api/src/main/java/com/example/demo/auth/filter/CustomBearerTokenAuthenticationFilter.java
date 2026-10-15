package com.example.demo.auth.filter;

import com.example.demo.auth.model.Token;
import com.example.demo.auth.service.InvalidTokenService;
import com.example.demo.auth.service.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Custom filter for handling Bearer token authentication.
 * This filter extracts the Bearer token from the HTTP `Authorization` header, validates the token,
 * checks if it has been invalidated, and sets the authentication in the {@link SecurityContextHolder}.
 * Extends {@link OncePerRequestFilter} to ensure the filter is executed only once per request.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CustomBearerTokenAuthenticationFilter extends OncePerRequestFilter {

    private final TokenService tokenService;
    private final InvalidTokenService invalidTokenService;

    /**
     * Performs filtering logic for each HTTP request to validate Bearer tokens.
     *
     * @param httpServletRequest  the current HTTP request.
     * @param httpServletResponse the current HTTP response.
     * @param filterChain         the filter chain to delegate further request processing.
     * @throws ServletException if an error occurs during request processing.
     * @throws IOException      if an input or output error occurs during request processing.
     */
    @Override
    protected void doFilterInternal(@NonNull final HttpServletRequest httpServletRequest,
                                    @NonNull final HttpServletResponse httpServletResponse,
                                    @NonNull final FilterChain filterChain) throws ServletException, IOException {

        log.debug("API Request was secured with Security!");

        final String authorizationHeader = httpServletRequest.getHeader(HttpHeaders.AUTHORIZATION);

        if (Token.isBearerToken(authorizationHeader)) {

            final String jwt = Token.getJwt(authorizationHeader);

            tokenService.verifyAndValidate(jwt);

            final String tokenId = tokenService.getId(jwt);

            invalidTokenService.checkForInvalidityOfToken(tokenId);

            final UsernamePasswordAuthenticationToken authentication = tokenService
                    .getAuthentication(jwt);

            SecurityContextHolder.getContext().setAuthentication(authentication);

        }

        filterChain.doFilter(httpServletRequest,httpServletResponse);

    }

}
