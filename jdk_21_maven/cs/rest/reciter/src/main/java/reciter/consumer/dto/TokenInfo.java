package reciter.consumer.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.Setter;
/**
 * Represents the authentication token information retrieved from AWS Cognito or another identity provider.
 *
 * <p>This class typically contains details such as the access token and its expiration time.
 * It is used to cache tokens and control access to downstream services that require OAuth2 or JWT tokens.</p>
 *
 * <p>Instances of this class are stored in a Caffeine cache, with expiration based on the {@code expiryTime}
 * field. The cache is keyed by an API key that maps to client credentials or secrets.</p>
 *
 * <p>Example usage:</p>
 * <pre>{@code
 * TokenInfo token = tokenService.getToken("some-api-key");
 * String accessToken = token.getAccessToken();
 * }</pre>
 *
 * @author mjangari
 * @version 1.0
 * @since 2025-09-15
 */
@Getter
@Setter
public class TokenInfo {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("expires_in")
    private int expiresIn;  // seconds

    @JsonProperty("refresh_token")
    private String refreshToken;  // optional

    private Instant expiresAt;  // computed field (not from JSON)
    
 
    // Call this after deserialization to set expiry timestamp
    public void computeExpiry() {
        this.expiresAt = Instant.now().plusSeconds(expiresIn);
    }

    public boolean isExpired() {
        return expiresAt == null || Instant.now().isAfter(expiresAt);
    }

 }
