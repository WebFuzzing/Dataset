package com.example.gdprkv.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.example.gdprkv.access.AuditEventAccess;
import com.example.gdprkv.access.DynamoAuditEventAccess;
import com.example.gdprkv.config.AuditRetentionProperties;
import com.example.gdprkv.models.AuditEvent;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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
class AuditLogRetentionJobTest {

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
    @DisplayName("Job can be constructed directly with dependencies")
    void directConstruction() {
        AuditRetentionProperties props = new AuditRetentionProperties();
        props.setRetentionDays(730);
        AuditLogRetentionJob directJob = new AuditLogRetentionJob(CLOCK, props, auditEventAccess);

        assertDoesNotThrow(directJob::enforceRetentionPolicy,
                "Job should execute without throwing exceptions");
    }

    @Test
    @DisplayName("Job deletes events older than retention period")
    void deletesOldEvents() {
        AuditRetentionProperties props = new AuditRetentionProperties();
        props.setRetentionDays(30);

        long now = CLOCK.millis();
        long oldTimestamp = now - (60L * 86400000L); // 60 days old
        long recentTimestamp = now - (10L * 86400000L); // 10 days old

        AuditEvent oldEvent = AuditEvent.builder()
                .subjectId("sub1")
                .tsUlid(oldTimestamp + "_OLD")
                .eventType(AuditEvent.EventType.PUT_REQUESTED)
                .requestId("req-old")
                .timestamp(oldTimestamp)
                .prevHash("0".repeat(64))
                .itemKey("key")
                .purpose("purpose")
                .build();

        AuditEvent recentEvent = AuditEvent.builder()
                .subjectId("sub1")
                .tsUlid(recentTimestamp + "_RECENT")
                .eventType(AuditEvent.EventType.PUT_REQUESTED)
                .requestId("req-recent")
                .timestamp(recentTimestamp)
                .prevHash("0".repeat(64))
                .itemKey("key")
                .purpose("purpose")
                .build();

        auditEventAccess.put(oldEvent);
        auditEventAccess.put(recentEvent);

        AuditLogRetentionJob directJob = new AuditLogRetentionJob(CLOCK, props, auditEventAccess);
        directJob.enforceRetentionPolicy();

        assertEquals(1, auditEventAccess.findAllBySubjectId("sub1").size(), "Should have 1 event remaining");
        assertEquals(recentEvent.getTsUlid(), auditEventAccess.findAllBySubjectId("sub1").getFirst().getTsUlid(),
                "Recent event should be preserved");
    }

    @Test
    @DisplayName("Job preserves all events when none are older than retention period")
    void preservesRecentEvents() {
        AuditRetentionProperties props = new AuditRetentionProperties();
        props.setRetentionDays(30);

        long now = CLOCK.millis();
        long recentTimestamp1 = now - (10L * 86400000L);
        long recentTimestamp2 = now - (20L * 86400000L);

        AuditEvent event1 = AuditEvent.builder()
                .subjectId("sub1")
                .tsUlid(recentTimestamp1 + "_1")
                .eventType(AuditEvent.EventType.PUT_REQUESTED)
                .requestId("req-1")
                .timestamp(recentTimestamp1)
                .prevHash("0".repeat(64))
                .itemKey("key")
                .purpose("purpose")
                .build();

        AuditEvent event2 = AuditEvent.builder()
                .subjectId("sub1")
                .tsUlid(recentTimestamp2 + "_2")
                .eventType(AuditEvent.EventType.PUT_REQUESTED)
                .requestId("req-2")
                .timestamp(recentTimestamp2)
                .prevHash("0".repeat(64))
                .itemKey("key")
                .purpose("purpose")
                .build();

        auditEventAccess.put(event1);
        auditEventAccess.put(event2);

        AuditLogRetentionJob directJob = new AuditLogRetentionJob(CLOCK, props, auditEventAccess);
        directJob.enforceRetentionPolicy();

        assertEquals(2, auditEventAccess.findAllBySubjectId("sub1").size(), "Should preserve both recent events");
    }

    @Test
    @DisplayName("Job handles empty audit log gracefully")
    void handlesEmptyLog() {
        AuditRetentionProperties props = new AuditRetentionProperties();
        props.setRetentionDays(30);

        AuditLogRetentionJob directJob = new AuditLogRetentionJob(CLOCK, props, auditEventAccess);

        assertDoesNotThrow(directJob::enforceRetentionPolicy,
                "Job should handle empty audit log without errors");
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
