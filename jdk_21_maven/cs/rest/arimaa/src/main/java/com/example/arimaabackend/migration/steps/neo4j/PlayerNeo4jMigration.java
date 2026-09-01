package com.example.arimaabackend.migration.steps.neo4j;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.arimaabackend.migration.MigrationContext;
import com.example.arimaabackend.migration.spi.MigrationStep;
import com.example.arimaabackend.migration.spi.MigrationTarget;
import com.example.arimaabackend.migration.support.Neo4jLinkSupport;
import com.example.arimaabackend.migration.support.Neo4jTransactionHelper;
import com.example.arimaabackend.model.neo4j.PlayerNode;
import com.example.arimaabackend.model.neo4j.UserNode;
import com.example.arimaabackend.model.sql.PlayerEntity;
import com.example.arimaabackend.repository.neo4j.PlayerNeo4jRepository;
import com.example.arimaabackend.repository.sql.PlayerJpaRepository;

@Service
@Profile("migration")
public class PlayerNeo4jMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(PlayerNeo4jMigration.class);

    private final PlayerJpaRepository playerJpaRepository;
    private final PlayerNeo4jRepository playerNeo4jRepository;
    private final Neo4jLinkSupport neo4jLinkSupport;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public PlayerNeo4jMigration(
            PlayerJpaRepository playerJpaRepository,
            PlayerNeo4jRepository playerNeo4jRepository,
            Neo4jLinkSupport neo4jLinkSupport,
            Neo4jTransactionHelper neo4jTransactionHelper
    ) {
        this.playerJpaRepository = playerJpaRepository;
        this.playerNeo4jRepository = playerNeo4jRepository;
        this.neo4jLinkSupport = neo4jLinkSupport;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "player";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 60;
    }

    @Override
    @Transactional(readOnly = true)
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), playerJpaRepository.count());
            return;
        }
        var entities = playerJpaRepository.findAllWithUserAndCountry();
        var nodes = entities.stream().map(this::toNode).toList();
        List<Map<String, Object>> countryRows = entities.stream().map(this::countryLinkRow).toList();
        List<Map<String, Object>> userRows = entities.stream().map(this::userLinkRow).toList();
        neo4jTransactionHelper.write(() -> {
            playerNeo4jRepository.saveAll(nodes);
            neo4jLinkSupport.mergePlayerCountryEdges(countryRows);
            neo4jLinkSupport.mergePlayerUserEdges(userRows);
        });
        log.info("[{}] migrated {} players", stepName(), nodes.size());
    }

    private PlayerNode toNode(PlayerEntity e) {
        var n = new PlayerNode();
        n.setId(e.getId());
        n.setRating(e.getRating());
        n.setRu(e.getRu());
        n.setGamesPlayed(e.getGamesPlayed());
        return n;
    }


    private Map<String, Object> countryLinkRow(PlayerEntity e) {
        var m = new HashMap<String, Object>();
        m.put("playerId", e.getId());
        m.put("countryId", e.getCountry().getId());
        return m;
    }

    private Map<String, Object> userLinkRow(PlayerEntity e) {
        var m = new HashMap<String, Object>();
        m.put("playerId", e.getId());
        m.put("userId", e.getUser().getId());
        return m;
    }
}
