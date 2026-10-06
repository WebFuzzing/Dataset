package com.example.demo.auth.service.impl;

import com.example.demo.auth.config.TokenConfigurationParameter;
import com.example.demo.auth.model.Token;
import com.example.demo.auth.service.InvalidTokenService;
import com.example.demo.base.AbstractBaseServiceTest;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;

/**
 * Unit test class for {@link TokenServiceImpl}.
 * This test class validates the functionality of the token service,
 * which is responsible for generating, validating, and managing tokens.
 * It mocks the {@link TokenConfigurationParameter} and {@link InvalidTokenService}
 * to test the token-related logic without dependencies on the actual configuration or token invalidation logic.
 */
class TokenServiceImplTest extends AbstractBaseServiceTest {

    @InjectMocks
    private TokenServiceImpl tokenService;

    @Mock
    private TokenConfigurationParameter tokenConfigurationParameter;

    @Mock
    private InvalidTokenService invalidTokenService;

    @Test
    void testGenerateTokenWithoutRefreshToken() throws Exception {

        // Given
        Map<String, Object> claims = new HashMap<>();
        claims.put("USER_ID", "12345");

        // Generate a valid RSA key pair for testing
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        Mockito.when(tokenConfigurationParameter.getAccessTokenExpireMinute()).thenReturn(60);
        Mockito.when(tokenConfigurationParameter.getRefreshTokenExpireDay()).thenReturn(7);
        Mockito.when(tokenConfigurationParameter.getPrivateKey()).thenReturn(privateKey);
        Mockito.when(tokenConfigurationParameter.getIssuer()).thenReturn("issuer");

        // When
        Token token = tokenService.generateToken(claims);

        assertNotNull(token, "Token should not be null");
        assertNotNull(token.getAccessToken(), "Access token should not be null");
        assertNotNull(token.getRefreshToken(), "Refresh token should not be null");

        // Verify
        Mockito.verify(tokenConfigurationParameter).getAccessTokenExpireMinute();
        Mockito.verify(tokenConfigurationParameter).getRefreshTokenExpireDay();
        Mockito.verify(tokenConfigurationParameter, Mockito.times(2)).getPrivateKey();

    }

    @Test
    void testGenerateTokenWithRefreshToken() {

        // Given
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();

        String refreshToken = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 7 * 24 * 60 * 60 * 1000))
                .signWith(privateKey) // Use the private key for signing
                .compact();

        Map<String, Object> claims = new HashMap<>();
        claims.put("USER_ID", "12345");

        // Mock the TokenConfigurationParameter to return the keys
        Mockito.when(tokenConfigurationParameter.getPrivateKey()).thenReturn(privateKey);
        Mockito.when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);
        Mockito.when(tokenConfigurationParameter.getAccessTokenExpireMinute()).thenReturn(60);
        Mockito.when(tokenConfigurationParameter.getIssuer()).thenReturn("issuer");
        Mockito.doNothing().when(invalidTokenService).checkForInvalidityOfToken(anyString());


        // When
        Token token = tokenService.generateToken(claims, refreshToken);

        // Then
        assertNotNull(token);
        assertNotNull(token.getAccessToken());
        assertNotNull(token.getRefreshToken());
        assertNotNull(token.getAccessTokenExpiresAt());

        // Verify
        Mockito.verify(tokenConfigurationParameter).getAccessTokenExpireMinute();
        Mockito.verify(invalidTokenService).checkForInvalidityOfToken(anyString());

    }

    @Test
    void testGetClaims() {

        // Given
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String jwt = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer("issuer")
                .setSubject("subject")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000)) // 1 hour
                .signWith(privateKey)
                .compact();

        // When
        Mockito.when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Then
        Jws<Claims> claims = tokenService.getClaims(jwt);

        assertNotNull(claims, "Claims should not be null");
        assertEquals("issuer", claims.getBody().getIssuer(), "Issuer should match");
        assertEquals("subject", claims.getBody().getSubject(), "Subject should match");

        // Verify
        Mockito.verify(tokenConfigurationParameter).getPublicKey();

    }

    @Test
    void testGetPayload() {

        // Given
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String jwt = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer("issuer")
                .setSubject("subject")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000)) // 1 hour
                .signWith(privateKey)
                .compact();

        // When
        Mockito.when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Then
        Claims payload = tokenService.getPayload(jwt);


        assertNotNull(payload, "Payload should not be null");
        assertEquals("issuer", payload.getIssuer(), "Issuer should match");
        assertEquals("subject", payload.getSubject(), "Subject should match");

        // Verify
        Mockito.verify(tokenConfigurationParameter).getPublicKey();

    }

    @Test
    void testVerifyAndValidateSet() {

        // Given
        KeyPair keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        String jwt1 = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer("issuer1")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000)) // 1 hour
                .signWith(privateKey)
                .compact();

        String jwt2 = Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setIssuer("issuer2")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60 * 60 * 1000)) // 1 hour
                .signWith(privateKey)
                .compact();

        Set<String> jwts = Set.of(jwt1, jwt2);

        // When
        Mockito.when(tokenConfigurationParameter.getPublicKey()).thenReturn(publicKey);

        // Then
        assertDoesNotThrow(() -> tokenService.verifyAndValidate(jwts), "All tokens should be valid");

        // Verify
        Mockito.verify(tokenConfigurationParameter, Mockito.times(2)).getPublicKey();

    }


}