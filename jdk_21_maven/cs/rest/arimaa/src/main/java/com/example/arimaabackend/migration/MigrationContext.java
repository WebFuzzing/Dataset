package com.example.arimaabackend.migration;

/**
 * Per-run migration settings passed into each {@link com.example.arimaabackend.migration.spi.MigrationStep}.
 */
public record MigrationContext(MigrationProperties properties) {

    public int batchSize() {
        return properties.getBatchSize();
    }

    public boolean dryRun() {
        return properties.isDryRun();
    }
}
