package com.example.demo.auth.service;

import com.example.demo.auth.model.Token;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Map;
import java.util.Set;

/**
 * Service interface for handling JWT token-related operations.
 * This interface defines methods for generating, validating, and extracting information from JWT tokens.
 * It provides functionality to create tokens, verify their validity, retrieve claims, and authenticate users.
 */
public interface TokenService {

    /**
     * Generates a new authentication token based on the provided claims.
     * This method creates a new JWT token using the specified claims.
     * The claims typically contain user-specific information such as the username, roles, and other custom attributes.
     *
     * @param claims The claims to be included in the JWT token.
     * @return A {@link Token} object containing the generated JWT token.
     */
    Token generateToken(final Map<String, Object> claims);

    /**
     * Generates a new authentication token based on the provided claims and a refresh token.
     * This method creates a new JWT token using the specified claims and includes the provided refresh token as part of the payload.
     * It is typically used when refreshing the user's authentication token.
     *
     * @param claims The claims to be included in the JWT token.
     * @param refreshToken The refresh token to include in the generated JWT token.
     * @return A {@link Token} object containing the generated JWT token.
     */
    Token generateToken(final Map<String, Object> claims, final String refreshToken);

    /**
     * Extracts the authentication information from the provided JWT token.
     * This method parses the JWT token and returns an {@link UsernamePasswordAuthenticationToken} containing the user authentication information.
     * It is typically used to authenticate the user during the request processing.
     *
     * @param token The JWT token to extract the authentication information from.
     * @return A {@link UsernamePasswordAuthenticationToken} containing the extracted authentication information.
     */
    UsernamePasswordAuthenticationToken getAuthentication(final String token);

    /**
     * Verifies the validity of the provided JWT token.
     * This method checks if the provided JWT token is valid, including verifying its signature, expiration, and other factors.
     *
     * @param jwt The JWT token to verify.
     * @throws SecurityException If the token is invalid or cannot be verified.
     */
    void verifyAndValidate(final String jwt);

    /**
     * Verifies the validity of a set of JWT tokens.
     * This method checks the validity of each JWT token in the provided set,
     * ensuring that all tokens are valid and have not been tampered with.
     *
     * @param jwts The set of JWT tokens to verify.
     * @throws SecurityException If any of the tokens are invalid or cannot be verified.
     */
    void verifyAndValidate(final Set<String> jwts);

    /**
     * Extracts the claims from the provided JWT token.
     * This method parses the JWT token and retrieves the claims contained in the token.
     * Claims typically contain user-specific information.
     *
     * @param jwt The JWT token to extract claims from.
     * @return A {@link Jws} object containing the claims of the token.
     */
    Jws<Claims> getClaims(final String jwt);

    /**
     * Retrieves the payload (claims) from the provided JWT token.
     * This method extracts the claims (payload) from the JWT token and returns them as a {@link Claims} object.
     *
     * @param jwt The JWT token to retrieve the payload from.
     * @return A {@link Claims} object containing the payload of the token.
     */
    Claims getPayload(final String jwt);

    /**
     * Retrieves the ID from the provided JWT token.
     * This method extracts the unique identifier (ID) from the JWT token, typically representing the user or session.
     *
     * @param jwt The JWT token to retrieve the ID from.
     * @return The unique identifier (ID) from the JWT token.
     */
    String getId(final String jwt);

}
