package dev.mirodil.testing_system.dtos;

import dev.mirodil.testing_system.models.enums.UserGender;
import dev.mirodil.testing_system.models.enums.UserRole;
import dev.mirodil.testing_system.validations.UniqueEmail;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserCreateRequestDTO(
        @Email @Size(min = 5, max = 50) @UniqueEmail
        String email,
        @NotBlank @Size(min = 5, max = 50)
        String password,
        @NotBlank @Size(max = 50)
        String fname,
        @NotBlank @Size(max = 50)
        String lname,
        @NotNull
        UserGender gender,
        @NotNull
        UserRole role
) implements UserRequestDTO {
}
