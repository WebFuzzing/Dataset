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
import com.example.arimaabackend.model.neo4j.CountryNode;
import com.example.arimaabackend.model.sql.CountryEntity;
import com.example.arimaabackend.repository.neo4j.CountryNeo4jRepository;
import com.example.arimaabackend.repository.sql.CountryJpaRepository;

@Service
@Profile("migration")
public class CountryNeo4jMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(CountryNeo4jMigration.class);

    private final CountryJpaRepository countryJpaRepository;
    private final CountryNeo4jRepository countryNeo4jRepository;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public CountryNeo4jMigration(
            CountryJpaRepository countryJpaRepository,
            CountryNeo4jRepository countryNeo4jRepository,
            Neo4jTransactionHelper neo4jTransactionHelper
    ) {
        this.countryJpaRepository = countryJpaRepository;
        this.countryNeo4jRepository = countryNeo4jRepository;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "country";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 10;
    }

    @Override
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), countryJpaRepository.count());
            return;
        }
        var entities = countryJpaRepository.findAll();
        var nodes = entities.stream().map(this::toNode).toList();
        neo4jTransactionHelper.write(() -> countryNeo4jRepository.saveAll(nodes));
        log.info("[{}] migrated {} countries", stepName(), nodes.size());
    }

    private CountryNode toNode(CountryEntity e) {
        var n = new CountryNode();
        n.setId(e.getId());
        n.setName(e.getName());
        return n;
    }
}
