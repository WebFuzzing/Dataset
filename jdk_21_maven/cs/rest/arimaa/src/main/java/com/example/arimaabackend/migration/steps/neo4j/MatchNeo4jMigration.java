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
import com.example.arimaabackend.model.neo4j.MatchNode;
import com.example.arimaabackend.model.sql.MatchEntity;
import com.example.arimaabackend.repository.neo4j.MatchNeo4jRepository;
import com.example.arimaabackend.repository.sql.MatchJpaRepository;

@Service
@Profile("migration")
public class MatchNeo4jMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(MatchNeo4jMigration.class);

    private final MatchJpaRepository matchJpaRepository;
    private final MatchNeo4jRepository matchNeo4jRepository;
    private final Neo4jLinkSupport neo4jLinkSupport;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public MatchNeo4jMigration(
            MatchJpaRepository matchJpaRepository,
            MatchNeo4jRepository matchNeo4jRepository,
            Neo4jLinkSupport neo4jLinkSupport,
            Neo4jTransactionHelper neo4jTransactionHelper
    ) {
        this.matchJpaRepository = matchJpaRepository;
        this.matchNeo4jRepository = matchNeo4jRepository;
        this.neo4jLinkSupport = neo4jLinkSupport;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "match";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 70;
    }

    @Override
    @Transactional(readOnly = true)
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), matchJpaRepository.count());
            return;
        }
        var entities = matchJpaRepository.findAllForMigration();
        var nodes = entities.stream().map(this::toNode).toList();
        List<Map<String, Object>> linkRows = entities.stream().map(this::linkRow).toList();
        neo4jTransactionHelper.write(() -> {
            matchNeo4jRepository.saveAll(nodes);
            neo4jLinkSupport.mergeMatchReferenceEdges(linkRows);
        });
        log.info("[{}] migrated {} matches", stepName(), nodes.size());
    }

    private MatchNode toNode(MatchEntity e) {
        var n = new MatchNode();
        n.setId(e.getId());
        n.setTerminationType(e.getTerminationType());
        n.setMatchResult(e.getMatchResult());
        n.setGoldRating(e.getGoldRating());
        n.setSilverRating(e.getSilverRating());
        n.setTimestamp(e.getTimestamp());
        return n;
    }

    private Map<String, Object> linkRow(MatchEntity e) {
        var m = new HashMap<String, Object>();
        m.put("matchId", e.getId());
        m.put("silverPlayerId", e.getSilverPlayer().getId());
        m.put("goldPlayerId", e.getGoldPlayer().getId());
        m.put("eventId", e.getEvent().getId());
        m.put("gameTypeId", e.getGameType().getId());
        return m;
    }
}
