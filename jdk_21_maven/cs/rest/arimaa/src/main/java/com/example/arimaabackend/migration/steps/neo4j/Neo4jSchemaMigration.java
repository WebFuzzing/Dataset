package com.example.arimaabackend.migration.steps.neo4j;

import java.util.EnumSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.stereotype.Service;

import com.example.arimaabackend.migration.MigrationContext;
import com.example.arimaabackend.migration.spi.MigrationStep;
import com.example.arimaabackend.migration.spi.MigrationTarget;
import com.example.arimaabackend.migration.support.Neo4jTransactionHelper;

@Service
@Profile("migration")
public class Neo4jSchemaMigration implements MigrationStep {

    private static final Logger log = LoggerFactory.getLogger(Neo4jSchemaMigration.class);

    private final Neo4jClient neo4jClient;
    private final Neo4jTransactionHelper neo4jTransactionHelper;

    public Neo4jSchemaMigration(Neo4jClient neo4jClient, Neo4jTransactionHelper neo4jTransactionHelper) {
        this.neo4jClient = neo4jClient;
        this.neo4jTransactionHelper = neo4jTransactionHelper;
    }

    @Override
    public String stepName() {
        return "neo4j-schema";
    }

    @Override
    public Set<MigrationTarget> targets() {
        return EnumSet.of(MigrationTarget.NEO4J);
    }

    @Override
    public int getOrder() {
        return 0;
    }

    @Override
    public void migrate(MigrationContext context) {
        if (context.dryRun()) {
            log.info("[{}] dry-run: would ensure constraints/indexes", stepName());
            return;
        }

        neo4jTransactionHelper.write(() -> {
            // Lookups used heavily by migration MATCH clauses.
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (m:Match) REQUIRE m.id IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (p:Player) REQUIRE p.id IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (e:Event) REQUIRE e.id IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (g:GameType) REQUIRE g.id IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (c:Country) REQUIRE c.id IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (z:Puzzle) REQUIRE z.id IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (s:Solution) REQUIRE s.id IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (v:Move) REQUIRE v.id IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (p:Position) REQUIRE p.id IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (u:User) REQUIRE u.id IS UNIQUE").run();

            // Uniques from SQL schema (identity on User, not Player).
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (c:Country) REQUIRE c.name IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (z:Puzzle) REQUIRE z.name IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (u:User) REQUIRE u.username IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (u:User) REQUIRE u.email IS UNIQUE").run();

            // Synthetic keys used for move/opening grouping.
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (ml:MoveList) REQUIRE ml.key IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (t:Turn) REQUIRE t.key IS UNIQUE").run();
            neo4jClient.query("CREATE CONSTRAINT IF NOT EXISTS FOR (o:Opening) REQUIRE o.key IS UNIQUE").run();
        });

        log.info("[{}] ensured Neo4j constraints/indexes", stepName());
    }
}

