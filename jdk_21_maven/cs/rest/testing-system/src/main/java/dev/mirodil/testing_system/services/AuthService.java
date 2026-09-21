package dev.mirodil.testing_system.services;

import dev.mirodil.testing_system.dtos.AuthResponseDTO;
import dev.mirodil.testing_system.dtos.UserLoginRequestDTO;
import dev.mirodil.testing_system.dtos.UserResponseDTO;
import dev.mirodil.testing_system.models.User;
import dev.mirodil.testing_system.utils.AuthUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import javax.security.auth.login.AccountLockedException;
import java.util.Map;

@Service
public class AuthService {
    private final UserService userService;

    public AuthService(UserService userService) {
        this.userService = userService;
    }

    public AuthResponseDTO authenticate(UserLoginRequestDTO request, HttpServletRequest servletRequest) throws BadCredentialsException, AccountLockedException {
        User user;
        try {
            user = userService.loadUserByUsername(request.email());
        } catch (UsernameNotFoundException e) {
            throw new BadCredentialsException("Email or password is incorrect");
        }

        if (!AuthUtil.isPasswordMatches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Email or password is incorrect");
        }

        if (!user.isAccountNonLocked()) {
            throw new AccountLockedException("Your account is locked. Please contact support.");
        }

        AuthUtil.setAuthenticationToSecurityContext(user, servletRequest);
        Map<String, Object> jwtTokenDetails = AuthUtil.generateTokenDetails(user.getEmail());

        return new AuthResponseDTO(new UserResponseDTO(user), jwtTokenDetails);
    }

    public void logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        AuthUtil.blacklistToken(AuthUtil.extractTokenFromRequest(request));
    }
}
