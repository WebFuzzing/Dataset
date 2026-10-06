package com.example.demo.auth.service.impl;

import com.example.demo.auth.exception.UserNotFoundException;
import com.example.demo.auth.exception.UserStatusNotValidException;
import com.example.demo.auth.model.Token;
import com.example.demo.auth.model.dto.request.TokenRefreshRequest;
import com.example.demo.auth.model.entity.UserEntity;
import com.example.demo.auth.model.enums.TokenClaims;
import com.example.demo.auth.model.enums.UserStatus;
import com.example.demo.auth.repository.UserRepository;
import com.example.demo.auth.service.RefreshTokenService;
import com.example.demo.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Service implementation for handling the refreshing of authentication tokens.
 * This interface defines the method responsible for refreshing the user's authentication token.
 * It generates a new access token and possibly a new refresh token, typically used when the old access token has expired.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final UserRepository userRepository;
    private final TokenService tokenService;

    /**
     * Refreshes the user's authentication token.
     * This method accepts a request containing the refresh token and generates new access and refresh tokens.
     * The new tokens can be used for subsequent requests that require authentication.
     *
     * @param tokenRefreshRequest The request containing the refresh token information to generate new tokens.
     * @return A {@link Token} containing the new access and refresh tokens.
     */
    @Override
    public Token refreshToken(TokenRefreshRequest tokenRefreshRequest) {
        tokenService.verifyAndValidate(tokenRefreshRequest.getRefreshToken());

        final String adminId = tokenService
                .getPayload(tokenRefreshRequest.getRefreshToken())
                .get(TokenClaims.USER_ID.getValue())
                .toString();

        final UserEntity userEntityFromDB = userRepository
                .findById(adminId)
                .orElseThrow(UserNotFoundException::new);

        this.validateAdminStatus(userEntityFromDB);

        return tokenService.generateToken(
                userEntityFromDB.getClaims(),
                tokenRefreshRequest.getRefreshToken()
        );
    }

    /**
     * Validates the user's status to ensure they are active.
     * This method checks if the user's status is set to active.
     * If the user is not active, it throws a {@link UserStatusNotValidException}.
     *
     * @param userEntity The {@link UserEntity} to validate.
     * @throws UserStatusNotValidException If the user is not in an active status.
     */
    private void validateAdminStatus(final UserEntity userEntity) {
        if (!(UserStatus.ACTIVE.equals(userEntity.getUserStatus()))) {
            throw new UserStatusNotValidException("UserStatus = " + userEntity.getUserStatus());
        }
    }

}
