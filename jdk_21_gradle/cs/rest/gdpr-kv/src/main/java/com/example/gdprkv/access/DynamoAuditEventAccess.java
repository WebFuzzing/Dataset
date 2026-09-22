package com.example.gdprkv.access;

import com.example.gdprkv.models.AuditEvent;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Component
public class DynamoAuditEventAccess implements AuditEventAccess {

    private final DynamoDbTable<AuditEvent> table;

    public DynamoAuditEventAccess(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("audit_events", TableSchema.fromBean(AuditEvent.class));
    }

    @Override
    public void put(AuditEvent event) {
        table.putItem(event);
    }

    @Override
    public Optional<AuditEvent> findLatest(String subjectId) {
        // Query the partition in reverse chronological order so the first item is the most recent.
        return table.query(r -> r.queryConditional(QueryConditional.keyEqualTo(buildKey(subjectId)))
                        .limit(1)
                        .scanIndexForward(false))
                .items()
                .stream()
                .findFirst();
    }

    @Override
    public List<AuditEvent> findAllBySubjectId(String subjectId) {
        return table.query(r -> r.queryConditional(QueryConditional.keyEqualTo(buildKey(subjectId)))
                        .scanIndexForward(true))
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public List<AuditEvent> findEventsOlderThan(long cutoffTimestamp) {
        Expression filterExpression = Expression.builder()
                .expression("#ts < :cutoff")
                .putExpressionName("#ts", "timestamp")
                .putExpressionValue(":cutoff", AttributeValue.builder().n(String.valueOf(cutoffTimestamp)).build())
                .build();

        ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder()
                .filterExpression(filterExpression)
                .build();

        return table.scan(scanRequest)
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public void delete(AuditEvent event) {
        Key key = Key.builder()
                .partitionValue(event.getSubjectId())
                .sortValue(event.getTsUlid())
                .build();
        table.deleteItem(key);
    }

    private Key buildKey(String subjectId) {
        return Key.builder().partitionValue(subjectId).build();
    }
}
