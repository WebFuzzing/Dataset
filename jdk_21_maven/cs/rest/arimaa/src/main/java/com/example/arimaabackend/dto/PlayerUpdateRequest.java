package com.example.arimaabackend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record PlayerUpdateRequest(
        @NotNull
        Integer rating,

        @NotNull
        Integer ru,

        @NotNull
        Integer gamesPlayed,

        @NotNull
        Integer countryId,

        @Valid
        UserUpdateRequest userUpdate
) {}
