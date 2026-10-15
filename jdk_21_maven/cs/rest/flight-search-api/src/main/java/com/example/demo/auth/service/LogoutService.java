package com.example.demo.auth.service;

import com.example.demo.auth.model.dto.request.TokenInvalidateRequest;

/**
 * Service interface for handling user logout functionality.
 * This interface defines the method responsible for logging out a user by invalidating their token.
 * It ensures that the user's authentication token is no longer valid, preventing further access to protected resources.
 */
public interface LogoutService {

    /**
     * Logs out a user by invalidating their authentication token.
     * This method accepts a request containing the token information to be invalidated. Once the token is invalidated,
     * it can no longer be used for authentication, ensuring that the user is effectively logged out.
     *
     * @param tokenInvalidateRequest The request containing the token to be invalidated for logging out the user.
     */
    void logout(final TokenInvalidateRequest tokenInvalidateRequest);

}
