package com.example.arimaabackend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserCreateRequest(
        @NotBlank @Size(max = 50)
        String username,

        @Email @NotBlank @Size(max = 254)
        String email,

        @NotBlank @Size(min = 8, max = 100)
        String password
) {}

