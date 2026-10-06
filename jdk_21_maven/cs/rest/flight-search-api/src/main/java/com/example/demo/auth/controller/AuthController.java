package com.example.demo.auth.controller;

import com.example.demo.auth.model.Token;
import com.example.demo.auth.model.dto.request.LoginRequest;
import com.example.demo.auth.model.dto.request.RegisterRequest;
import com.example.demo.auth.model.dto.request.TokenInvalidateRequest;
import com.example.demo.auth.model.dto.request.TokenRefreshRequest;
import com.example.demo.auth.model.dto.response.TokenResponse;
import com.example.demo.auth.model.mapper.TokenToTokenResponseMapper;
import com.example.demo.auth.service.LoginService;
import com.example.demo.auth.service.LogoutService;
import com.example.demo.auth.service.RefreshTokenService;
import com.example.demo.auth.service.RegisterService;
import com.example.demo.common.model.dto.response.CustomResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that handles user authentication and authorization operations.
 * This controller provides endpoints for user registration, login, token refreshing, and logout functionality.
 * It delegates the business logic to the appropriate services: {@link RegisterService}, {@link LoginService},
 * {@link RefreshTokenService}, and {@link LogoutService}.
 */
@RestController
@RequestMapping("/api/v1/authentication/user")
@RequiredArgsConstructor
@Validated
@Tag(name = "Authentication", description = "Handles user authentication and authorization operations.")
public class AuthController {

    private final RegisterService registerService;

    private final LoginService loginService;

    private final RefreshTokenService refreshTokenService;

    private final LogoutService logoutService;

    private final TokenToTokenResponseMapper tokenToTokenResponseMapper = TokenToTokenResponseMapper.initialize();

    /**
     * Endpoint to register a new user.
     *
     * @param registerRequest The {@link RegisterRequest} object containing user registration details.
     * @return A {@link CustomResponse} indicating the success of the registration operation.
     */
    @Operation(
            summary = "Register a new user",
            description = "Registers a new user in the system.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "User successfully registered"),
                    @ApiResponse(responseCode = "400", description = "Invalid request data"),
                    @ApiResponse(responseCode = "409", description = "User already exists")
            }
    )
    @PostMapping("/register")
    public CustomResponse<Void> registerAdmin(@RequestBody @Valid final RegisterRequest registerRequest) {
        registerService.registerUser(registerRequest);
        return CustomResponse.SUCCESS;
    }

    /**
     * Endpoint for user login.
     *
     * @param loginRequest The {@link LoginRequest} object containing user login credentials.
     * @return A {@link CustomResponse} containing a {@link TokenResponse} with the generated tokens.
     */
    @Operation(
            summary = "User login",
            description = "Authenticates a user and returns an access and refresh token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login successful"),
                    @ApiResponse(responseCode = "401", description = "Invalid login credentials")
            }
    )
    @PostMapping("/login")
    public CustomResponse<TokenResponse> loginAdmin(@RequestBody @Valid final LoginRequest loginRequest) {
        final Token token = loginService.login(loginRequest);
        final TokenResponse tokenResponse = tokenToTokenResponseMapper.map(token);
        return CustomResponse.successOf(tokenResponse);
    }

    /**
     * Endpoint to refresh an expired access token.
     *
     * @param tokenRefreshRequest The {@link TokenRefreshRequest} object containing the refresh token.
     * @return A {@link CustomResponse} containing a {@link TokenResponse} with the refreshed tokens.
     */
    @Operation(
            summary = "Refresh access token",
            description = "Refreshes an expired access token using a valid refresh token.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Token successfully refreshed"),
                    @ApiResponse(responseCode = "400", description = "Invalid refresh token"),
                    @ApiResponse(responseCode = "401", description = "Refresh token expired or invalid")
            }
    )
    @PostMapping("/refresh-token")
    public CustomResponse<TokenResponse> refreshToken(@RequestBody @Valid final TokenRefreshRequest tokenRefreshRequest) {
        final Token token = refreshTokenService.refreshToken(tokenRefreshRequest);
        final TokenResponse tokenResponse = tokenToTokenResponseMapper.map(token);
        return CustomResponse.successOf(tokenResponse);
    }

    /**
     * Endpoint to log out a user.
     *
     * @param tokenInvalidateRequest The {@link TokenInvalidateRequest} object containing the token to be invalidated.
     * @return A {@link CustomResponse} indicating the success of the logout operation.
     */
    @Operation(
            summary = "Log out a user",
            description = "Invalidates the provided token, effectively logging out the user.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Logout successful"),
                    @ApiResponse(responseCode = "400", description = "Invalid token provided")
            }
    )
    @PostMapping("/logout")
    public CustomResponse<Void> logout(@RequestBody @Valid final TokenInvalidateRequest tokenInvalidateRequest) {
        logoutService.logout(tokenInvalidateRequest);
        return CustomResponse.SUCCESS;
    }

}
