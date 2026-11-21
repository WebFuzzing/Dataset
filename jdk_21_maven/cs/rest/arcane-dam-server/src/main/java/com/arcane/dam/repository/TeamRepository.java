package com.arcane.dam.repository;

// MODIFIED: migrated from AWS SDK v1 DynamoDBMapper to AWS SDK v2 DynamoDbEnhancedClient
import com.arcane.dam.entity.Team;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class TeamRepository {
    private static final TableSchema<Team> TABLE_SCHEMA = TableSchema.fromBean(Team.class);

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private DynamoDbTable<Team> teamTable() {
        return dynamoDbEnhancedClient.table("team", TABLE_SCHEMA);
    }

    public Team save(Team team) {
        if (team.getId() == null || team.getId().isBlank()) {
            team.setId(UUID.randomUUID().toString());
        }
        teamTable().putItem(team);
        return team;
    }

    public List<Team> findAll() {
        return teamTable().scan().items().stream().collect(Collectors.toList());
    }

    public boolean delete(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        Team deleted = teamTable().deleteItem(Key.builder().partitionValue(id).build());
        return deleted != null;
    }

    public boolean update(Team team) {
        if (team == null || team.getId() == null || team.getId().isBlank()) {
            return false;
        }
        Expression condition = Expression.builder()
                .expression("attribute_exists(id)")
                .build();
        try {
            teamTable().putItem(PutItemEnhancedRequest.builder(Team.class)
                    .item(team)
                    .conditionExpression(condition)
                    .build());
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }
}
