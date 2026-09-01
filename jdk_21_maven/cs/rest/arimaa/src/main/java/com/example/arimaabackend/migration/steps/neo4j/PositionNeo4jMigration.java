package com.example.arimaabackend.migration.steps.neo4j;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import com.example.arimaabackend.migration.MigrationContext;
import com.example.arimaabackend.migration.spi.MigrationStep;
import com.example.arimaabackend.migration.spi.MigrationTarget;
import com.example.arimaabackend.migration.support.Neo4jTransactionHelper;
import com.example.arimaabackend.model.neo4j.PositionNode;
import com.example.arimaabackend.model.sql.PositionEntity;
import com.example.arimaabackend.repository.neo4j.PositionNeo4jRepository;
import com.example.arimaabackend.repository.sql.PositionJpaRepository;

@Service
@Profile("migration")
public class PositionNeo4jMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(PositionNeo4jMigration.class);

    private final PositionJpaRepository positionJpaRepository;
    private final PositionNeo4jRepository positionNeo4jRepository;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public PositionNeo4jMigration(
            PositionJpaRepository positionJpaRepository,
            PositionNeo4jRepository positionNeo4jRepository,
            Neo4jTransactionHelper neo4jTransactionHelper
    ) {
        this.positionJpaRepository = positionJpaRepository;
        this.positionNeo4jRepository = positionNeo4jRepository;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "position";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 40;
    }

    @Override
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), positionJpaRepository.count());
            return;
        }
        int batch = context.batchSize();
        long total = positionJpaRepository.count();
        long migrated = 0;
        for (int page = 0; ; page++) {
            var slice = positionJpaRepository.findAll(PageRequest.of(page, batch));
            if (!slice.hasContent()) {
                break;
            }
            List<PositionNode> batchNodes = new ArrayList<>(slice.getNumberOfElements());
            for (PositionEntity e : slice) {
                batchNodes.add(toNode(e));
            }
            neo4jTransactionHelper.write(() -> positionNeo4jRepository.saveAll(batchNodes));
            migrated += batchNodes.size();
            if (!slice.hasNext()) {
                break;
            }
        }
        log.info("[{}] migrated {} positions (total source ~{})", stepName(), migrated, total);
    }

    private PositionNode toNode(PositionEntity e) {
        var n = new PositionNode();
        n.setId(e.getId());
        n.setColor(e.getColor());
        n.setPiece(e.getPiece());
        n.setCoordinate(e.getCoordinate());
        return n;
    }
}
