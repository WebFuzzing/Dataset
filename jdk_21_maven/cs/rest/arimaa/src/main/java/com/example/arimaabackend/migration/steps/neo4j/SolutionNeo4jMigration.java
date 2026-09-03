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
import com.example.arimaabackend.model.neo4j.SolutionNode;
import com.example.arimaabackend.model.sql.SolutionEntity;
import com.example.arimaabackend.repository.neo4j.SolutionNeo4jRepository;
import com.example.arimaabackend.repository.sql.SolutionJpaRepository;

@Service
@Profile("migration")
public class SolutionNeo4jMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(SolutionNeo4jMigration.class);

    private final SolutionJpaRepository solutionJpaRepository;
    private final SolutionNeo4jRepository solutionNeo4jRepository;
    private final Neo4jLinkSupport neo4jLinkSupport;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public SolutionNeo4jMigration(
            SolutionJpaRepository solutionJpaRepository,
            SolutionNeo4jRepository solutionNeo4jRepository,
            Neo4jLinkSupport neo4jLinkSupport,
            Neo4jTransactionHelper neo4jTransactionHelper
    ) {
        this.solutionJpaRepository = solutionJpaRepository;
        this.solutionNeo4jRepository = solutionNeo4jRepository;
        this.neo4jLinkSupport = neo4jLinkSupport;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "solution";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 90;
    }

    @Override
    @Transactional(readOnly = true)
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), solutionJpaRepository.count());
            return;
        }
        int batch = context.batchSize();
        long migrated = 0;
        for (int page = 0; ; page++) {
            var slice = solutionJpaRepository.findAll(PageRequest.of(page, batch));
            if (!slice.hasContent()) {
                break;
            }
            List<SolutionNode> nodes = new ArrayList<>(slice.getNumberOfElements());
            List<Map<String, Object>> linkRows = new ArrayList<>(slice.getNumberOfElements());
            for (SolutionEntity e : slice) {
                nodes.add(toNode(e));
                linkRows.add(linkRow(e));
            }
            neo4jTransactionHelper.write(() -> {
                solutionNeo4jRepository.saveAll(nodes);
                neo4jLinkSupport.mergeHasSolutionEdges(linkRows);
                neo4jLinkSupport.mergeSolutionPositionEdges(linkRows);
            });
            migrated += nodes.size();
            if (!slice.hasNext()) {
                break;
            }
        }
        log.info("[{}] migrated {} solutions (nodes + HAS_SOLUTION edges)", stepName(), migrated);
    }

    private SolutionNode toNode(SolutionEntity e) {
        var n = new SolutionNode();
        n.setId(e.getId());
        n.setTurn(e.getTurn());
        n.setSequence(e.getSequence());
        n.setDirection(e.getDirection());
        n.setStatus(e.getStatus());
        return n;
    }

    private Map<String, Object> linkRow(SolutionEntity e) {
        var m = new HashMap<String, Object>();
        m.put("puzzleId", e.getPuzzle().getId());
        m.put("solutionId", e.getId());
        m.put("positionId", e.getPosition().getId());
        return m;
    }
}
