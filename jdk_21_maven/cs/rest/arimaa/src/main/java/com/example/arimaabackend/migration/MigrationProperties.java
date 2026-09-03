package com.example.arimaabackend.migration;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.example.arimaabackend.migration.spi.MigrationTarget;

/**
 * <p>Configure under prefix {@code migration}.</p>
 * <ul>
 *   <li>{@code enabled-targets} — if non-empty, only those targets run ({@code neo4j}, {@code mongodb}).</li>
 *   <li>{@code enabled-steps} — if non-empty, only those steps run (see each migration's {@code stepName()}).
 *       Step ids are case-insensitive. If empty, all steps run in order.</li>
 *   <li>{@code batch-size} — page size for large JPA reads (default 500).</li>
 *   <li>{@code dry-run} — if true, steps log counts and skip target writes.</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "migration")
public class MigrationProperties {

    /**
     * Target names to run; empty = all targets.
     */
    private List<String> enabledTargets = new ArrayList<>();

    /**
     * Step names to run; empty = all steps.
     */
    private List<String> enabledSteps = new ArrayList<>();

    private int batchSize = 1000;

    private boolean dryRun = false;

    private boolean exitOnComplete = true;

    public List<String> getEnabledTargets() {
        return enabledTargets;
    }

    public void setEnabledTargets(List<String> enabledTargets) {
        this.enabledTargets = enabledTargets != null ? enabledTargets : new ArrayList<>();
    }

    public List<String> getEnabledSteps() {
        return enabledSteps;
    }

    public void setEnabledSteps(List<String> enabledSteps) {
        this.enabledSteps = enabledSteps != null ? enabledSteps : new ArrayList<>();
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isDryRun() {
        return dryRun;
    }

    public void setDryRun(boolean dryRun) {
        this.dryRun = dryRun;
    }

    public boolean isExitOnComplete() {
        return exitOnComplete;
    }

    public void setExitOnComplete(boolean exitOnComplete) {
        this.exitOnComplete = exitOnComplete;
    }

    /**
     * When {@link #enabledSteps} is empty, all steps run; otherwise only listed steps (case-insensitive).
     */
    public boolean isStepEnabled(String stepName) {
        if (enabledSteps == null || enabledSteps.isEmpty()) {
            return true;
        }
        for (String s : enabledSteps) {
            if (s != null && s.equalsIgnoreCase(stepName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * When {@link #enabledTargets} is empty, all targets run; otherwise only listed targets.
     */
    public boolean isTargetEnabled(MigrationTarget target) {
        return getEnabledTargetSet().contains(target);
    }

    /**
     * Resolved set of enabled targets. Empty configuration means all targets.
     */
    public Set<MigrationTarget> getEnabledTargetSet() {
        var resolved = EnumSet.noneOf(MigrationTarget.class);
        resolved.addAll(getEnabledTargetOrder());
        return resolved;
    }

    /**
     * Resolved enabled targets in run order.
     * <p>When empty, all targets are enabled in enum declaration order.</p>
     */
    public List<MigrationTarget> getEnabledTargetOrder() {
        if (enabledTargets == null || enabledTargets.isEmpty()) {
            return List.of(MigrationTarget.values());
        }
        var ordered = new ArrayList<MigrationTarget>();
        for (String rawTarget : enabledTargets) {
            var target = MigrationTarget.fromPropertyValue(rawTarget);
            if (!ordered.contains(target)) {
                ordered.add(target);
            }
        }
        return ordered;
    }
}
