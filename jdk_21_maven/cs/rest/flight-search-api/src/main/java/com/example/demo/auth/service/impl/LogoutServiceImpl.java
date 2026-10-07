package com.example.demo.auth.service.impl;

import com.example.demo.auth.model.dto.request.TokenInvalidateRequest;
import com.example.demo.auth.service.InvalidTokenService;
import com.example.demo.auth.service.LogoutService;
import com.example.demo.auth.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Service implementation for handling user logout functionality.
 * This interface defines the method responsible for logging out a user by invalidating their token.
 * It ensures that the user's authentication token is no longer valid, preventing further access to protected resources.
 */
@Service
@RequiredArgsConstructor
public class LogoutServiceImpl implements LogoutService {

    private final TokenService tokenService;
    private final InvalidTokenService invalidTokenService;

    /**
     * Logs out a user by invalidating their authentication token.
     * This method accepts a request containing the token information to be invalidated. Once the token is invalidated,
     * it can no longer be used for authentication, ensuring that the user is effectively logged out.
     *
     * @param tokenInvalidateRequest The request containing the token to be invalidated for logging out the user.
     */
    @Override
    public void logout(TokenInvalidateRequest tokenInvalidateRequest) {

        tokenService.verifyAndValidate(
                Set.of(
                        tokenInvalidateRequest.getAccessToken(),
                        tokenInvalidateRequest.getRefreshToken()
                )
        );

        final String accessTokenId = tokenService
                .getPayload(tokenInvalidateRequest.getAccessToken())
                .getId();

        invalidTokenService.checkForInvalidityOfToken(accessTokenId);


        final String refreshTokenId = tokenService
                .getPayload(tokenInvalidateRequest.getRefreshToken())
                .getId();

        invalidTokenService.checkForInvalidityOfToken(refreshTokenId);

        invalidTokenService.invalidateTokens(Set.of(accessTokenId,refreshTokenId));

    }

}
