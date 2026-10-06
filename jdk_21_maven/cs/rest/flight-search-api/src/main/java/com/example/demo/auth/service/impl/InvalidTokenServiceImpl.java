package com.example.demo.auth.service.impl;

import com.example.demo.auth.exception.TokenAlreadyInvalidatedException;
import com.example.demo.auth.model.entity.InvalidTokenEntity;
import com.example.demo.auth.repository.InvalidTokenRepository;
import com.example.demo.auth.service.InvalidTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service implementation for managing and validating invalidated tokens.
 * This service is responsible for invalidating tokens and checking the validity of tokens.
 * It provides methods to mark tokens as invalid and to check if a given token has already been invalidated.
 */
@Service
@RequiredArgsConstructor
public class InvalidTokenServiceImpl implements InvalidTokenService {

    private final InvalidTokenRepository invalidTokenRepository;

    /**
     * Invalidates the tokens with the given token IDs.
     * This method marks the tokens as invalid, making them unusable for authentication or authorization.
     * It can be used to invalidate tokens that have been revoked or are no longer valid.
     *
     * @param tokenIds A set of token IDs to invalidate.
     */
    @Override
    public void invalidateTokens(Set<String> tokenIds) {
        final Set<InvalidTokenEntity> invalidTokenEntities = tokenIds.stream()
                .map(tokenId -> InvalidTokenEntity.builder()
                        .tokenId(tokenId)
                        .build()
                )
                .collect(Collectors.toSet());

        invalidTokenRepository.saveAll(invalidTokenEntities);
    }

    /**
     * Checks if the given token ID is invalidated.
     * This method checks whether a specific token has been invalidated and is no longer valid for use.
     * It is typically used to validate if a token is still active before allowing access to protected resources.
     *
     * @param tokenId The token ID to check for invalidity.
     */
    @Override
    public void checkForInvalidityOfToken(String tokenId) {
        final boolean isTokenInvalid = invalidTokenRepository.findByTokenId(tokenId).isPresent();

        if (isTokenInvalid) {
            throw new TokenAlreadyInvalidatedException(tokenId);
        }
    }

}
