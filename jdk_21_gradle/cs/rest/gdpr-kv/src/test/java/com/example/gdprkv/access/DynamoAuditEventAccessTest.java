package com.example.gdprkv.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.gdprkv.models.AuditEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamoAuditEventAccessTest {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.6");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-10-02T08:00:00Z"), ZoneOffset.UTC);

    @Container
    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.DYNAMODB);

    private DynamoDbClient dynamo;
    private DynamoDbEnhancedClient enhancedClient;
    private AuditEventAccess auditEventAccess;

    @BeforeAll
    void init() {
        AwsBasicCredentials creds = AwsBasicCredentials.create(
                LOCALSTACK.getAccessKey(), LOCALSTACK.getSecretKey());
        dynamo = DynamoDbClient.builder()
                .endpointOverride(LOCALSTACK.getEndpointOverride(LocalStackContainer.Service.DYNAMODB))
                .credentialsProvider(StaticCredentialsProvider.create(creds))
                .region(Region.of(LOCALSTACK.getRegion()))
                .build();
        enhancedClient = DynamoDbEnhancedClient.builder().dynamoDbClient(dynamo).build();
        ensureAuditEventsTable();

        auditEventAccess = new DynamoAuditEventAccess(enhancedClient);
    }

    @BeforeEach
    void cleanup() {
        enhancedClient.table("audit_events", TableSchema.fromBean(AuditEvent.class))
                .scan().items()
                .forEach(item -> enhancedClient.table("audit_events", TableSchema.fromBean(AuditEvent.class))
                        .deleteItem(item));
    }

    @Test
    @DisplayName("findAllBySubjectId returns all events for subject in chronological order")
    void findAllBySubjectId() {
        long now = CLOCK.millis();

        AuditEvent event1 = AuditEvent.builder()
                .subjectId("sub1")
                .tsUlid(now + "_EVENT1")
                .eventType(AuditEvent.EventType.CREATE_SUBJECT_REQUESTED)
                .requestId("req-1")
                .timestamp(now)
                .prevHash("0".repeat(64))
                .build();

        AuditEvent event2 = AuditEvent.builder()
                .subjectId("sub1")
                .tsUlid((now + 1000) + "_EVENT2")
                .eventType(AuditEvent.EventType.CREATE_SUBJECT_COMPLETED)
                .requestId("req-1")
                .timestamp(now + 1000)
                .prevHash(event1.getHash())
                .build();

        AuditEvent event3 = AuditEvent.builder()
                .subjectId("sub1")
                .tsUlid((now + 2000) + "_EVENT3")
                .eventType(AuditEvent.EventType.PUT_REQUESTED)
                .requestId("req-2")
                .timestamp(now + 2000)
                .prevHash(event2.getHash())
                .itemKey("pref:email")
                .purpose("preferences")
                .build();

        AuditEvent otherSubjectEvent = AuditEvent.builder()
                .subjectId("sub2")
                .tsUlid(now + "_OTHER")
                .eventType(AuditEvent.EventType.CREATE_SUBJECT_REQUESTED)
                .requestId("req-3")
                .timestamp(now)
                .prevHash("0".repeat(64))
                .build();

        auditEventAccess.put(event1);
        auditEventAccess.put(event2);
        auditEventAccess.put(event3);
        auditEventAccess.put(otherSubjectEvent);

        List<AuditEvent> events = auditEventAccess.findAllBySubjectId("sub1");

        assertEquals(3, events.size());
        assertEquals(event1.getTsUlid(), events.get(0).getTsUlid());
        assertEquals(event2.getTsUlid(), events.get(1).getTsUlid());
        assertEquals(event3.getTsUlid(), events.get(2).getTsUlid());
    }

    @Test
    @DisplayName("findAllBySubjectId returns empty list when subject has no events")
    void findAllBySubjectIdEmpty() {
        List<AuditEvent> events = auditEventAccess.findAllBySubjectId("nonexistent");
        assertTrue(events.isEmpty());
    }

    @Test
    @DisplayName("findAllBySubjectId returns events in chronological order")
    void findAllBySubjectIdOrdering() {
        long now = CLOCK.millis();

        AuditEvent event1 = createEvent(now, "EVENT1");
        AuditEvent event2 = createEvent(now + 5000, "EVENT2");
        AuditEvent event3 = createEvent(now + 10000, "EVENT3");

        // Insert in non-chronological order
        auditEventAccess.put(event2);
        auditEventAccess.put(event1);
        auditEventAccess.put(event3);

        List<AuditEvent> events = auditEventAccess.findAllBySubjectId("sub1");

        assertEquals(3, events.size());
        assertEquals(event1.getTsUlid(), events.get(0).getTsUlid());
        assertEquals(event2.getTsUlid(), events.get(1).getTsUlid());
        assertEquals(event3.getTsUlid(), events.get(2).getTsUlid());
    }

    private AuditEvent createEvent(long timestamp, String suffix) {
        return AuditEvent.builder()
                .subjectId("sub1")
                .tsUlid(timestamp + "_" + suffix)
                .eventType(AuditEvent.EventType.PUT_REQUESTED)
                .requestId("req-" + suffix)
                .timestamp(timestamp)
                .prevHash("0".repeat(64))
                .itemKey("key")
                .purpose("purpose")
                .build();
    }

    private void ensureAuditEventsTable() {
        try {
            dynamo.describeTable(b -> b.tableName("audit_events"));
        } catch (ResourceNotFoundException ex) {
            dynamo.createTable(CreateTableRequest.builder()
                    .tableName("audit_events")
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("subject_id").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("ts_ulid").attributeType(ScalarAttributeType.S).build())
                    .keySchema(
                            KeySchemaElement.builder().attributeName("subject_id").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("ts_ulid").keyType(KeyType.RANGE).build())
                    .billingMode("PAY_PER_REQUEST")
                    .build());
        }
    }
}
