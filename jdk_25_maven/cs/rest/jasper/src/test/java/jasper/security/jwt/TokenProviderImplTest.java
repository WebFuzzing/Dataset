package jasper.security.jwt;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jasper.component.ConfigCache;
import jasper.config.Config.SecurityConfig;
import jasper.config.Config.ServerConfig;
import jasper.config.Props;
import jasper.domain.User;
import jasper.management.SecurityMetersService;
import jasper.security.AuthoritiesConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static jasper.repository.spec.QualifiedTag.qt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TokenProviderImplTest {

    private static final int ONE_MINUTE = 60;

    private byte[] secret;
    private TokenProviderImpl tokenProvider;

	ConfigCache configCache = getConfigs();
	ConfigCache getConfigs() {
		var root = new ServerConfig();
		var security = new SecurityConfig();
		security.setMode("jwt");
		var configCache = mock(ConfigCache.class);
		when(configCache.root()).thenReturn(root);
		when(configCache.security(anyString())).thenReturn(security);
		return configCache;
	}

	TokenProviderImpl getTokenProvider(String localOrigin) {
		return getTokenProvider(getProps(localOrigin));
	}

	Claims getClaims(String sub) {
		return new Claims(Map.of(
			"sub", sub
		));
	}

	Claims getClaims(String sub, String auth) {
		return new Claims(Map.of(
			"auth", auth,
			"sub", sub
		));
	}

	Props getProps(String localOrigin) {
		var props = new Props();
		props.setLocalOrigin(localOrigin);
		return props;
	}

	TokenProviderImpl getTokenProvider(Props props) {
		return new TokenProviderImpl(props, configCache, null, null) {
			@Override
			public Authentication getAuthentication(String jwt, String origin) {
				return null;
			}
		};
	}

	User getUser(String userTag, String ...tags) {
		var u = new User();
		var qt = qt(userTag);
		u.setTag(qt.tag);
		u.setOrigin(qt.origin);
		u.setReadAccess(new ArrayList<>(List.of(tags)));
		u.setWriteAccess(new ArrayList<>(List.of(tags)));
		u.setTagReadAccess(new ArrayList<>(List.of(tags)));
		u.setTagWriteAccess(new ArrayList<>(List.of(tags)));
		return u;
	}

    @BeforeEach
    public void setup() {
		var security = configCache.security("");
		security.setMode("jwt");
		security.setClientId("");
		String base64Secret = "Xfd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8";
		security.setBase64Secret(base64Secret);
		Props props = new Props();

        SecurityMetersService securityMetersService = new SecurityMetersService(new SimpleMeterRegistry());

        tokenProvider = new TokenProviderImpl(props, configCache, securityMetersService, null);
        secret = Base64.getDecoder().decode(base64Secret);
    }

    @Test
    void testReturnFalseWhenJWThasInvalidSignature() {
        boolean isTokenValid = tokenProvider.validateToken(createTokenWithDifferentSignature(), "");

        assertThat(isTokenValid).isFalse();
    }

    @Test
    void testReturnTrueWhenJWTisValid() {
        String token = createToken("anonymous", secret, new Date(System.currentTimeMillis() + 1800 * 1000L));

        boolean isTokenValid = tokenProvider.validateToken(token, "");

        assertThat(isTokenValid).isTrue();
    }

    @Test
    void testReturnFalseWhenJWTisMalformed() {
        String token = createToken("anonymous", secret, new Date(System.currentTimeMillis() + 1800 * 1000L));
        String invalidToken = token.substring(1);
        boolean isTokenValid = tokenProvider.validateToken(invalidToken, "");

        assertThat(isTokenValid).isFalse();
    }

    @Test
    void testReturnFalseWhenJWTisExpired() {
        String token = createToken("anonymous", secret, new Date(System.currentTimeMillis() - ONE_MINUTE * 1000L));

        boolean isTokenValid = tokenProvider.validateToken(token, "");

        assertThat(isTokenValid).isFalse();
    }

    @Test
    void testReturnFalseWhenJWTisUnsupported() {
        String unsupportedToken = createUnsupportedToken();

        boolean isTokenValid = tokenProvider.validateToken(unsupportedToken, "");

        assertThat(isTokenValid).isFalse();
    }

    @Test
    void testReturnFalseWhenJWTisInvalid() {
        boolean isTokenValid = tokenProvider.validateToken("", "");

        assertThat(isTokenValid).isFalse();
    }

    private String createToken(String sub, byte[] key, Date expiration) {
        try {
            var claims = new JWTClaimsSet.Builder()
                .subject(sub)
                .claim("auth", AuthoritiesConstants.ANONYMOUS)
                .claim("verified_email", true)
                .expirationTime(expiration)
                .build();
            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS512), claims);
            jwt.sign(new MACSigner(key));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private String createUnsupportedToken() {
        try {
            var rsaKey = new RSAKeyGenerator(2048).generate();
            var claims = new JWTClaimsSet.Builder().subject("anonymous").build();
            var jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
            jwt.sign(new RSASSASigner(rsaKey));
            return jwt.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }

    private String createTokenWithDifferentSignature() {
        var otherKey = Base64.getDecoder().decode("abfd54a45s65fds737b9aafcb3412e07ed99b267f33413274720ddbb7f6c5e64e9f14075f2d7ed041592f0b7657baf8");

        return createToken("anonymous", otherKey, new Date(new Date().getTime() + ONE_MINUTE * 1000L));
    }

	@Test
	void testGetUsername_Default() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("alice"), ""))
			.isEqualTo("+user/alice");
	}

	@Test
	void testGetUsername_Default_Invalid() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("!!!!!!!"), ""))
			.isNull();
	}

	@Test
	void testGetUsername_Default_Root_Invalid() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("!!!!!!!", "ROLE_ADMIN"), ""))
			.isEqualTo("_user");
	}

	@Test
	void testGetUsername_Default_Invalid_Origin() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("!!!!!!!"), ""))
			.isNull();
	}

	@Test
	void testGetUsername_Default_Root_Invalid_Origin() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("!!!!!!!", "ROLE_ADMIN"), ""))
			.isEqualTo("_user");
	}

	@Test
	void testGetUsername_Default_InvalidOrigin() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("alice@!!!!!!!"), ""))
			.isEqualTo("+user/alice");
	}

	@Test
	void testGetUsername_Default_Root_InvalidOrigin() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("alice@!!!!!!!", "ROLE_ADMIN"), ""))
			.isEqualTo("+user/alice");
	}

	@Test
	void testGetUsername_Default_ClaimOrigin() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("alice@main"), ""))
			.isEqualTo("+user/alice");
	}

	@Test
	void testGetUsername_Origin_Root_Invalid() {
		var tokenProvider = getTokenProvider("@main");

		assertThat(tokenProvider.getUsername(getClaims("!!!!!!!", "ROLE_ADMIN"), "@main"))
			.isEqualTo("_user@main");
	}

	@Test
	void testGetUsername_Origin_Invalid_Origin() {
		var tokenProvider = getTokenProvider("@main");

		assertThat(tokenProvider.getUsername(getClaims("!!!!!!!"), "@main"))
			.isNull();
	}

	@Test
	void testGetUsername_Origin_InvalidOrigin() {
		var tokenProvider = getTokenProvider("@main");

		assertThat(tokenProvider.getUsername(getClaims("alice@!!!!!!!"), "@main"))
			.isEqualTo("+user/alice@main");
	}

	@Test
	void testGetUsername_Origin_Root_InvalidOrigin() {
		var tokenProvider = getTokenProvider("@main");

		assertThat(tokenProvider.getUsername(getClaims("alice@!!!!!!!", "ROLE_ADMIN"), "@main"))
			.isEqualTo("+user/alice@main");
	}

	@Test
	void testGetUsername_Origin_NoClaimOrigin() {
		var tokenProvider = getTokenProvider("@other");

		assertThat(tokenProvider.getUsername(getClaims("alice@main"), "@other"))
			.isEqualTo("+user/alice@other");
	}

	@Test
	void testGetUsername_Blank_Default() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims(""), ""))
			.isNull();
	}

	@Test
	void testGetUsername_Blank_Default_ClaimOrigin() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("@main"), ""))
			.isNull();
	}

	@Test
	void testGetUsername_Blank_Origin() {
		var tokenProvider = getTokenProvider("@other");

		assertThat(tokenProvider.getUsername(getClaims("+user"), "@other"))
			.isNull();
	}

	@Test
	void testGetUsername_Blank_Origin_NoClaimOrigin() {
		var tokenProvider = getTokenProvider("@other");

		assertThat(tokenProvider.getUsername(getClaims("+user@main"), "@other"))
			.isNull();
	}

	@Test
	void testGetUsername_BlankProtected_Default() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("+user"), ""))
			.isNull();
	}

	@Test
	void testGetUsername_BlankProtected_Default_ClaimOrigin() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("+user@main"), ""))
			.isNull();
	}

	@Test
	void testGetUsername_BlankProtected_Origin() {
		var tokenProvider = getTokenProvider("@other");

		assertThat(tokenProvider.getUsername(getClaims("+user"), "@other"))
			.isNull();
	}

	@Test
	void testGetUsername_BlankProtected_Origin_NoClaimOrigin() {
		var tokenProvider = getTokenProvider("@other");

		assertThat(tokenProvider.getUsername(getClaims("+user@main"), "@other"))
			.isNull();
	}

	@Test
	void testGetUsername_Root_Default() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("", "ROLE_ADMIN"), ""))
			.isEqualTo("_user");
	}

	@Test
	void testGetUsername_Root_Default_ClaimOrigin() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("@main", "ROLE_ADMIN"), ""))
			.isEqualTo("_user");
	}

	@Test
	void testGetUsername_Root_Origin() {
		var tokenProvider = getTokenProvider("@other");

		assertThat(tokenProvider.getUsername(getClaims("", "ROLE_ADMIN"), "@other"))
			.isEqualTo("_user@other");
	}

	@Test
	void testGetUsername_Root_Origin_NoClaimOrigin() {
		var tokenProvider = getTokenProvider("@other");

		assertThat(tokenProvider.getUsername(getClaims("@main", "ROLE_ADMIN"), "@other"))
			.isEqualTo("_user@other");
	}

	@Test
	void testGetUsername_RootProtected_Default() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("+user", "ROLE_ADMIN"), ""))
			.isEqualTo("+user");
	}

	@Test
	void testGetUsername_RootProtected_Default_ClaimOrigin() {
		var tokenProvider = getTokenProvider("");

		assertThat(tokenProvider.getUsername(getClaims("+user@main", "ROLE_ADMIN"), ""))
			.isEqualTo("+user");
	}

	@Test
	void testGetUsername_RootProtected_Origin() {
		var tokenProvider = getTokenProvider("@other");

		assertThat(tokenProvider.getUsername(getClaims("+user", "ROLE_ADMIN"), "@other"))
			.isEqualTo("+user@other");
	}

	@Test
	void testGetUsername_RootProtected_Origin_NoClaimOrigin() {
		var tokenProvider = getTokenProvider("@other");

		assertThat(tokenProvider.getUsername(getClaims("+user@main", "ROLE_ADMIN"), "@other"))
			.isEqualTo("+user@other");
	}
}
