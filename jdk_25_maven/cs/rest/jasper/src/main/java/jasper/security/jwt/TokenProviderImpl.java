package jasper.security.jwt;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import jasper.component.ConfigCache;
import jasper.config.Props;
import jasper.domain.User;
import jasper.domain.proj.Tag;
import jasper.errors.UserTagInUseException;
import jasper.management.SecurityMetersService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.client.RestTemplate;

import javax.crypto.spec.SecretKeySpec;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static jasper.domain.proj.HasOrigin.formatOrigin;
import static jasper.domain.proj.Tag.localTag;
import static jasper.domain.proj.Tag.matchesPublic;
import static jasper.domain.proj.Tag.prefix;
import static jasper.security.Auth.USER_TAG_HEADER;
import static jasper.security.Auth.getHeader;
import static jasper.security.AuthoritiesConstants.ADMIN;
import static jasper.security.AuthoritiesConstants.MOD;
import static jasper.security.AuthoritiesConstants.PRIVATE;
import static jasper.util.Logging.getMessage;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.util.StringUtils.hasText;

public class TokenProviderImpl extends AbstractTokenProvider implements TokenProvider {
	private final Logger logger = LoggerFactory.getLogger(TokenProviderImpl.class);

	private static final String INVALID_JWT_TOKEN = "Invalid JWT token.";

	private static final String[] ROOT_ROLES_ALLOWED = new String[]{ MOD, ADMIN };

	Map<String, JwtDecoder> jwtDecoders = new ConcurrentHashMap<>();

	private final SecurityMetersService securityMetersService;
	private final RestTemplate restTemplate;

	public TokenProviderImpl(Props props, ConfigCache caches, SecurityMetersService securityMetersService, RestTemplate restTemplate) {
		super(props, caches);
		this.securityMetersService = securityMetersService;
		this.restTemplate = restTemplate;
	}

	public Authentication getAuthentication(String token, String origin) {
		var claims = new Claims(getDecoder(origin).decode(token).getClaims());
		var principal = getUsername(claims, origin);
		User user;
		try {
			user = getUser(localTag(principal), claims, origin);
		} catch (UserTagInUseException e) {
			principal = localTag(principal) + "." + (int) Math.floor(Math.random() * 1000) + origin;
			user = getUser(localTag(principal), claims, origin);
		}
		logger.debug("{} Token Auth {}", origin, principal);
		return new JwtAuthentication(principal, user, claims, getAuthorities(claims, user, origin));
	}

	JwtDecoder getDecoder(String origin) {
		return jwtDecoders.computeIfAbsent(origin, o -> {
			var security = configs.security(o);
			switch (security.getMode()) {
				case "jwt":
					var secret = security.getSecretBytes();
					return noClockSkew(NimbusJwtDecoder
						.withSecretKey(new SecretKeySpec(secret, "HmacSHA512"))
						.macAlgorithm(MacAlgorithm.HS512)
						.jwtProcessorCustomizer(p -> p.setJWSKeySelector(new JWSVerificationKeySelector<SecurityContext>(
							Set.of(JWSAlgorithm.HS256, JWSAlgorithm.HS384, JWSAlgorithm.HS512),
							new ImmutableSecret<>(secret))))
						.build());
				case "jwks":
					return noClockSkew(NimbusJwtDecoder
						.withJwkSetUri(security.getJwksUri())
						.restOperations(restTemplate)
						.jwsAlgorithms(algs -> algs.addAll(List.of(SignatureAlgorithm.RS256, SignatureAlgorithm.RS384, SignatureAlgorithm.RS512)))
						.build());
				default:
					return null;
			}
		});
	}

	private static NimbusJwtDecoder noClockSkew(NimbusJwtDecoder decoder) {
		decoder.setJwtValidator(JwtValidators.createDefaultWithValidators(new JwtTimestampValidator(Duration.ZERO)));
		return decoder;
	}

	Collection<? extends GrantedAuthority> getAuthorities(Claims claims, User user, String origin) {
		var auth = getPartialAuthorities(claims, origin);
		if (user != null && user.getRole() != null) {
			logger.debug("{} User Roles: {}", origin, user.getRole());
			if (User.ROLES.contains(user.getRole().trim())) {
				auth.add(new SimpleGrantedAuthority(user.getRole().trim()));
			}
		} else {
			logger.debug("{} No User", origin);
		}
		return auth;
	}

	List<SimpleGrantedAuthority> getPartialAuthorities(Claims claims, String origin) {
		var auth = getPartialAuthorities(origin);
		var authClaim = claims.getString(configs.security(origin).getAuthoritiesClaim());
		if (isNotBlank(authClaim)) {
			Arrays.stream(authClaim.split(","))
				.filter(r -> !r.isBlank())
				.map(String::trim)
				.map(SimpleGrantedAuthority::new)
				.forEach(auth::add);
		}
		return auth;
	}

	String getUsername(Claims claims, String origin) {
		var userTagHeader = getHeader(USER_TAG_HEADER);
		if (isBlank(userTagHeader) || !userTagHeader.matches(User.REGEX)) {
			userTagHeader = "";
		} else {
			userTagHeader = userTagHeader.toLowerCase();
		}
		var security = configs.security(origin);
		var principal = claims.getString(security.getUsernameClaim());
		logger.debug("{} User tag set by JWT claim {}: ({})", origin, security.getUsernameClaim(), principal);
		if (props.isAllowUserTagHeader() && isNotBlank(userTagHeader)) {
			principal = userTagHeader;
			logger.debug("{} User tag set by header: {}", origin, principal);
		} else if (security.isExternalId()) {
			var user = configs.getUserByExternalId(origin, principal);
			if (user.isPresent()) {
				logger.debug("{} Username: {} (external ID: {})", origin, user.get().getTag(), principal);
				if (isBlank(userTagHeader)) {
					return user.get().getTag() + origin;
				} else if (matchesPublic(principal, userTagHeader)) {
					logger.debug("{} User tag set by header: {}", origin, userTagHeader);
					return userTagHeader + origin;
				}
			}
		}
		logger.debug("{} Principal: {}", origin, principal);
		if (principal != null && principal.contains("@")) {
			var emailDomain = principal.substring(principal.indexOf("@") + 1);
			principal = principal.substring(0, principal.indexOf("@"));
			if (security.isEmailDomainInUsername() && !emailDomain.equals(security.getRootEmailDomain())) {
				principal = emailDomain + "/" + principal;
			}
		}
		var authorities = getPartialAuthorities(claims, origin);
		if (isBlank(principal) ||
			!principal.matches(Tag.QTAG_REGEX) ||
			principal.equals("+user") ||
			principal.equals("_user")) {
			logger.debug("{} Invalid principal {}.", origin, principal);
			if (authorities.stream().noneMatch(a ->
				Arrays.stream(ROOT_ROLES_ALLOWED).anyMatch(r -> a.getAuthority().equals(r)))) {
				// Invalid username and can't fall back to root user
				logger.debug("{} Root role not allowed.", origin);
				return null;
			}
			// The root user has access to every other user.
			// Only assign to mods or higher when username is missing.
			if (!"+user".equals(principal)) {
				// Default to private user if +user is not exactly specified
				principal = "_user";
			}
		} else if (!matchesPublic("+user", principal)) {
			var isPrivate = authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(a -> a.equals(PRIVATE));
			principal = prefix(isPrivate ? "_user" : "+user", principal);
		}
		if (isNotBlank(userTagHeader) && (matchesPublic(principal, userTagHeader) || matchesPublic(security.getDefaultUser(), userTagHeader))) {
			logger.debug("{} User tag set by header: {}", origin, userTagHeader);
			principal = userTagHeader;
		}
		logger.debug("{} Username: {}", origin, principal);
		return principal + origin;
	}

	@Override
	public boolean validateToken(String authToken, String origin) {
		if (!hasText(authToken)) return false;
		var security = configs.security(origin);
		if (isBlank(security.getMode())) {
			logger.error("{} No client for origin {} in security settings", origin, formatOrigin(origin));
			return false;
		}
		try {
			var decoder = getDecoder(origin);
			if (decoder == null) {
				logger.error("{} No client for origin {} in security settings", origin, formatOrigin(origin));
				return false;
			}
			var jwt = decoder.decode(authToken);
			var audience = jwt.getAudience();
			if (isBlank(security.getClientId()) &&
				audience != null &&
				(!audience.contains("") || !audience.isEmpty())) {
				securityMetersService.trackTokenInvalidAudience();
				logger.trace(INVALID_JWT_TOKEN + " Invalid Audience");
			} else if (isNotBlank(security.getClientId()) &&
				(audience == null || !audience.contains(security.getClientId()) || audience.size() != 1)) {
				// TODO: add method to whitelist extra audiences
				securityMetersService.trackTokenInvalidAudience();
				logger.trace(INVALID_JWT_TOKEN + " Invalid Audience");
			} else if (isNotBlank(security.getVerifiedEmailClaim()) && jwt.getClaims().getOrDefault(security.getVerifiedEmailClaim(), Boolean.FALSE).equals(false)) {
				securityMetersService.trackUnverifiedEmail();
				logger.trace(INVALID_JWT_TOKEN + " Email is not verified");
			} else {
				return true;
			}
		} catch (JwtValidationException e) {
			if (e.getErrors().stream().anyMatch(err -> err.getDescription() != null && err.getDescription().contains("expired"))) {
				securityMetersService.trackTokenExpired();
			}
			logger.trace(INVALID_JWT_TOKEN, e);
		} catch (BadJwtException e) {
			var message = getMessageChain(e);
			if (message.contains("Invalid signature")) {
				securityMetersService.trackTokenInvalidSignature();
			} else if (message.contains("Another algorithm expected") || message.contains("Unsupported") || message.contains("not a valid JSON")) {
				securityMetersService.trackTokenUnsupported();
			} else {
				securityMetersService.trackTokenMalformed();
			}
			logger.trace(INVALID_JWT_TOKEN, e);
		} catch (JwtException e) {
			logger.trace(INVALID_JWT_TOKEN, e);
		} catch (IllegalArgumentException e) {
			logger.error("{} Token validation error {}", origin, getMessage(e));
		}
        return false;
	}

	private static String getMessageChain(Throwable e) {
		var sb = new StringBuilder();
		while (e != null) {
			if (e.getMessage() != null) sb.append(e.getMessage()).append(" ");
			e = e.getCause();
		}
		return sb.toString();
	}
}
