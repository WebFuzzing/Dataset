package com.example.demo.auth.service;

import java.util.Set;

/**
 * Service interface for managing and validating invalidated tokens.
 * This service is responsible for invalidating tokens and checking the validity of tokens.
 * It provides methods to mark tokens as invalid and to check if a given token has already been invalidated.
 */
public interface InvalidTokenService {

    /**
     * Invalidates the tokens with the given token IDs.
     * This method marks the tokens as invalid, making them unusable for authentication or authorization.
     * It can be used to invalidate tokens that have been revoked or are no longer valid.
     *
     * @param tokenIds A set of token IDs to invalidate.
     */
    void invalidateTokens(final Set<String> tokenIds);

    /**
     * Checks if the given token ID is invalidated.
     * This method checks whether a specific token has been invalidated and is no longer valid for use.
     * It is typically used to validate if a token is still active before allowing access to protected resources.
     *
     * @param tokenId The token ID to check for invalidity.
     */
    void checkForInvalidityOfToken(final String tokenId);

}
