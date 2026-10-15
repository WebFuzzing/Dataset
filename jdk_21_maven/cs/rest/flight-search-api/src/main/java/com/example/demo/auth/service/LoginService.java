package com.example.demo.auth.service;

import com.example.demo.auth.model.Token;
import com.example.demo.auth.model.dto.request.LoginRequest;

/**
 * Service interface for handling user login functionality.
 * This interface defines the method responsible for logging in a user by validating their credentials
 * and generating a token for subsequent authentication requests.
 */
public interface LoginService {

    /**
     * Authenticates a user and generates an authentication token.
     * This method accepts login credentials, validates the user, and returns a token if the login is successful.
     * The token can then be used for authenticated access to protected resources.
     *
     * @param loginRequest The request containing the login credentials (e.g., username/email and password).
     * @return A {@link Token} containing the authentication information, including access and refresh tokens.
     */
    Token login(final LoginRequest loginRequest);

}
