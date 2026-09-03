package com.example.arimaabackend.migration.steps.neo4j;

import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.example.arimaabackend.migration.MigrationContext;
import com.example.arimaabackend.migration.spi.MigrationStep;
import com.example.arimaabackend.migration.spi.MigrationTarget;
import com.example.arimaabackend.migration.support.Neo4jTransactionHelper;
import com.example.arimaabackend.model.neo4j.PuzzleNode;
import com.example.arimaabackend.model.sql.PuzzleEntity;
import com.example.arimaabackend.repository.neo4j.PuzzleNeo4jRepository;
import com.example.arimaabackend.repository.sql.PuzzleJpaRepository;

@Service
@Profile("migration")
public class PuzzleNeo4jMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(PuzzleNeo4jMigration.class);

    private final PuzzleJpaRepository puzzleJpaRepository;
    private final PuzzleNeo4jRepository puzzleNeo4jRepository;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public PuzzleNeo4jMigration(
            PuzzleJpaRepository puzzleJpaRepository,
            PuzzleNeo4jRepository puzzleNeo4jRepository,
            Neo4jTransactionHelper neo4jTransactionHelper
    ) {
        this.puzzleJpaRepository = puzzleJpaRepository;
        this.puzzleNeo4jRepository = puzzleNeo4jRepository;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "puzzle";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 50;
    }

    @Override
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), puzzleJpaRepository.count());
            return;
        }
        var nodes = puzzleJpaRepository.findAll().stream().map(this::toNode).toList();
        neo4jTransactionHelper.write(() -> puzzleNeo4jRepository.saveAll(nodes));
        log.info("[{}] migrated {} puzzles", stepName(), nodes.size());
    }

    private PuzzleNode toNode(PuzzleEntity e) {
        var n = new PuzzleNode();
        n.setId(e.getId());
        n.setName(e.getName());
        n.setObjective(e.getObjective());
        n.setPlayerSide(e.getPlayerSide());
        n.setRounds(e.getRounds());
        return n;
    }
}
