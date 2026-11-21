package reciter.security;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * @author mjangari
 * This will intercept and request for consumer api JWT token
 * and admin api key and authenticate its JWT token or api-key
 */
@EnableWebSecurity
@Configuration
public class APISecurityConfig {

    @Autowired(required = false)
    private MultiApiKeyFilter multiApiKeyFilter;

    @Autowired(required = false)
    private CustomAuthenticationEntryPoint customEntryPoint;

    private static final Logger log = LoggerFactory.getLogger(APISecurityConfig.class);

    @Value("${spring.security.enabled}")
    private boolean securityEnabled;

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}")
    private String jwkSetUri;

    @Value("${aws.cognito.issuer-uri}")
    private String issuerUri;

    @Value("${aws.cognito.user-pool-id}")
    private String userPoolId;

    @Value("${aws.cognito.userpool.region}")
    private String awsRegion;

    @Value("${aws.cognito.cache.refresh.timeinterval}")
    private int cognitoCacheRefreshTimeInterval;

    @Autowired
    private S3UserLogHandler s3UserLogHandler;

    @Bean
    public MultiApiKeyFilter MultiApiKeyAuthenticationFilter() {
        log.info("JWT filter bean is being created!");
        return new MultiApiKeyFilter();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertyScanner() {
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        log.info("DEBUG: Issuer URI is " + issuerUri);
        log.info("DEBUG: awsRegion is " + awsRegion);
        log.info("DEBUG: u is " + userPoolId);

        // 1. Mandatory Property Validation (The Guard Clause)
        boolean isConfigMissing = jwkSetUri == null || jwkSetUri.isBlank() || jwkSetUri.contains("${") ||
                                  issuerUri == null || issuerUri.isBlank() || issuerUri.contains("${") ||
                                  "NONE".equals(userPoolId);

        if (isConfigMissing) {
            log.warn(">>> JWT SECURITY IS DISABLED <<<");
            log.warn("Missing or unresolved Kubernetes ConfigMap values. JWK URI: {}, Pool ID: {}", jwkSetUri, userPoolId);

            // Return a fail-fast decoder so the Spring Context stays healthy locally
            return token -> {
                throw new JwtException("JWT Authentication is not configured in this environment.");
            };
        }

        // 2. Initialize the Base Decoder
        log.info("Initializing Cognito JwtDecoder for Region: {} with Refresh Interval: {} min",
                 awsRegion, cognitoCacheRefreshTimeInterval);
        NimbusJwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(jwkSetUri.trim()).build();

        // 3. Audience Validator — handles ArrayList<String> from Cognito ID tokens
        OAuth2TokenValidator<Jwt> audValidator = new JwtClaimValidator<List<String>>(
            JwtClaimNames.AUD,
            audList -> audList != null && audList.contains(System.getenv("MASTER_CLIENT_ID"))
        );

        // 4. Timestamp Validator — checks 'exp' and 'nbf' with a 30s clock skew
        OAuth2TokenValidator<Jwt> timestampValidator = new JwtTimestampValidator(Duration.ofSeconds(30));

        // 5. Issuer Validator
        OAuth2TokenValidator<Jwt> issuerValidator = JwtValidators.createDefaultWithIssuer(issuerUri.trim());

        // 6. Combine all validators (order: timestamp → issuer → audience)
        OAuth2TokenValidator<Jwt> combinedValidator = new DelegatingOAuth2TokenValidator<>(
            timestampValidator,
            issuerValidator,
            audValidator
        );

        jwtDecoder.setJwtValidator(combinedValidator);
        return jwtDecoder;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {

        log.info("Inside the SecurityFilterChain configuration of APISecurityConfig");

        httpSecurity
            .securityMatcher("/reciter/**")
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(customEntryPoint)
            )
            // Configure JWT Bearer token support.
            // BearerTokenAuthenticationFilter is wired internally here.
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.decoder(jwtDecoder()))
            )
            // API Key filter — runs early, before standard auth filters
            .addFilterBefore(multiApiKeyFilter, UsernamePasswordAuthenticationFilter.class)
            // S3 logging filter — must run AFTER all authentication is complete
            // so that SecurityContextHolder is populated with the authenticated principal.
            //
            // WHY NOT addFilterAfter(..., BearerTokenAuthenticationFilter.class)?
            // In Spring Security 6.4, BearerTokenAuthenticationFilter is NOT registered
            // in FilterOrderRegistration (the internal map powering addFilterBefore/After).
            // It is inserted directly into the chain as an OrderedFilter at build() time,
            // so it is never available as an anchor — regardless of when oauth2ResourceServer()
            // is called. Using it as an anchor always throws IllegalArgumentException.
            //
            // SOLUTION: Anchor to ExceptionTranslationFilter (always registered, order 1500).
            // All authentication filters — including BearerTokenAuthenticationFilter — run
            // before ExceptionTranslationFilter, so placing S3LoggingFilter just before it
            // guarantees authentication is complete when the logger executes.
            .addFilterBefore(new S3LoggingFilter(s3UserLogHandler), ExceptionTranslationFilter.class)
            .authorizeHttpRequests(auth -> auth
                // Allow token generation endpoint without authentication
                .requestMatchers(HttpMethod.POST, "/reciter/generate-access-token").permitAll()
                .requestMatchers("/reciter/v3/api-docs/**", "/reciter/swagger-ui/**").permitAll()
                // Everything else requires a valid JWT or API Key
                .anyRequest().authenticated()
            );

        return httpSecurity.build();
    }
    
   
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> {
            log.info("Inside WebSecurityCustomizer of APiSecurity");
            if (!securityEnabled) {
                web.ignoring().requestMatchers("/reciter/**");
            }
            // Whitelist ping and token generation endpoints
            web.ignoring().requestMatchers("/reciter/ping");
        };
    }
}