package com.example.demo.auth.service;

import com.example.demo.auth.model.User;
import com.example.demo.auth.model.dto.request.RegisterRequest;

/**
 * Service interface for handling user registration functionality.
 * This interface defines the method responsible for registering a new user in the system.
 * It processes the registration request, validates the data, and creates a new user entity.
 */
public interface RegisterService {

    /**
     * Registers a new user in the system.
     * This method accepts a registration request containing the user details (e.g., email, password, etc.).
     * It validates the input and creates a new user in the system.
     *
     * @param registerRequest The request containing the user's registration details (e.g., name, email, password).
     * @return A {@link User} object representing the newly created user.
     */
    User registerUser(final RegisterRequest registerRequest);

}
