package com.example.demo.base;

import com.example.demo.auth.config.TokenConfigurationParameter;
import com.example.demo.auth.model.Token;
import com.example.demo.auth.model.enums.TokenClaims;
import com.example.demo.builder.AdminUserBuilder;
import com.example.demo.builder.UserBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.shaded.org.apache.commons.lang3.time.DateUtils;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Abstract base test class for REST controller tests. This class is extended by all controller test classes
 * and provides a mock MVC setup, a Jackson ObjectMapper, and mock tokens for authentication.
 * This class sets up the necessary configurations for testing REST endpoints and includes mock authorization tokens
 * for admin and user roles.
 */
@SpringBootTest
@AutoConfigureMockMvc
public class AbstractRestControllerTest extends AbstractTestContainerConfiguration {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected Token mockAdminToken;

    protected Token mockUserToken;

    @Mock
    private TokenConfigurationParameter tokenConfiguration;

    /**
     * Initializes the mock tokens for admin and user roles before each test.
     * This method creates new tokens with the required claims for testing.
     */
    @BeforeEach
    public void initializeAuth() {

        this.tokenConfiguration = new TokenConfigurationParameter();
        this.mockAdminToken = this.generate(new AdminUserBuilder().withValidFields().build().getClaims());
        this.mockUserToken = this.generate(new UserBuilder().withValidFields().build().getClaims());
    }

    /**
     * Generates a {@link Token} using the given claims for both access and refresh tokens.
     *
     * @param claims the claims to be included in the token.
     * @return a {@link Token} containing access and refresh tokens.
     */
    private Token generate(Map<String, Object> claims) {

        final long currentTimeMillis = System.currentTimeMillis();

        final Date tokenIssuedAt = new Date(currentTimeMillis);

        final Date accessTokenExpiresAt = DateUtils.addMinutes(new Date(currentTimeMillis), tokenConfiguration.getAccessTokenExpireMinute());

        final String accessToken = Jwts.builder()
                .header()
                .add(TokenClaims.TYP.getValue(), OAuth2AccessToken.TokenType.BEARER.getValue())
                .and()
                .id(UUID.randomUUID().toString())
                .issuer(tokenConfiguration.getIssuer())
                .issuedAt(tokenIssuedAt)
                .expiration(accessTokenExpiresAt)
                .signWith(tokenConfiguration.getPrivateKey())
                .claims(claims)
                .compact();

        final Date refreshTokenExpiresAt = DateUtils.addDays(new Date(currentTimeMillis), tokenConfiguration.getRefreshTokenExpireDay());

        final JwtBuilder refreshTokenBuilder = Jwts.builder();

        final String refreshToken = refreshTokenBuilder
                .header()
                .add(TokenClaims.TYP.getValue(), OAuth2AccessToken.TokenType.BEARER.getValue())
                .and()
                .id(UUID.randomUUID().toString())
                .issuer(tokenConfiguration.getIssuer())
                .issuedAt(tokenIssuedAt)
                .expiration(refreshTokenExpiresAt)
                .signWith(tokenConfiguration.getPrivateKey())
                .claim(TokenClaims.USER_ID.getValue(), claims.get(TokenClaims.USER_ID.getValue()))
                .compact();

        return Token.builder()
                .accessToken(accessToken)
                .accessTokenExpiresAt(accessTokenExpiresAt.toInstant().getEpochSecond())
                .refreshToken(refreshToken)
                .build();

    }

}
