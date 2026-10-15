package com.example.demo.auth.service.impl;

import com.example.demo.auth.config.TokenConfigurationParameter;
import com.example.demo.auth.model.Token;
import com.example.demo.auth.model.enums.ConfigurationParameter;
import com.example.demo.auth.model.enums.TokenClaims;
import com.example.demo.auth.model.enums.TokenType;
import com.example.demo.auth.model.enums.UserType;
import com.example.demo.auth.service.InvalidTokenService;
import com.example.demo.auth.service.TokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service implementation for handling JWT token-related operations.
 * This interface defines methods for generating, validating, and extracting information from JWT tokens.
 * It provides functionality to create tokens, verify their validity, retrieve claims, and authenticate users.
 */
@Service
@RequiredArgsConstructor
public class TokenServiceImpl implements TokenService {

    private final TokenConfigurationParameter tokenConfigurationParameter;
    private final InvalidTokenService invalidTokenService;

    /**
     * Generates a new authentication token based on the provided claims.
     * This method creates a new JWT token using the specified claims.
     * The claims typically contain user-specific information such as the username, roles, and other custom attributes.
     *
     * @param claims The claims to be included in the JWT token.
     * @return A {@link Token} object containing the generated JWT token.
     */
    @Override
    public Token generateToken(Map<String, Object> claims) {

        final long currentTimeMillis = System.currentTimeMillis();

        final Date tokenIssuedAt = new Date(currentTimeMillis);

        final Date accessTokenExpiresAt = DateUtils.addMinutes(
                new Date(currentTimeMillis),
                tokenConfigurationParameter.getAccessTokenExpireMinute()
        );

        final String accessToken = Jwts.builder()
                .header()
                .type(TokenType.BEARER.getValue())
                .and()
                .id(UUID.randomUUID().toString())
                .issuer(ConfigurationParameter.ISSUER.getDefaultValue())
                .issuedAt(tokenIssuedAt)
                .expiration(accessTokenExpiresAt)
                .signWith(tokenConfigurationParameter.getPrivateKey())
                .claims(claims)
                .compact();

        final Date refreshTokenExpiresAt = DateUtils.addDays(
                new Date(currentTimeMillis),
                tokenConfigurationParameter.getRefreshTokenExpireDay()
        );

        final String refreshToken = Jwts.builder()
                .header()
                .type(TokenType.BEARER.getValue())
                .and()
                .id(UUID.randomUUID().toString())
                .issuer(tokenConfigurationParameter.getIssuer())
                .issuedAt(tokenIssuedAt)
                .expiration(refreshTokenExpiresAt)
                .signWith(tokenConfigurationParameter.getPrivateKey())
                .claim(TokenClaims.USER_ID.getValue(), claims.get(TokenClaims.USER_ID.getValue()))
                .compact();

        return Token.builder()
                .accessToken(accessToken)
                .accessTokenExpiresAt(accessTokenExpiresAt.toInstant().getEpochSecond())
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Generates a new authentication token based on the provided claims and a refresh token.
     * This method creates a new JWT token using the specified claims and includes the provided refresh token as part of the payload.
     * It is typically used when refreshing the user's authentication token.
     *
     * @param claims The claims to be included in the JWT token.
     * @param refreshToken The refresh token to include in the generated JWT token.
     * @return A {@link Token} object containing the generated JWT token.
     */
    @Override
    public Token generateToken(Map<String, Object> claims, String refreshToken) {

        final long currentTimeMillis = System.currentTimeMillis();

        final String refreshTokenId = this.getId(refreshToken);

        invalidTokenService.checkForInvalidityOfToken(refreshTokenId);

        final Date accessTokenIssuedAt = new Date(currentTimeMillis);

        final Date accessTokenExpiresAt = DateUtils.addMinutes(
                new Date(currentTimeMillis),
                tokenConfigurationParameter.getAccessTokenExpireMinute()
        );

        final String accessToken = Jwts.builder()
                .header()
                .type(TokenType.BEARER.getValue())
                .and()
                .id(UUID.randomUUID().toString())
                .issuer(tokenConfigurationParameter.getIssuer())
                .issuedAt(accessTokenIssuedAt)
                .expiration(accessTokenExpiresAt)
                .signWith(tokenConfigurationParameter.getPrivateKey())
                .claims(claims)
                .compact();

        return Token.builder()
                .accessToken(accessToken)
                .accessTokenExpiresAt(accessTokenExpiresAt.toInstant().getEpochSecond())
                .refreshToken(refreshToken)
                .build();
    }

    /**
     * Extracts the authentication information from the provided JWT token.
     * This method parses the JWT token and returns an {@link UsernamePasswordAuthenticationToken} containing the user authentication information.
     * It is typically used to authenticate the user during the request processing.
     *
     * @param token The JWT token to extract the authentication information from.
     * @return A {@link UsernamePasswordAuthenticationToken} containing the extracted authentication information.
     */
    @Override
    public UsernamePasswordAuthenticationToken getAuthentication(String token) {

        final Jws<Claims> claimsJws = Jwts.parser()
                .verifyWith(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseSignedClaims(token);

        final JwsHeader jwsHeader = claimsJws.getHeader();
        final Claims payload = claimsJws.getPayload();

        final Jwt jwt = new org.springframework.security.oauth2.jwt.Jwt(
                token,
                payload.getIssuedAt().toInstant(),
                payload.getExpiration().toInstant(),
                Map.of(
                        TokenClaims.TYP.getValue(), jwsHeader.getType(),
                        TokenClaims.ALGORITHM.getValue(), jwsHeader.getAlgorithm()
                ),
                payload
        );


        final UserType userType = UserType.valueOf(payload.get(TokenClaims.USER_TYPE.getValue()).toString());

        final List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        authorities.add(new SimpleGrantedAuthority(userType.name()));

        return UsernamePasswordAuthenticationToken
                .authenticated(jwt, null, authorities);
    }

    /**
     * Verifies the validity of the provided JWT token.
     * This method checks if the provided JWT token is valid, including verifying its signature, expiration, and other factors.
     *
     * @param jwt The JWT token to verify.
     * @throws SecurityException If the token is invalid or cannot be verified.
     */
    @Override
    public void verifyAndValidate(String jwt) {
        Jwts.parser()
                .verifyWith(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseSignedClaims(jwt);
    }

    /**
     * Verifies the validity of a set of JWT tokens.
     * This method checks the validity of each JWT token in the provided set,
     * ensuring that all tokens are valid and have not been tampered with.
     *
     * @param jwts The set of JWT tokens to verify.
     * @throws SecurityException If any of the tokens are invalid or cannot be verified.
     */
    @Override
    public void verifyAndValidate(Set<String> jwts) {
        jwts.forEach(this::verifyAndValidate);
    }

    /**
     * Extracts the claims from the provided JWT token.
     * This method parses the JWT token and retrieves the claims contained in the token.
     * Claims typically contain user-specific information.
     *
     * @param jwt The JWT token to extract claims from.
     * @return A {@link Jws} object containing the claims of the token.
     */
    @Override
    public Jws<Claims> getClaims(String jwt) {
        return Jwts.parser()
                .verifyWith(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseSignedClaims(jwt);
    }

    /**
     * Retrieves the payload (claims) from the provided JWT token.
     * This method extracts the claims (payload) from the JWT token and returns them as a {@link Claims} object.
     *
     * @param jwt The JWT token to retrieve the payload from.
     * @return A {@link Claims} object containing the payload of the token.
     */
    @Override
    public Claims getPayload(String jwt) {
        return Jwts.parser()
                .verifyWith(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    /**
     * Retrieves the ID from the provided JWT token.
     * This method extracts the unique identifier (ID) from the JWT token, typically representing the user or session.
     *
     * @param jwt The JWT token to retrieve the ID from.
     * @return The unique identifier (ID) from the JWT token.
     */
    @Override
    public String getId(String jwt) {
        return Jwts.parser()
                .verifyWith(tokenConfigurationParameter.getPublicKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload()
                .getId();
    }

}
