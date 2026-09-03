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
import com.example.arimaabackend.model.neo4j.GameTypeNode;
import com.example.arimaabackend.model.sql.GameTypeEntity;
import com.example.arimaabackend.repository.neo4j.GameTypeNeo4jRepository;
import com.example.arimaabackend.repository.sql.GameTypeJpaRepository;

@Service
@Profile("migration")
public class GameTypeNeo4jMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(GameTypeNeo4jMigration.class);

    private final GameTypeJpaRepository gameTypeJpaRepository;
    private final GameTypeNeo4jRepository gameTypeNeo4jRepository;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public GameTypeNeo4jMigration(
            GameTypeJpaRepository gameTypeJpaRepository,
            GameTypeNeo4jRepository gameTypeNeo4jRepository,
            Neo4jTransactionHelper neo4jTransactionHelper
    ) {
        this.gameTypeJpaRepository = gameTypeJpaRepository;
        this.gameTypeNeo4jRepository = gameTypeNeo4jRepository;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "game-type";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 30;
    }

    @Override
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), gameTypeJpaRepository.count());
            return;
        }
        var nodes = gameTypeJpaRepository.findAll().stream().map(this::toNode).toList();
        neo4jTransactionHelper.write(() -> gameTypeNeo4jRepository.saveAll(nodes));
        log.info("[{}] migrated {} game types", stepName(), nodes.size());
    }

    private GameTypeNode toNode(GameTypeEntity e) {
        var n = new GameTypeNode();
        n.setId(e.getId());
        n.setName(e.getName());
        n.setTimeIncrement(e.getTimeIncrement());
        n.setTimeReserve(e.getTimeReserve());
        return n;
    }
}
