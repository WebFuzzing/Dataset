package dev.mirodil.testing_system.configs;

import dev.mirodil.testing_system.exceptions.InvalidTokenException;
import dev.mirodil.testing_system.exceptions.UserAccountBlocked;
import dev.mirodil.testing_system.models.User;
import dev.mirodil.testing_system.services.UserService;
import dev.mirodil.testing_system.utils.AuthUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final UserService userService;

    public JwtAuthenticationFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        //=== Exceptions are handled in separate ExceptionHandlerFilter ===//
        String token = authHeader.substring(7);
        String username = AuthUtil.extractUsername(token);
        AuthUtil.checkTokenBlacklisted(token);

        User user = userService.loadUserByUsername(username);
        if (AuthUtil.isTokenExpired(token)) {
            throw new InvalidTokenException("Token expired");
//            Alternative way without throwing exceptions
//            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Token expired");
//            return;
        }

        // TODO: if SessionCreationPolicy stateful, then remove:
        AuthUtil.setAuthenticationToSecurityContext(user, request);

        if (AuthUtil.isUserAuthenticated()) {
            if (!user.isAccountNonLocked()) {
                throw new UserAccountBlocked();
            }
            filterChain.doFilter(request, response);
            return;
        }

        // Continue with the filter chain
        filterChain.doFilter(request, response);
    }
}