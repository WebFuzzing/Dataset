package com.example.arimaabackend.dto;

public record GameTypeSummary(
    Integer id,
    String name,
    Integer timeIncrement,
    Integer timeReserve
) {}

