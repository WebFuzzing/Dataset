package com.example.demo.auth.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Represents a request to refresh a token, typically used when a user needs a new access token
 * using a valid refresh token.
 * This class captures the refresh token, which is required for generating a new access token.
 */
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenRefreshRequest {

    @NotBlank
    private String refreshToken;

}
