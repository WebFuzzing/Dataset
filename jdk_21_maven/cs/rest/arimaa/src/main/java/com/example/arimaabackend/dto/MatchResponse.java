package com.example.arimaabackend.dto;

import java.time.Instant;

public record MatchResponse(
    Integer id,
    String terminationType,
    PlayerSummary silverPlayer,
    PlayerSummary goldPlayer,
    Integer goldRating,
    Integer silverRating,
    String matchResult,
    EventSummary event,
    GameTypeSummary gameType,
    Instant timestamp
) {}
