package dev.mirodil.testing_system.controllers;

import dev.mirodil.testing_system.dtos.AuthResponseDTO;
import dev.mirodil.testing_system.dtos.UserLoginRequestDTO;
import dev.mirodil.testing_system.dtos.WrapResponseWithContentKey;
import dev.mirodil.testing_system.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.security.auth.login.AccountLockedException;
import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Login (for all user roles)",
            description = "Authenticate using email and password. **Default Admin Credentials: email:** info@mirodil.dev, **password:** 12345"
    )
    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody UserLoginRequestDTO request,
            HttpServletRequest servletRequest
    ) throws BadCredentialsException, AccountLockedException {
        AuthResponseDTO authResponseDTO = authService.authenticate(request, servletRequest);
        return ResponseEntity.ok(
                new WrapResponseWithContentKey<>(authResponseDTO)
        );
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(Collections.singletonMap("message", "Successfully logged out"));
    }
}
