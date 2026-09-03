package com.example.arimaabackend.dto;

import java.time.Instant;

public record PlayerResponse(
        Integer id,
        String username,
        String email,
        Integer rating,
        Integer ru,
        Integer gamesPlayed,
        Instant createTime,
        Integer countryId
) {}
