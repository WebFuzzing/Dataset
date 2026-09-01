package com.example.arimaabackend.dto;

import jakarta.validation.constraints.NotNull;

public record PlayerCreateRequest(
        @NotNull
        Long userId,

        Integer rating,

        Integer ru,

        Integer gamesPlayed,

        Integer countryId,

        String country
) {}
