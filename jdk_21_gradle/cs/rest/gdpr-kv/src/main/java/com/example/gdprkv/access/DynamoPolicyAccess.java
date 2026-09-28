package com.example.gdprkv.access;

import com.example.gdprkv.models.Policy;
import java.util.Optional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Component
public class DynamoPolicyAccess implements PolicyAccess {

    private final DynamoDbTable<Policy> table;

    public DynamoPolicyAccess(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("policies", TableSchema.fromBean(Policy.class));
    }

    @Override
    public Optional<Policy> findByPurpose(String purpose) {
        return Optional.ofNullable(table.getItem(r -> r.key(Key.builder().partitionValue(purpose).build())));
    }
}
