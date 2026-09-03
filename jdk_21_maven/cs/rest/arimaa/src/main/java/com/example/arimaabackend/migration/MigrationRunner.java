package com.example.arimaabackend.migration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.example.arimaabackend.migration.spi.MigrationStep;
import com.example.arimaabackend.migration.spi.MigrationTarget;

@Component
@Profile("migration")
public class MigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(MigrationRunner.class);

    private final List<MigrationStep> migrations;
    private final MigrationProperties migrationProperties;
    private final ApplicationContext applicationContext;

    public MigrationRunner(List<MigrationStep> migrations, MigrationProperties migrationProperties, ApplicationContext applicationContext) {
        this.migrations = new ArrayList<>(migrations);
        this.migrations.sort(Comparator.comparingInt(MigrationStep::getOrder));
        this.migrationProperties = migrationProperties;
        this.applicationContext = applicationContext;
    }

    @Override
    public void run(String... args) {
        MigrationContext context = new MigrationContext(migrationProperties);
        Set<MigrationTarget> enabledTargets = migrationProperties.getEnabledTargetSet();
        List<MigrationTarget> targetOrder = migrationProperties.getEnabledTargetOrder();

        logStart(enabledTargets, targetOrder);

        List<MigrationStep> stepsEnabledByName = filterStepsByName();
        Set<MigrationStep> executedSteps = runByTargetGroups(context, targetOrder, stepsEnabledByName);

        logStepsSkippedByTarget(stepsEnabledByName, executedSteps);
        log.info("Data migration completed.");

        System.exit(SpringApplication.exit(applicationContext, () -> 0));
    }

    private void logStart(Set<MigrationTarget> enabledTargets, List<MigrationTarget> targetOrder) {
        log.info(
                "Starting data migration (dryRun={}, batchSize={}, enabledTargets={}, targetOrder={}, enabledSteps={})",
                migrationProperties.isDryRun(),
                migrationProperties.getBatchSize(),
                migrationProperties.getEnabledTargets().isEmpty() ? "ALL" : enabledTargets,
                targetOrder,
                migrationProperties.getEnabledSteps().isEmpty() ? "ALL" : migrationProperties.getEnabledSteps()
        );
    }

    private List<MigrationStep> filterStepsByName() {
        List<MigrationStep> stepsEnabledByName = new ArrayList<>();
        for (MigrationStep step : migrations) {
            if (!migrationProperties.isStepEnabled(step.stepName())) {
                log.info("Skipping step '{}' (not in migration.enabled-steps)", step.stepName());
                continue;
            }
            stepsEnabledByName.add(step);
        }
        return stepsEnabledByName;
    }

    private Set<MigrationStep> runByTargetGroups(
            MigrationContext context,
            List<MigrationTarget> targetOrder,
            List<MigrationStep> stepsEnabledByName
    ) {
        Set<MigrationStep> executedSteps = new HashSet<>();
        for (MigrationTarget target : targetOrder) {
            log.info("Running target group '{}'", target);
            runTargetGroup(context, target, stepsEnabledByName, executedSteps);
        }
        return executedSteps;
    }

    private void runTargetGroup(
            MigrationContext context,
            MigrationTarget target,
            List<MigrationStep> stepsEnabledByName,
            Set<MigrationStep> executedSteps
    ) {
        for (MigrationStep step : stepsEnabledByName) {
            if (!step.targets().contains(target) || executedSteps.contains(step)) {
                continue;
            }
            runStep(context, step);
            executedSteps.add(step);
        }
    }

    private void runStep(MigrationContext context, MigrationStep step) {
        long startedAt = System.nanoTime();
        try {
            step.migrate(context);
        } catch (RuntimeException ex) {
            log.error("Migration step '{}' failed: {}", step.stepName(), ex.getMessage(), ex);
            throw ex;
        }
        long durationMs = (System.nanoTime() - startedAt) / 1_000_000L;
        log.info("Finished step '{}' in {} ms", step.stepName(), durationMs);
    }

    private void logStepsSkippedByTarget(List<MigrationStep> stepsEnabledByName, Set<MigrationStep> executedSteps) {
        for (MigrationStep step : stepsEnabledByName) {
            if (!executedSteps.contains(step)) {
                log.info("Skipping step '{}' (target not enabled: {})", step.stepName(), step.targets());
            }
        }
    }
}
