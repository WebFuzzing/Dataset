package com.example.arimaabackend.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        String role,
        Instant createdAt,
        Instant updatedAt
) {}

