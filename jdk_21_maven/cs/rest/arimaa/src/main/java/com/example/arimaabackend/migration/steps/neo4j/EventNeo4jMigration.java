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
import com.example.arimaabackend.model.neo4j.EventNode;
import com.example.arimaabackend.model.sql.EventEntity;
import com.example.arimaabackend.repository.neo4j.EventNeo4jRepository;
import com.example.arimaabackend.repository.sql.EventJpaRepository;

@Service
@Profile("migration")
public class EventNeo4jMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(EventNeo4jMigration.class);

    private final EventJpaRepository eventJpaRepository;
    private final EventNeo4jRepository eventNeo4jRepository;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public EventNeo4jMigration(
            EventJpaRepository eventJpaRepository,
            EventNeo4jRepository eventNeo4jRepository,
            Neo4jTransactionHelper neo4jTransactionHelper
    ) {
        this.eventJpaRepository = eventJpaRepository;
        this.eventNeo4jRepository = eventNeo4jRepository;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "event";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 20;
    }

    @Override
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), eventJpaRepository.count());
            return;
        }
        var nodes = eventJpaRepository.findAll().stream().map(this::toNode).toList();
        neo4jTransactionHelper.write(() -> eventNeo4jRepository.saveAll(nodes));
        log.info("[{}] migrated {} events", stepName(), nodes.size());
    }

    private EventNode toNode(EventEntity e) {
        var n = new EventNode();
        n.setId(e.getId());
        n.setName(e.getName());
        n.setRated(e.getRated());
        return n;
    }
}
