package com.example.arimaabackend.migration.steps.neo4j;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.arimaabackend.migration.MigrationContext;
import com.example.arimaabackend.migration.spi.MigrationStep;
import com.example.arimaabackend.migration.spi.MigrationTarget;
import com.example.arimaabackend.migration.support.Neo4jLinkSupport;
import com.example.arimaabackend.migration.support.Neo4jTransactionHelper;
import com.example.arimaabackend.model.sql.MoveEntity;
import com.example.arimaabackend.repository.sql.MoveJpaRepository;

@Service
@Profile("migration")
public class MoveNeo4jMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(MoveNeo4jMigration.class);

    private final MoveJpaRepository moveJpaRepository;
    private final Neo4jLinkSupport neo4jLinkSupport;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public MoveNeo4jMigration(
            MoveJpaRepository moveJpaRepository,
            Neo4jLinkSupport neo4jLinkSupport,
            Neo4jTransactionHelper neo4jTransactionHelper
    ) {
        this.moveJpaRepository = moveJpaRepository;
        this.neo4jLinkSupport = neo4jLinkSupport;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "move";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 80;
    }

    @Override
    @Transactional(readOnly = true)
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), moveJpaRepository.count());
            return;
        }
        int batch = context.batchSize();
        long migrated = 0;
        for (int page = 0; ; page++) {
            var slice = moveJpaRepository.findAll(PageRequest.of(page, batch));
            if (!slice.hasContent()) {
                break;
            }
            List<Map<String, Object>> rows = new ArrayList<>(slice.getNumberOfElements());
            for (MoveEntity e : slice) {
                rows.add(row(e));
            }
            neo4jTransactionHelper.write(() -> {
                neo4jLinkSupport.mergeMovesBatch(rows);
            });
            migrated += rows.size();
            if (!slice.hasNext()) {
                break;
            }
        }
        log.info("[{}] migrated {} moves (nodes + edges in one batch query)", stepName(), migrated);
    }

    private Map<String, Object> row(MoveEntity e) {
        var m = new HashMap<String, Object>();
        m.put("matchId", e.getMatch().getId());
        m.put("moveId", e.getId());
        m.put("turn", e.getTurn());
        m.put("sequence", e.getSequence());
        m.put("direction", e.getDirection());
        m.put("status", e.getStatus());
        m.put("positionId", e.getPosition().getId());
        return m;
    }
}
