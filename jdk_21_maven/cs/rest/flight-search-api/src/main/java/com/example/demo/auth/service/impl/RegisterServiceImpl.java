package com.example.demo.auth.service.impl;

import com.example.demo.auth.exception.UserAlreadyExistException;
import com.example.demo.auth.model.User;
import com.example.demo.auth.model.dto.request.RegisterRequest;
import com.example.demo.auth.model.entity.UserEntity;
import com.example.demo.auth.model.mapper.RegisterRequestToUserEntityMapper;
import com.example.demo.auth.model.mapper.UserEntityToUserMapper;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.auth.service.RegisterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service implementation for handling user registration functionality.
 * This interface defines the method responsible for registering a new user in the system.
 * It processes the registration request, validates the data, and creates a new user entity.
 */
@Service
@RequiredArgsConstructor
public class RegisterServiceImpl implements RegisterService {

    private final UserRepository userRepository;

    private final RegisterRequestToUserEntityMapper registerRequestToUserEntityMapper =
            RegisterRequestToUserEntityMapper.initialize();

    private final UserEntityToUserMapper userEntityToUserMapper = UserEntityToUserMapper.initialize();

    private final PasswordEncoder passwordEncoder;

    /**
     * Registers a new user in the system.
     * This method accepts a registration request containing the user details (e.g., email, password, etc.).
     * It validates the input and creates a new user in the system.
     *
     * @param registerRequest The request containing the user's registration details (e.g., name, email, password).
     * @return A {@link User} object representing the newly created user.
     */
    @Override
    public User registerUser(final RegisterRequest registerRequest) {

        if (userRepository.existsUserEntityByEmail(registerRequest.getEmail())) {
            throw new UserAlreadyExistException("The email is already used for another user : " + registerRequest.getEmail());
        }

        final UserEntity userEntityToBeSaved = registerRequestToUserEntityMapper.mapForSaving(registerRequest);

        userEntityToBeSaved.setPassword(passwordEncoder.encode(registerRequest.getPassword()));

        final UserEntity savedUserEntity = userRepository.save(userEntityToBeSaved);

        return userEntityToUserMapper.map(savedUserEntity);

    }

}
