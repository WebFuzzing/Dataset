package dev.mirodil.testing_system.utils;

import dev.mirodil.testing_system.controllers.UserManagementController;
import dev.mirodil.testing_system.dtos.UserResponseDTO;
import dev.mirodil.testing_system.exceptions.InvalidTokenException;
import dev.mirodil.testing_system.exceptions.UnauthorizedException;
import dev.mirodil.testing_system.models.User;
import dev.mirodil.testing_system.models.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.net.URI;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

@Component
public class AuthUtil {
    private static final long ONE_DAY_IN_SECONDS = 24 * 60 * 60;
    private static final String secretKeyPlain = System.getenv("SECRET_KEY");
    private static final SecretKey key = Keys.hmacShaKeyFor(secretKeyPlain.getBytes());
    private static PasswordEncoder passwordEncoder;
    private static StringRedisTemplate redisTemplate;

    public AuthUtil(StringRedisTemplate redisTemplate, PasswordEncoder passwordEncoder) {
        AuthUtil.redisTemplate = redisTemplate;
        AuthUtil.passwordEncoder = passwordEncoder;
    }

    public static void blacklistToken(String token) {
        redisTemplate.opsForValue().set(
                token,
                "blackListed",
                ONE_DAY_IN_SECONDS,
                TimeUnit.SECONDS
        );
    }

    public static boolean isTokenBlacklisted(String token) {
        return redisTemplate.hasKey(token);
    }

    public static void checkTokenBlacklisted(String token) throws InvalidTokenException {
        if (isTokenBlacklisted(token)) {
            throw new InvalidTokenException();
        }
    }

    public static boolean isUserAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return isUserAuthenticated(authentication);
    }

    public static boolean isUserAuthenticated(Authentication authentication) {
        return authentication != null &&
                authentication.isAuthenticated() &&
                !(authentication instanceof AnonymousAuthenticationToken);
    }

    public static UserResponseDTO getAuthenticatedUserDTO() throws UnauthorizedException {
        User user = getAuthenticatedUser();
        if (user.isAdmin()) {
            URI customPath = linkTo(methodOn(UserManagementController.class).getProfile()).toUri();
            return new UserResponseDTO(user, customPath);
        }
        return new UserResponseDTO(user);
    }

    public static User getAuthenticatedUser() throws UnauthorizedException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!isUserAuthenticated(authentication)) {
            throw new UnauthorizedException();
        }
        return (User) authentication.getPrincipal();
    }

    public static UserRole getAuthenticatedUserRole() throws UnauthorizedException {
        User user = getAuthenticatedUser();
        return user.getUserRoleName();
    }

    private static Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public static String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    public static String extractTokenFromRequest(HttpServletRequest request) throws InvalidTokenException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException("Authorization header is missing or invalid", HttpStatus.BAD_REQUEST);
        }

        return authHeader.substring(7);
    }

    public static boolean isTokenExpired(String token) {
        Claims claims = getClaims(token);
        return claims.getExpiration().toInstant().isBefore(Instant.now());
    }

    public static Map<String, Object> generateTokenDetails(String subject) {
        String jwtToken = generateToken(subject);
        Instant expiresAt = getTokenExpiresAt(jwtToken);

        return Map.of(
                "access", jwtToken,
                "issuedAt", Instant.now(),
                "expiresAt", expiresAt,
                "type", "Bearer"
        );
    }

    public static Instant getTokenExpiresAt(String token) {
        Claims claims = getClaims(token);
        return claims.getExpiration().toInstant();
    }

    public static String generateToken(String subject) {
        Instant iat = Instant.now();
        Instant exp = iat.plusSeconds(ONE_DAY_IN_SECONDS);
        return generateToken(subject, exp);
    }

    public static String generateToken(String subject, Instant exp) {
        Instant iat = Instant.now();
        if (exp.isBefore(iat)) {
            throw new IllegalArgumentException("JWT token's expiration date cannot be before it's issued date");
        }
        return Jwts
                .builder()
                .subject(subject)
                .issuedAt(Date.from(iat))
                .expiration(Date.from(exp))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    public static String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public static boolean isPasswordMatches(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    public static void setAuthenticationToSecurityContext(User user, HttpServletRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken =
                UsernamePasswordAuthenticationToken.authenticated(
                        user,
                        null,
                        user.getAuthorities()
                );
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
