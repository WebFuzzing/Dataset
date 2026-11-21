package reciter.consumer.service;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.cognitoidentityprovider.CognitoIdentityProviderClient;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthRequest;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AdminInitiateAuthResponse;
import software.amazon.awssdk.services.cognitoidentityprovider.model.AuthFlowType;

@Service
public class CognitoAuthService {
	
	private static final Logger log = LoggerFactory.getLogger(CognitoAuthService.class);

    // These should be injected from Kubernetes Secrets/Env Vars
    private final String USER_POOL_ID = System.getenv("AWS_COGNITO_USER_POOL_ID");
    private final String CLIENT_ID = System.getenv("MASTER_CLIENT_ID");
    private final String CLIENT_SECRET = System.getenv("MASTER_CLIENT_SECRET");
    // MODIFIED: fallback to us-east-1 when AWS_REGION env var is unset (prevents Region.of(null) NPE on startup)
    private final String AWS_REGION = Optional.ofNullable(System.getenv("AWS_REGION")).filter(s -> !s.isBlank()).orElse("us-east-1");
    
    
    
 // Initialize Caffeine Cache
    // Key: Department Username, Value: JWT Token
     private final Cache<String, String> tokenCache = Caffeine.newBuilder()
    	    .maximumSize(2000)
    	    .expireAfter(new Expiry<String, String>() {
    	        @Override
    	        public long expireAfterCreate(String key, String token, long currentTime) {
    	            // Calculate remaining time in nanoseconds
    	            long secondsToExpiry = getSecondsUntilExpiry(token);
    	            // Provide a 60-second "grace period" so we don't return a token 
    	            // that expires exactly as it reaches the destination
    	            long safeSeconds = Math.max(0, secondsToExpiry - 60);
    	            return TimeUnit.SECONDS.toNanos(safeSeconds);
    	        }

    	        @Override
    	        public long expireAfterUpdate(String key, String value, long currentTime, long currentDuration) {
    	            return currentDuration; // Keep original expiry on update
    	        }

    	        @Override
    	        public long expireAfterRead(String key, String value, long currentTime, long currentDuration) {
    	            return currentDuration; // Keep original expiry on read
    	        }
    	    })
    	    .build();
    
    private long getSecondsUntilExpiry(String token) {
        try {
            // Use your existing JwtDecoder or a simple library like JJWT/Auth0
            DecodedJWT jwt = JWT.decode(token);
            long expiresAt = jwt.getExpiresAt().getTime(); // Milliseconds
            long now = System.currentTimeMillis();
            return (expiresAt - now) / 1000;
        } catch (Exception e) {
            return 0; // Expire immediately if token is invalid
        }
    }
    // v2: CognitoIdentityProviderClient replaces AWSCognitoIdentityProvider
    // v2: .builder().region() replaces .withRegion()
    private final CognitoIdentityProviderClient cognitoClient = CognitoIdentityProviderClient.builder()
            .region(Region.of(AWS_REGION))
            .build();

    public String authenticateConsumer(String username, String password) {
    
        // 1. Check Cache First
        String cachedToken = tokenCache.getIfPresent(username);
        if (cachedToken != null) {
            return cachedToken;
        }

        // 2. If not in cache, calculate Hash and call Cognito
        String secretHash = calculateSecretHash(CLIENT_ID, CLIENT_SECRET, username);

        Map<String, String> authParams = new HashMap<>();
        authParams.put("USERNAME", username);
        authParams.put("PASSWORD", password);
        authParams.put("SECRET_HASH", secretHash);

       
        AdminInitiateAuthRequest authRequest = AdminInitiateAuthRequest.builder()
                .authFlow(AuthFlowType.ADMIN_USER_PASSWORD_AUTH)
                .userPoolId(USER_POOL_ID)
                .clientId(CLIENT_ID)
                .authParameters(authParams)
                .build();

        try {
        	// v2: adminInitiateAuth() returns AdminInitiateAuthResponse (not AdminInitiateAuthResult)
            AdminInitiateAuthResponse result = cognitoClient.adminInitiateAuth(authRequest);
            String idToken = result.authenticationResult().idToken();
            
            // 3. Store in Cache before returning
            tokenCache.put(username, idToken);
            
            return idToken;
            
        } catch (Exception e) {
            log.error("Login failed for: "+ username + " Error: " + e.getMessage());
            throw e;
        }
    }

    private String calculateSecretHash(String clientId, String clientSecret, String userName) {
        final String HMAC_SHA256_ALGORITHM = "HmacSHA256";
        SecretKeySpec signingKey = new SecretKeySpec(
                clientSecret.getBytes(StandardCharsets.UTF_8),
                HMAC_SHA256_ALGORITHM);
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            mac.init(signingKey);
            mac.update(userName.getBytes(StandardCharsets.UTF_8));
            byte[] rawHmac = mac.doFinal(clientId.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(rawHmac);
        } catch (Exception e) {
            throw new RuntimeException("Error calculating secret hash", e);
        }
    }
}
