package dev.mirodil.testing_system.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserLoginRequestDTO(
        @Email @Size(min = 5, max = 50)
        String email,
        @NotBlank @Size(min = 5, max = 50)
        String password
) {
}
