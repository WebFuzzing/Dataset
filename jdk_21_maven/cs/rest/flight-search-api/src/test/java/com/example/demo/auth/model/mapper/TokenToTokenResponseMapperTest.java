package com.example.demo.auth.model.mapper;

import com.example.demo.auth.model.Token;
import com.example.demo.auth.model.dto.response.TokenResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit test class for {@link TokenToTokenResponseMapper}.
 * This class tests the mapping functionality between a {@link Token} and a {@link TokenResponse}
 * to ensure correct transformation of token data.
 */
class TokenToTokenResponseMapperTest {

    private final TokenToTokenResponseMapper mapper = TokenToTokenResponseMapper.initialize();

    @Test
    void testMapTokenNull() {

        TokenResponse result = mapper.map((Token) null);

        assertNull(result);

    }

    @Test
    void testMapTokenCollectionNull() {

        List<TokenResponse> result = mapper.map((Collection<Token>) null);

        assertNull(result);

    }

    @Test
    void testMapTokenListEmpty() {

        List<TokenResponse> result = mapper.map(Collections.emptyList());

        assertNotNull(result);
        assertTrue(result.isEmpty());

    }

    @Test
    void testMapTokenListWithNullElements() {

        Token token = Token.builder()
                .accessToken("access123")
                .refreshToken("refresh123")
                .accessTokenExpiresAt(123456789L)
                .build();

        List<Token> tokens = Arrays.asList(token, null);

        List<TokenResponse> result = mapper.map(tokens);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertNotNull(result.get(0));
        assertNull(result.get(1));

    }

    @Test
    void testMapSingleToken() {

        Token token = Token.builder()
                .accessToken("accessToken123")
                .refreshToken("refreshToken123")
                .accessTokenExpiresAt(987654321L)
                .build();

        TokenResponse result = mapper.map(token);

        assertNotNull(result);
        assertEquals(token.getAccessToken(), result.getAccessToken());
        assertEquals(token.getRefreshToken(), result.getRefreshToken());
        assertEquals(token.getAccessTokenExpiresAt(), result.getAccessTokenExpiresAt());

    }

    @Test
    void testMapTokenListWithValues() {

        Token token1 = Token.builder()
                .accessToken("access1")
                .refreshToken("refresh1")
                .accessTokenExpiresAt(111111111L)
                .build();

        Token token2 = Token.builder()
                .accessToken("access2")
                .refreshToken("refresh2")
                .accessTokenExpiresAt(222222222L)
                .build();

        List<Token> tokens = Arrays.asList(token1, token2);

        List<TokenResponse> result = mapper.map(tokens);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(token1.getAccessToken(), result.get(0).getAccessToken());
        assertEquals(token2.getAccessToken(), result.get(1).getAccessToken());
        assertEquals(token1.getRefreshToken(), result.get(0).getRefreshToken());
        assertEquals(token2.getRefreshToken(), result.get(1).getRefreshToken());
        assertEquals(token1.getAccessTokenExpiresAt(), result.get(0).getAccessTokenExpiresAt());
        assertEquals(token2.getAccessTokenExpiresAt(), result.get(1).getAccessTokenExpiresAt());

    }

    @Test
    void testMapWithEdgeCaseValues() {

        Token token = Token.builder()
                .accessToken("")
                .refreshToken(null)
                .accessTokenExpiresAt(null)
                .build();

        TokenResponse result = mapper.map(token);

        assertNotNull(result);
        assertEquals("", result.getAccessToken());
        assertNull(result.getRefreshToken());
        assertNull(result.getAccessTokenExpiresAt());

    }

}