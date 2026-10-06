package com.example.demo.auth.service;

import com.example.demo.auth.model.Token;
import com.example.demo.auth.model.dto.request.TokenRefreshRequest;

/**
 * Service interface for handling the refreshing of authentication tokens.
 * This interface defines the method responsible for refreshing the user's authentication token.
 * It generates a new access token and possibly a new refresh token, typically used when the old access token has expired.
 */
public interface RefreshTokenService {

    /**
     * Refreshes the user's authentication token.
     * This method accepts a request containing the refresh token and generates new access and refresh tokens.
     * The new tokens can be used for subsequent requests that require authentication.
     *
     * @param tokenRefreshRequest The request containing the refresh token information to generate new tokens.
     * @return A {@link Token} containing the new access and refresh tokens.
     */
    Token refreshToken(final TokenRefreshRequest tokenRefreshRequest);

}
