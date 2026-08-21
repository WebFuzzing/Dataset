package jasper.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jasper.component.ConfigCache;
import jasper.config.Config.SecurityConfig;
import jasper.config.Config.ServerConfig;
import jasper.config.Props;
import jasper.management.SecurityMetersService;
import jasper.security.AuthoritiesConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Base64;
import java.util.Date;

import static jasper.security.AuthoritiesConstants.ANONYMOUS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JWTFilterTest {

    private TokenProviderImpl tokenProvider;
    private TokenProviderImplDefault defaultTokenProvider;
    private byte[] secret;

    private JWTFilter jwtFilter;

    @BeforeEach
    public void setup() {
		var root = new ServerConfig();
		var security = new SecurityConfig();
		var configCache = mock(ConfigCache.class);
		when(configCache.root()).thenReturn(root);
		when(configCache.security(anyString())).thenReturn(security);
		security.setMode("jwt");
		security.setClientId("");
        String base64Secret = "Xfd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8";
		security.setBase64Secret(base64Secret);
		Props props = new Props();

        SecurityMetersService securityMetersService = new SecurityMetersService(new SimpleMeterRegistry());

        tokenProvider = new TokenProviderImpl(props, configCache, securityMetersService, null);
        defaultTokenProvider = new TokenProviderImplDefault(props, configCache);
        secret = Base64.getDecoder().decode(base64Secret);

        jwtFilter = new JWTFilter(props, tokenProvider, defaultTokenProvider, configCache);
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    private String createToken(String sub, String authorities) {
        try {
            var claims = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("auth", authorities)
                .claim("verified_email", true)
                .expirationTime(new Date(System.currentTimeMillis() + 1800 * 1000L))
                .build();
            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
            jwt.sign(new MACSigner(secret));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testJWTFilter() throws Exception {
        String jwt = createToken("test.user", AuthoritiesConstants.USER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("+user/test.user");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).map(GrantedAuthority::getAuthority).contains(AuthoritiesConstants.USER);
    }

    @Test
    void testJWTFilterInvalidPrincipal() throws Exception {
        String jwt = createToken("test-user", AuthoritiesConstants.USER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isBlank();
    }

    @Test
    void testJWTFilterInvalidToken() throws Exception {
        String jwt = "wrong_jwt";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Bearer " + jwt);
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isInstanceOf(PreAuthenticatedAuthenticationToken.class);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).filteredOn(a -> a.getAuthority().equals(ANONYMOUS));
    }

    @Test
    void testJWTFilterMissingAuthorization() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isInstanceOf(PreAuthenticatedAuthenticationToken.class);
		assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).filteredOn(a -> a.getAuthority().equals(ANONYMOUS));
    }

    @Test
    void testJWTFilterMissingToken() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Bearer ");
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isInstanceOf(PreAuthenticatedAuthenticationToken.class);
		assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).filteredOn(a -> a.getAuthority().equals(ANONYMOUS));
    }

    @Test
    void testJWTFilterWrongScheme() throws Exception {
        String jwt = createToken("test-user", AuthoritiesConstants.USER);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(JWTFilter.AUTHORIZATION_HEADER, "Basic " + jwt);
        request.setRequestURI("/api/test");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain filterChain = new MockFilterChain();
        jwtFilter.doFilter(request, response, filterChain);
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
		assertThat(SecurityContextHolder.getContext().getAuthentication()).isInstanceOf(PreAuthenticatedAuthenticationToken.class);
		assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities()).filteredOn(a -> a.getAuthority().equals(ANONYMOUS));
    }
}
