package com.example.gdprkv.access;

import com.example.gdprkv.models.Subject;
import java.util.Optional;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Component
public class DynamoSubjectAccess implements SubjectAccess {

    private final DynamoDbTable<Subject> table;

    public DynamoSubjectAccess(DynamoDbEnhancedClient enhancedClient) {
        this.table = enhancedClient.table("subjects", TableSchema.fromBean(Subject.class));
    }

    @Override
    public Optional<Subject> findBySubjectId(String subjectId) {
        return Optional.ofNullable(table.getItem(r -> r.key(Key.builder()
                        .partitionValue(subjectId)
                        .build())
                .consistentRead(true)));
    }

    @Override
    public Subject save(Subject subject) {
        table.putItem(r -> r.item(subject)
                .conditionExpression(Expression.builder()
                        .expression("attribute_not_exists(subject_id)")
                        .build()));
        return subject;
    }

    @Override
    public Subject update(Subject subject) {
        table.putItem(r -> r.item(subject)
                .conditionExpression(Expression.builder()
                        .expression("attribute_exists(subject_id)")
                        .build()));
        return subject;
    }
}
