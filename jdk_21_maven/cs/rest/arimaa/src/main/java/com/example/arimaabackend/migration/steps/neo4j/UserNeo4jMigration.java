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
import com.example.arimaabackend.model.neo4j.UserNode;
import com.example.arimaabackend.model.sql.UserEntity;
import com.example.arimaabackend.repository.neo4j.UserNeo4jRepository;
import com.example.arimaabackend.repository.sql.UserJpaRepository;

@Service
@Profile("migration")
public class UserNeo4jMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(UserNeo4jMigration.class);

    private final UserJpaRepository userJpaRepository;
    private final UserNeo4jRepository userNeo4jRepository;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public UserNeo4jMigration(
            UserJpaRepository userJpaRepository,
            UserNeo4jRepository userNeo4jRepository,
            Neo4jTransactionHelper neo4jTransactionHelper
    ) {
        this.userJpaRepository = userJpaRepository;
        this.userNeo4jRepository = userNeo4jRepository;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "user";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 55;
    }

    @Override
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), userJpaRepository.count());
            return;
        }
        var nodes = userJpaRepository.findAll().stream().map(this::toNode).toList();
        neo4jTransactionHelper.write(() -> userNeo4jRepository.saveAll(nodes));
        log.info("[{}] migrated {} users", stepName(), nodes.size());
    }

    private UserNode toNode(UserEntity e) {
        var n = new UserNode();
        n.setId(e.getId());
        n.setUsername(e.getUsername());
        n.setEmail(e.getEmail());
        n.setRole(e.getRole());
        n.setCreatedAt(e.getCreatedAt());
        n.setUpdatedAt(e.getUpdatedAt());
        return n;
    }
}
