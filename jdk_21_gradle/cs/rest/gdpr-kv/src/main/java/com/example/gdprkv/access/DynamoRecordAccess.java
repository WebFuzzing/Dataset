package com.example.gdprkv.access;

import com.example.gdprkv.models.Record;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Component
public class DynamoRecordAccess implements RecordAccess {

    private final DynamoDbTable<Record> table;

    public DynamoRecordAccess(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("records", TableSchema.fromBean(Record.class));
    }

    @Override
    public Optional<Record> findBySubjectIdAndRecordKey(String subjectId, String recordKey) {
        return Optional.ofNullable(table.getItem(r -> r.key(Key.builder()
                .partitionValue(subjectId)
                .sortValue(recordKey)
                .build())));
    }

    @Override
    public List<Record> findAllBySubjectId(String subjectId) {
        return table.query(r -> r.queryConditional(QueryConditional.keyEqualTo(
                        Key.builder().partitionValue(subjectId).build()))
                        .scanIndexForward(true))
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    @Override
    public List<Record> findRecordsDueForPurge(String purgeBucket, long cutoffTimestamp) {
        return table.index("records_by_purge_due")
                .query(r -> r.queryConditional(
                        QueryConditional.sortLessThanOrEqualTo(
                                Key.builder()
                                        .partitionValue(purgeBucket)
                                        .sortValue(cutoffTimestamp)
                                        .build())))
                .stream()
                .flatMap(page -> page.items().stream())
                .collect(Collectors.toList());
    }

    @Override
    public Record save(Record record) {
        table.putItem(record);
        return record;
    }

    @Override
    public void delete(Record record) {
        table.deleteItem(Key.builder()
                .partitionValue(record.getSubjectId())
                .sortValue(record.getRecordKey())
                .build());
    }
}
