package com.example.arimaabackend.migration.spi;

import java.util.Locale;

public enum MigrationTarget {
    NEO4J,
    MONGODB;

    public static MigrationTarget fromPropertyValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Migration target must not be blank");
        }
        var normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "neo4j" -> NEO4J;
            case "mongodb", "mongo" -> MONGODB;
            default -> throw new IllegalArgumentException("Unsupported migration target: " + value);
        };
    }
}
