package com.example.gdprkv.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.gdprkv.access.AuditEventAccess;
import com.example.gdprkv.access.DynamoAuditEventAccess;
import com.example.gdprkv.access.DynamoRecordAccess;
import com.example.gdprkv.access.RecordAccess;
import com.example.gdprkv.config.PurgeSweeperProperties;
import com.example.gdprkv.models.AuditEvent;
import com.example.gdprkv.models.Record;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
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
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Integration test for PurgeSweeper using LocalStack-backed DynamoDB.
 * Tests the complete flow: Sweeper → RecordAccess → DynamoDB GSI → Physical deletion.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PurgeSweeperIntegrationTest {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.6");
    private static final DateTimeFormatter PURGE_BUCKET_FORMATTER =
            DateTimeFormatter.ofPattern("'h#'yyyyMMdd'T'HH").withZone(ZoneOffset.UTC);

    @Container
    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.DYNAMODB);

    private DynamoDbClient dynamo;
    private DynamoDbEnhancedClient enhancedClient;
    private RecordAccess recordAccess;
    private AuditEventAccess auditEventAccess;
    private AuditLogService auditLogService;
    private PurgeSweeper purgeSweeper;
    private final Clock clock = Clock.fixed(Instant.parse("2025-08-27T21:30:00Z"), ZoneOffset.UTC);

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
        ensureRecordsTable();
        ensureAuditEventsTable();

        recordAccess = new DynamoRecordAccess(enhancedClient);
        auditEventAccess = new DynamoAuditEventAccess(enhancedClient);
        auditLogService = new AuditLogService(auditEventAccess, clock);

        PurgeSweeperProperties properties = new PurgeSweeperProperties();
        properties.setEnabled(true);
        properties.setLookbackHours(2);

        purgeSweeper = new PurgeSweeper(clock, properties, recordAccess, auditLogService);
    }

    @BeforeEach
    void cleanup() {
        // Clean up records table
        enhancedClient.table("records", TableSchema.fromBean(Record.class))
                .scan().items()
                .forEach(item -> enhancedClient.table("records", TableSchema.fromBean(Record.class))
                        .deleteItem(item));

        // Clean up audit events table
        enhancedClient.table("audit_events", TableSchema.fromBean(AuditEvent.class))
                .scan().items()
                .forEach(item -> enhancedClient.table("audit_events", TableSchema.fromBean(AuditEvent.class))
                        .deleteItem(item));
    }

    @Test
    @DisplayName("PurgeSweeper physically deletes expired tombstoned records from DynamoDB")
    void purgeSweeperDeletesExpiredRecords() {
        long now = clock.millis();
        long pastDue = now - 10000; // 10 seconds ago
        String purgeBucket = PURGE_BUCKET_FORMATTER.format(Instant.ofEpochMilli(now));

        // Create a tombstoned record that's past its purge_due_at
        Record expiredRecord = Record.builder()
                .subjectId("subject_purge_test")
                .recordKey("expired_key")
                .purpose("test_purpose")
                .version(1L)
                .createdAt(now - 100000)
                .updatedAt(now - 50000)
                .retentionDays(30)
                .requestId("req-1")
                .tombstoned(true)
                .tombstonedAt(now - 50000)
                .purgeDueAt(pastDue)
                .purgeBucket(purgeBucket)
                .build();

        recordAccess.save(expiredRecord);

        // Verify record exists before purge
        Optional<Record> beforePurge = recordAccess.findBySubjectIdAndRecordKey("subject_purge_test", "expired_key");
        assertTrue(beforePurge.isPresent(), "Record should exist before purge");

        // Run the purge sweeper
        purgeSweeper.purgeExpiredRecords();

        // Verify record was physically deleted
        Optional<Record> afterPurge = recordAccess.findBySubjectIdAndRecordKey("subject_purge_test", "expired_key");
        assertFalse(afterPurge.isPresent(), "Record should be physically deleted after purge");
    }

    @Test
    @DisplayName("PurgeSweeper creates audit events for purged records")
    void purgeSweeperCreatesAuditEvents() {
        long now = clock.millis();
        long pastDue = now - 10000;
        String purgeBucket = PURGE_BUCKET_FORMATTER.format(Instant.ofEpochMilli(now));

        // Create expired tombstoned record
        Record expiredRecord = Record.builder()
                .subjectId("subject_audit_test")
                .recordKey("audit_key")
                .purpose("test_purpose")
                .version(1L)
                .createdAt(now - 100000)
                .updatedAt(now - 50000)
                .retentionDays(30)
                .requestId("req-1")
                .tombstoned(true)
                .tombstonedAt(now - 50000)
                .purgeDueAt(pastDue)
                .purgeBucket(purgeBucket)
                .build();

        recordAccess.save(expiredRecord);

        // Run the purge sweeper
        purgeSweeper.purgeExpiredRecords();

        // Verify audit events were created
        List<AuditEvent> events = auditEventAccess.findAllBySubjectId("subject_audit_test");

        // Should have PURGE_CANDIDATE_IDENTIFIED and PURGE_CANDIDATE_SUCCESSFUL
        assertEquals(2, events.size(), "Should have 2 audit events");

        AuditEvent identifiedEvent = events.stream()
                .filter(e -> e.getEventType() == AuditEvent.EventType.PURGE_CANDIDATE_IDENTIFIED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("PURGE_CANDIDATE_IDENTIFIED event not found"));

        AuditEvent successfulEvent = events.stream()
                .filter(e -> e.getEventType() == AuditEvent.EventType.PURGE_CANDIDATE_SUCCESSFUL)
                .findFirst()
                .orElseThrow(() -> new AssertionError("PURGE_CANDIDATE_SUCCESSFUL event not found"));

        assertEquals("subject_audit_test", identifiedEvent.getSubjectId());
        assertEquals("audit_key", identifiedEvent.getItemKey());
        assertEquals("subject_audit_test", successfulEvent.getSubjectId());
        assertEquals("audit_key", successfulEvent.getItemKey());
    }

    @Test
    @DisplayName("PurgeSweeper does not delete records that are not yet due for purge")
    void purgeSweeperSkipsRecordsNotYetDue() {
        long now = clock.millis();
        long futureDate = now + 100000; // 100 seconds in the future
        String purgeBucket = PURGE_BUCKET_FORMATTER.format(Instant.ofEpochMilli(now));

        // Create a tombstoned record that's NOT yet due for purging
        Record notYetDueRecord = Record.builder()
                .subjectId("subject_not_due")
                .recordKey("future_key")
                .purpose("test_purpose")
                .version(1L)
                .createdAt(now - 100000)
                .updatedAt(now - 50000)
                .retentionDays(30)
                .requestId("req-1")
                .tombstoned(true)
                .tombstonedAt(now - 50000)
                .purgeDueAt(futureDate)
                .purgeBucket(purgeBucket)
                .build();

        recordAccess.save(notYetDueRecord);

        // Run the purge sweeper
        purgeSweeper.purgeExpiredRecords();

        // Verify record was NOT deleted (still exists)
        Optional<Record> afterPurge = recordAccess.findBySubjectIdAndRecordKey("subject_not_due", "future_key");
        assertTrue(afterPurge.isPresent(), "Record should still exist (not yet due for purge)");
    }

    @Test
    @DisplayName("PurgeSweeper processes multiple expired records in one run")
    void purgeSweeperProcessesMultipleRecords() {
        long now = clock.millis();
        long pastDue = now - 10000;
        String purgeBucket = PURGE_BUCKET_FORMATTER.format(Instant.ofEpochMilli(now));

        // Create multiple expired tombstoned records
        for (int i = 1; i <= 3; i++) {
            Record record = Record.builder()
                    .subjectId("subject_multi_" + i)
                    .recordKey("key_" + i)
                    .purpose("test_purpose")
                    .version(1L)
                    .createdAt(now - 100000)
                    .updatedAt(now - 50000)
                    .retentionDays(30)
                    .requestId("req-" + i)
                    .tombstoned(true)
                    .tombstonedAt(now - 50000)
                    .purgeDueAt(pastDue)
                    .purgeBucket(purgeBucket)
                    .build();

            recordAccess.save(record);
        }

        // Run the purge sweeper
        purgeSweeper.purgeExpiredRecords();

        // Verify all records were deleted
        for (int i = 1; i <= 3; i++) {
            Optional<Record> afterPurge = recordAccess.findBySubjectIdAndRecordKey("subject_multi_" + i, "key_" + i);
            assertFalse(afterPurge.isPresent(), "Record " + i + " should be deleted");
        }
    }

    @Test
    @DisplayName("PurgeSweeper queries records using GSI and handles empty results")
    void purgeSweeperHandlesEmptyResults() {
        // Run sweeper with no records in the database
        purgeSweeper.purgeExpiredRecords();

        // Should complete without errors (verified by test not throwing)
        // No records to verify, just ensuring the sweeper doesn't crash
    }

    @Test
    @DisplayName("PurgeSweeper does not delete non-tombstoned records")
    void purgeSweeperSkipsNonTombstonedRecords() {
        long now = clock.millis();
        long pastDue = now - 10000;
        String purgeBucket = PURGE_BUCKET_FORMATTER.format(Instant.ofEpochMilli(now));

        // Create a NON-tombstoned record (even though it has purge metadata)
        Record nonTombstonedRecord = Record.builder()
                .subjectId("subject_not_tombstoned")
                .recordKey("active_key")
                .purpose("test_purpose")
                .version(1L)
                .createdAt(now - 100000)
                .updatedAt(now - 50000)
                .retentionDays(30)
                .requestId("req-1")
                .tombstoned(false)  // NOT tombstoned
                .purgeDueAt(pastDue)
                .purgeBucket(purgeBucket)
                .build();

        recordAccess.save(nonTombstonedRecord);

        // Run the purge sweeper
        purgeSweeper.purgeExpiredRecords();

        // Verify record was NOT deleted (safety check)
        Optional<Record> afterPurge = recordAccess.findBySubjectIdAndRecordKey("subject_not_tombstoned", "active_key");
        assertTrue(afterPurge.isPresent(), "Non-tombstoned record should NOT be deleted");
    }

    private void ensureRecordsTable() {
        try {
            dynamo.describeTable(b -> b.tableName("records"));
        } catch (ResourceNotFoundException ex) {
            dynamo.createTable(CreateTableRequest.builder()
                    .tableName("records")
                    .attributeDefinitions(
                            AttributeDefinition.builder().attributeName("subject_id").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("record_key").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("purge_bucket").attributeType(ScalarAttributeType.S).build(),
                            AttributeDefinition.builder().attributeName("purge_due_at").attributeType(ScalarAttributeType.N).build())
                    .keySchema(
                            KeySchemaElement.builder().attributeName("subject_id").keyType(KeyType.HASH).build(),
                            KeySchemaElement.builder().attributeName("record_key").keyType(KeyType.RANGE).build())
                    .globalSecondaryIndexes(GlobalSecondaryIndex.builder()
                            .indexName("records_by_purge_due")
                            .keySchema(
                                    KeySchemaElement.builder().attributeName("purge_bucket").keyType(KeyType.HASH).build(),
                                    KeySchemaElement.builder().attributeName("purge_due_at").keyType(KeyType.RANGE).build())
                            .projection(b -> b.projectionType("ALL"))  // Changed to ALL for testing
                            .provisionedThroughput(ProvisionedThroughput.builder().readCapacityUnits(1L).writeCapacityUnits(1L).build())
                            .build())
                    .billingMode("PAY_PER_REQUEST")
                    .build());
        }
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
