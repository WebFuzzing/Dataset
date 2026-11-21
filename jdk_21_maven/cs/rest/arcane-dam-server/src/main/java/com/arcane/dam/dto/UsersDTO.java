package com.arcane.dam.dto;

import com.arcane.dam.entity.Space;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UsersDTO {

    @NotNull(message = "ID cannot be null")
    private String id;

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String userName;

    @NotBlank(message = "First name cannot be blank")
    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Email must be a valid email address")
    private String email;

    @NotBlank(message = "Password cannot be blank")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;

    @NotBlank(message = "Role cannot be blank")
    @Pattern(regexp = "USER|ADMIN", message = "Role must be either 'USER' or 'ADMIN'")
    private String role;

    @NotNull(message = "Spaces cannot be null")
    private List<Space> spaces;

    @PastOrPresent(message = "CreatedAt must be in the past or present")
    private Instant createdAt;

    @PastOrPresent(message = "UpdatedAt must be in the past or present")
    private Instant updatedAt;

    private Boolean isEnabled;
}
