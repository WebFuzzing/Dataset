package com.example.demo.builder;

import com.example.demo.auth.model.enums.TokenClaims;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A builder class for creating JWT claims used for token generation.
 */
public class TokenBuilder {

    /**
     * Creates valid JWT claims for a given user ID and first name.
     * These claims are typically used to generate a JWT token.
     *
     * @param userId the user ID to include in the claims
     * @param firstName the user's first name to include in the claims
     * @return the generated JWT claims
     */
    public static Claims getValidClaims(String userId, String firstName) {
        Map<String, Object> mockClaimsMap = new HashMap<>();
        mockClaimsMap.put(TokenClaims.JWT_ID.getValue(), UUID.randomUUID().toString());
        mockClaimsMap.put(TokenClaims.USER_ID.getValue(), userId);
        mockClaimsMap.put(TokenClaims.USER_FIRST_NAME.getValue(), firstName);
        return Jwts.claims(mockClaimsMap);
    }

}
