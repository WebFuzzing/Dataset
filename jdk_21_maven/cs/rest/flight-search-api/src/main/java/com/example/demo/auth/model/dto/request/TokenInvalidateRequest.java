package com.example.demo.auth.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

/**
 * Represents a request to invalidate a token, typically used to log out a user
 * by invalidating their access and refresh tokens.
 * This class is used to capture the access token and refresh token that need to be invalidated.
 * Both tokens are required for the invalidation process to be carried out.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenInvalidateRequest {

    @NotBlank
    private String accessToken;

    @NotBlank
    private String refreshToken;

}
