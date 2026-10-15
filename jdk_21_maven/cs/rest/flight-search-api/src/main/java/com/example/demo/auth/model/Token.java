package com.example.demo.auth.model;

import lombok.Builder;
import lombok.Getter;
import org.springframework.util.StringUtils;

/**
 * Represents an access and refresh token used for authentication and authorization.
 * This class contains information about the access token, its expiration time, and the refresh token.
 * It also provides utility methods for handling tokens in the form of "Bearer" tokens, which are commonly used
 * in HTTP Authorization headers.
 */
@Getter
@Builder
public class Token {

    private String accessToken;
    private Long accessTokenExpiresAt;
    private String refreshToken;

    private static final String TOKEN_PREFIX = "Bearer ";

    /**
     * Checks if the given authorization header contains a valid bearer token.
     *
     * @param authorizationHeader The authorization header to be checked.
     * @return {@code true} if the authorization header contains a valid bearer token, otherwise {@code false}.
     */
    public static boolean isBearerToken(final String authorizationHeader) {
        return StringUtils.hasText(authorizationHeader) &&
                authorizationHeader.startsWith(TOKEN_PREFIX);
    }

    /**
     * Extracts the JWT token from the authorization header.
     *
     * @param authorizationHeader The authorization header containing the token.
     * @return The JWT token extracted from the authorization header.
     */
    public static String getJwt(final String authorizationHeader) {
        return authorizationHeader.replace(TOKEN_PREFIX, "");
    }

}
