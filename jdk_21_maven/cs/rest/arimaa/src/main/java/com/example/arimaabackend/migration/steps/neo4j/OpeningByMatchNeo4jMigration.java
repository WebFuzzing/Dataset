package com.example.arimaabackend.migration.steps.neo4j;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.arimaabackend.migration.MigrationContext;
import com.example.arimaabackend.migration.spi.MigrationStep;
import com.example.arimaabackend.migration.spi.MigrationTarget;
import com.example.arimaabackend.migration.support.Neo4jLinkSupport;
import com.example.arimaabackend.migration.support.Neo4jTransactionHelper;

@Service
@Profile("migration")
public class OpeningByMatchNeo4jMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(OpeningByMatchNeo4jMigration.class);

    private final JdbcTemplate jdbcTemplate;
    private final Neo4jLinkSupport neo4jLinkSupport;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public OpeningByMatchNeo4jMigration(
            JdbcTemplate jdbcTemplate,
            Neo4jLinkSupport neo4jLinkSupport,
            Neo4jTransactionHelper neo4jTransactionHelper
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.neo4jLinkSupport = neo4jLinkSupport;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "opening-by-match";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    public void migrate(MigrationContext context) {
        Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM OpeningsByMatch", Long.class);
        if (count == null) {
            count = 0L;
        }
        if (context.dryRun()) {
            log.info("[{}] dry-run: would migrate {} rows", stepName(), count);
            return;
        }
        int batch = context.batchSize();
        List<Map<String, Object>> rows = jdbcTemplate.query(
                "SELECT id, matches_id, position_id FROM OpeningsByMatch",
                (rs, rowNum) -> {
                    var m = new HashMap<String, Object>();
                    m.put("openingId", rs.getInt("id"));
                    m.put("matchId", rs.getInt("matches_id"));
                    m.put("positionId", rs.getInt("position_id"));
                    return m;
                }
        );
        for (int i = 0; i < rows.size(); i += batch) {
            var batchRows = rows.subList(i, Math.min(i + batch, rows.size()));
            neo4jTransactionHelper.write(() -> neo4jLinkSupport.mergeOpeningByMatch(batchRows));
        }
        log.info("[{}] migrated {} opening-by-match links", stepName(), rows.size());
    }
}
