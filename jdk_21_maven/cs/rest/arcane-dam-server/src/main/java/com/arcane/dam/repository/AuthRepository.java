package com.arcane.dam.repository;

// MODIFIED: migrated from AWS SDK v1 DynamoDBMapper to AWS SDK v2 DynamoDbEnhancedClient
import com.arcane.dam.entity.Users;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

@Repository
@RequiredArgsConstructor
public class AuthRepository {
    private static final TableSchema<Users> TABLE_SCHEMA = TableSchema.fromBean(Users.class);

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private DynamoDbTable<Users> usersTable() {
        return dynamoDbEnhancedClient.table("users", TABLE_SCHEMA);
    }

    public Users findUserByEmail(String email) {
        if (email == null) {
            return null;
        }
        Expression filter = Expression.builder()
                .expression("email = :email")
                .expressionValues(Map.of(":email", AttributeValue.fromS(email)))
                .build();

        return usersTable().scan(ScanEnhancedRequest.builder().filterExpression(filter).build())
                .items()
                .stream()
                .findFirst()
                .orElse(null);
    }
}
