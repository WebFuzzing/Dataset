package com.example.arimaabackend.migration.spi;

import java.util.Set;

import org.springframework.core.Ordered;

import com.example.arimaabackend.migration.MigrationContext;

/**
 * One modular step in the data migration pipeline (e.g. SQL → Neo4j, SQL → MongoDB, or both).
 * {@link Ordered#getOrder()} defines run order (lower first). {@link #stepName()} is used for
 * {@code migration.enabled-steps} filtering.
 * <p>Concrete implementations typically live in {@code com.example.arimaabackend.migration.steps}.</p>
 */
public interface MigrationStep extends Ordered {

    /**
     * Stable step id, e.g. {@code country}. Compared case-insensitively to configured enabled steps.
     */
    String stepName();

    Set<MigrationTarget> targets();

    void migrate(MigrationContext context);
}
