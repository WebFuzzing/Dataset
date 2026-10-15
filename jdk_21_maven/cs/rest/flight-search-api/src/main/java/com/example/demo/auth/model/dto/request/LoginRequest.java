package com.example.demo.auth.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a request for user login containing email and password.
 * This class is used to capture login credentials when a user attempts to log in.
 * It is validated to ensure that both the email and password fields are not blank.
 */
@Getter
@Setter
@Builder
public class LoginRequest {

    @NotBlank
    private String email;

    @NotBlank
    private String password;

}
