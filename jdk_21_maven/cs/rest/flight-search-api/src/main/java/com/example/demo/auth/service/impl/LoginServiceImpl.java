package com.example.demo.auth.service.impl;

import com.example.demo.auth.exception.PasswordNotValidException;
import com.example.demo.auth.exception.UserNotFoundException;
import com.example.demo.auth.model.Token;
import com.example.demo.auth.model.dto.request.LoginRequest;
import com.example.demo.auth.model.entity.UserEntity;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.auth.service.LoginService;
import com.example.demo.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service implementation for handling user login functionality.
 * This interface defines the method responsible for logging in a user by validating their credentials
 * and generating a token for subsequent authentication requests.
 */
@Service
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    /**
     * Authenticates a user and generates an authentication token.
     * This method accepts login credentials, validates the user, and returns a token if the login is successful.
     * The token can then be used for authenticated access to protected resources.
     *
     * @param loginRequest The request containing the login credentials (e.g., username/email and password).
     * @return A {@link Token} containing the authentication information, including access and refresh tokens.
     */
    @Override
    public Token login(LoginRequest loginRequest) {

        final UserEntity userEntityFromDB = userRepository
                .findUserEntityByEmail(loginRequest.getEmail())
                .orElseThrow(
                        () -> new UserNotFoundException(loginRequest.getEmail())
                );

        if (Boolean.FALSE.equals(passwordEncoder.matches(
                loginRequest.getPassword(), userEntityFromDB.getPassword()))) {
            throw new PasswordNotValidException();
        }

        return tokenService.generateToken(userEntityFromDB.getClaims());

    }

}
