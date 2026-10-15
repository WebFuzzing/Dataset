package com.example.demo.auth.model.dto.response;

import lombok.*;

/**
 * Represents a request to refresh a token, typically used when a user needs a new access token
 * using a valid refresh token.
 * This class captures the refresh token, which is required for generating a new access token.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;
    private Long accessTokenExpiresAt;
    private String refreshToken;

}
