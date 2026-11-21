package com.arcane.dam.repository;

// MODIFIED: migrated from AWS SDK v1 DynamoDBMapper to AWS SDK v2 DynamoDbEnhancedClient
import com.arcane.dam.entity.Space;
import com.arcane.dam.entity.Users;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserRepository {
    private static final TableSchema<Users> TABLE_SCHEMA = TableSchema.fromBean(Users.class);

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private DynamoDbTable<Users> usersTable() {
        return dynamoDbEnhancedClient.table("users", TABLE_SCHEMA);
    }

    public Users save(Users user) {
        if (user.getId() == null || user.getId().isBlank()) {
            user.setId(UUID.randomUUID().toString());
        }
        usersTable().putItem(user);
        return user;
    }

    public List<Users> getAllUsers() {
        return usersTable().scan().items().stream().collect(Collectors.toList());
    }

    public Users getUsersById(String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        return usersTable().getItem(Key.builder().partitionValue(id).build());
    }

    public boolean delete(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        Users deleted = usersTable().deleteItem(Key.builder().partitionValue(id).build());
        return deleted != null;
    }

    public boolean update(String id, Users user) {
        if (id == null || id.isBlank() || user == null) {
            return false;
        }
        user.setId(id);
        Expression condition = Expression.builder()
                .expression("attribute_exists(id)")
                .build();
        try {
            usersTable().putItem(PutItemEnhancedRequest.builder(Users.class)
                    .item(user)
                    .conditionExpression(condition)
                    .build());
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
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

    public Users getUserByUserName(String userName) {
        if (userName == null) {
            return null;
        }
        Expression filter = Expression.builder()
                .expression("username = :userName")
                .expressionValues(Map.of(":userName", AttributeValue.fromS(userName)))
                .build();

        return usersTable().scan(ScanEnhancedRequest.builder().filterExpression(filter).build())
                .items()
                .stream()
                .findFirst()
                .orElse(null);
    }

    public Users updateSpace(String userId, Space updatedSpace) {
        Users user = getUsersById(userId);

        if (user != null) {
            Optional<Space> existingSpace = Optional.empty();
            try {
                if (user.getSpaces() != null && updatedSpace.getId() != null) {
                    Space finalUpdatedSpace = updatedSpace;
                    existingSpace = user.getSpaces().stream()
                            .filter(space -> finalUpdatedSpace.getId().equals(space.getId()))
                            .findFirst();
                }
            } catch (RuntimeException e) {
                log.atError().log(e.getMessage());
            }

            if (existingSpace.isPresent()) {
                Space space = existingSpace.get();
                space.setName(updatedSpace.getName());
                space.setDescription(updatedSpace.getDescription());
                space.setTags(updatedSpace.getTags());
            } else {
                updatedSpace = new Space(updatedSpace.getId(), updatedSpace.getName(), updatedSpace.getDescription(), updatedSpace.getTags());
                if (user.getSpaces() == null) {
                    user.setSpaces(new ArrayList<>(Collections.singletonList(updatedSpace)));
                } else {
                    user.getSpaces().add(updatedSpace);
                }
            }
            usersTable().putItem(user);
            return user;
        } else {
            throw new RuntimeException("User with ID " + userId + " not found.");
        }
    }
}
