package com.arcane.dam.repository;

// MODIFIED: migrated from AWS SDK v1 DynamoDBMapper to AWS SDK v2 DynamoDbEnhancedClient
import com.arcane.dam.entity.AssetMetaData;
import lombok.RequiredArgsConstructor;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class AssetRepository {
    private static final TableSchema<AssetMetaData> TABLE_SCHEMA = TableSchema.fromBean(AssetMetaData.class);

    private final DynamoDbEnhancedClient dynamoDbEnhancedClient;

    private DynamoDbTable<AssetMetaData> assetTable() {
        return dynamoDbEnhancedClient.table("asset", TABLE_SCHEMA);
    }

    public void save(AssetMetaData metaData) {
        if (metaData.getId() == null || metaData.getId().isBlank()) {
            metaData.setId(UUID.randomUUID().toString());
        }
        assetTable().putItem(metaData);
    }

    public Optional<AssetMetaData> findById(String fileId) {
        if (fileId == null || fileId.isBlank()) {
            return Optional.empty();
        }
        AssetMetaData assetMetaData = assetTable().getItem(Key.builder().partitionValue(fileId).build());
        return Optional.ofNullable(assetMetaData);
    }

    public boolean update(String id, AssetMetaData metaData) {
        if (id == null || id.isBlank() || metaData == null) {
            return false;
        }
        metaData.setId(id);
        Expression condition = Expression.builder()
                .expression("attribute_exists(asset_id)")
                .build();
        try {
            assetTable().putItem(PutItemEnhancedRequest.builder(AssetMetaData.class)
                    .item(metaData)
                    .conditionExpression(condition)
                    .build());
            return true;
        } catch (ConditionalCheckFailedException e) {
            return false;
        }
    }

    public boolean delete(AssetMetaData assetMetaData) {
        if (assetMetaData == null || assetMetaData.getId() == null || assetMetaData.getId().isBlank()) {
            return false;
        }
        AssetMetaData deleted = assetTable().deleteItem(Key.builder().partitionValue(assetMetaData.getId()).build());
        return deleted != null;
    }

    public boolean delete(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        AssetMetaData deleted = assetTable().deleteItem(Key.builder().partitionValue(id).build());
        return deleted != null;
    }

    public List<AssetMetaData> findBySpaceId(String spaceId) {
        Expression filter = Expression.builder()
                .expression("space_id = :spaceId")
                .expressionValues(Map.of(":spaceId", AttributeValue.fromS(spaceId)))
                .build();
        return assetTable().scan(ScanEnhancedRequest.builder().filterExpression(filter).build())
                .items()
                .stream()
                .collect(Collectors.toList());
    }
}
