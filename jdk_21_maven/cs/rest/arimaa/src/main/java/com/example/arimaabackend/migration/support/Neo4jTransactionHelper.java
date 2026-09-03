package com.example.arimaabackend.migration.support;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Imperative Spring Data Neo4j writes require a Neo4j-managed transaction; the default
 * application {@code transactionManager} is typically JPA, so repository {@code save*} calls
 * must run inside this helper.
 */
@Component
@Profile("migration")
public class Neo4jTransactionHelper {

    private final TransactionTemplate neo4jWrite;

    public Neo4jTransactionHelper(Neo4jTransactionManager neo4jTransactionManager) {
        this.neo4jWrite = new TransactionTemplate(neo4jTransactionManager);
    }

    public void write(Runnable work) {
        neo4jWrite.executeWithoutResult(status -> work.run());
    }
}
