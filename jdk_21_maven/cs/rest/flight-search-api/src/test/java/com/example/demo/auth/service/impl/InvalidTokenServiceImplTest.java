package com.example.demo.auth.service.impl;

import com.example.demo.auth.exception.TokenAlreadyInvalidatedException;
import com.example.demo.auth.model.entity.InvalidTokenEntity;
import com.example.demo.auth.repository.InvalidTokenRepository;
import com.example.demo.base.AbstractBaseServiceTest;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test class for {@link InvalidTokenServiceImpl}.
 * This test class verifies the functionality of the service responsible for managing invalid tokens.
 * It mocks the {@link InvalidTokenRepository} to simulate interactions with the data layer.
 */
class InvalidTokenServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private InvalidTokenServiceImpl invalidTokenService;

    @Mock
    private InvalidTokenRepository invalidTokenRepository;

    @Test
    void invalidateTokens_ShouldSaveAllInvalidTokens() {

        // Given
        Set<String> tokenIds = Set.of("token-id-1", "token-id-2");

        // When
        when(invalidTokenRepository.saveAll(any(Set.class))).thenReturn(Collections.emptyList());

        // Then
        invalidTokenService.invalidateTokens(tokenIds);

        // Verify
        verify(invalidTokenRepository, times(1)).saveAll(any(Set.class));

    }

    @Test
    void checkForInvalidityOfToken_ShouldThrowExceptionIfTokenInvalid() {

        // Given
        String tokenId = "test-token-id";

        // When
        when(invalidTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.of(new InvalidTokenEntity()));

        // Then
        assertThrows(TokenAlreadyInvalidatedException.class, () -> {
            invalidTokenService.checkForInvalidityOfToken(tokenId);
        });

        // Verify
        verify(invalidTokenRepository, times(1)).findByTokenId(tokenId);

    }

    @Test
    void checkForInvalidityOfToken_ShouldNotThrowExceptionIfTokenValid() {

        // Given
        String tokenId = "test-token-id";

        // When
        when(invalidTokenRepository.findByTokenId(tokenId)).thenReturn(Optional.empty());

        // Then
        invalidTokenService.checkForInvalidityOfToken(tokenId);

        // Verify
        verify(invalidTokenRepository, times(1)).findByTokenId(tokenId);

    }

}