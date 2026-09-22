package com.example.gdprkv.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.gdprkv.access.AuditEventAccess;
import com.example.gdprkv.access.DynamoAuditEventAccess;
import com.example.gdprkv.access.DynamoRecordAccess;
import com.example.gdprkv.access.DynamoSubjectAccess;
import com.example.gdprkv.access.PolicyAccess;
import com.example.gdprkv.access.RecordAccess;
import com.example.gdprkv.access.SubjectAccess;
import com.example.gdprkv.models.AuditEvent;
import com.example.gdprkv.models.Policy;
import com.example.gdprkv.models.Record;
import com.example.gdprkv.models.Subject;
import com.example.gdprkv.requests.PutSubjectHttpRequest;
import com.example.gdprkv.service.AuditLogService;
import com.example.gdprkv.service.PolicyDrivenRecordService;
import com.example.gdprkv.service.SubjectService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.http.ResponseEntity;
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
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Full-stack integration test for {@link SubjectController} using LocalStack-backed DynamoDB.
 * Tests the complete flow: HTTP request → Controller → Service → SubjectAccess → DynamoDB.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SubjectControllerDynamoIntegrationTest {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.6");
    private static final String EXISTING_SUBJECT_ID = "intTest_existing";

    @Container
    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.DYNAMODB);

    private DynamoDbClient dynamo;
    private DynamoDbEnhancedClient enhancedClient;
    private SubjectAccess subjectAccess;
    private RecordAccess recordAccess;
    private InMemoryPolicyAccess policyAccess;
    private PolicyDrivenRecordService recordService;
    private SubjectService subjectService;
    private AuditEventAccess auditAccess;
    private AuditLogService auditLogService;
    private SubjectController controller;
    private final Clock clock = Clock.fixed(Instant.parse("2024-10-02T08:00:00Z"), ZoneOffset.UTC);

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
        ensureSubjectsTable();
        ensureRecordsTable();
        ensureAuditEventsTable();

        subjectAccess = new DynamoSubjectAccess(enhancedClient);
        recordAccess = new DynamoRecordAccess(enhancedClient);
        auditAccess = new DynamoAuditEventAccess(enhancedClient);

        // Set up policy access with a test policy
        policyAccess = new InMemoryPolicyAccess();
        policyAccess.save(Policy.builder()
                .purpose("test_purpose")
                .retentionDays(30)
                .lastUpdatedAt(clock.millis())
                .build());

        recordService = new PolicyDrivenRecordService(policyAccess, recordAccess, subjectAccess, clock);
        subjectService = new SubjectService(subjectAccess, recordAccess, recordService, clock);
        auditLogService = new AuditLogService(auditAccess, clock);
        controller = new SubjectController(subjectService, auditLogService);
    }

    @BeforeEach
    void seedExistingSubject() {
        Subject existingSubject = Subject.builder()
                .subjectId(EXISTING_SUBJECT_ID)
                .createdAt(clock.millis() - 10000L)
                .residency("US")
                .requestId("intTest_seed_req")
                .build();

        try {
            subjectAccess.save(existingSubject);
        } catch (Exception ignore) {
            // Subject may already exist from previous test run in LocalStack; that's fine
        }
    }

    @Test
    @DisplayName("PUT subject with random ID creates new subject and persists to DynamoDB")
    void putSubjectHappyPath() {
        String randomSubjectId = "test_" + UUID.randomUUID().toString().substring(0, 8);
        PutSubjectHttpRequest request = new PutSubjectHttpRequest("EU");

        ResponseEntity<SubjectResponse> response = controller.putSubject(randomSubjectId, request, null);

        assertEquals(200, response.getStatusCode().value());
        SubjectResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(randomSubjectId, body.subjectId());
        assertEquals("EU", body.residency());
        assertEquals(clock.millis(), body.createdAt());

        Optional<Subject> persisted = subjectAccess.findBySubjectId(randomSubjectId);
        assertTrue(persisted.isPresent());
        assertEquals(randomSubjectId, persisted.get().getSubjectId());
        assertEquals("EU", persisted.get().getResidency());
        assertEquals(clock.millis(), persisted.get().getCreatedAt());

        // Sanity check: verify LocalStack actually persisted to DynamoDB
        GetItemResponse dbResponse = dynamo.getItem(GetItemRequest.builder()
                .tableName("subjects")
                .key(java.util.Map.of("subject_id", AttributeValue.builder().s(randomSubjectId).build()))
                .build());
        assertTrue(dbResponse.hasItem());
        assertEquals(randomSubjectId, dbResponse.item().get("subject_id").s());
    }

    @Test
    @DisplayName("PUT subject with existing ID throws GdprKvException and leaves subject unchanged")
    void putSubjectConflict() {
        PutSubjectHttpRequest request = new PutSubjectHttpRequest("EU");

        try {
            controller.putSubject(EXISTING_SUBJECT_ID, request, null);
            throw new AssertionError("Expected GdprKvException to be thrown");
        } catch (com.example.gdprkv.service.GdprKvException ex) {
            assertEquals(com.example.gdprkv.service.GdprKvException.Code.SUBJECT_ALREADY_EXISTS, ex.getCode());
            assertTrue(ex.getMessage().contains(EXISTING_SUBJECT_ID));
        }

        Optional<Subject> unchanged = subjectAccess.findBySubjectId(EXISTING_SUBJECT_ID);
        assertTrue(unchanged.isPresent());
        assertEquals("US", unchanged.get().getResidency());
        assertEquals(clock.millis() - 10000L, unchanged.get().getCreatedAt());
    }

    @Test
    @DisplayName("PUT subject with null residency creates subject successfully")
    void putSubjectNullResidency() {
        String randomSubjectId = "test_" + UUID.randomUUID().toString().substring(0, 8);
        PutSubjectHttpRequest request = new PutSubjectHttpRequest(null);

        ResponseEntity<SubjectResponse> response = controller.putSubject(randomSubjectId, request, null);

        assertEquals(200, response.getStatusCode().value());
        SubjectResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(randomSubjectId, body.subjectId());
        assertNull(body.residency());

        Optional<Subject> persisted = subjectAccess.findBySubjectId(randomSubjectId);
        assertTrue(persisted.isPresent());
        assertNull(persisted.get().getResidency());
    }

    @Test
    @DisplayName("PUT subject creates audit events for request and completion")
    void putSubjectCreatesAuditEvents() {
        String randomSubjectId = "test_" + UUID.randomUUID().toString().substring(0, 8);
        PutSubjectHttpRequest request = new PutSubjectHttpRequest("US");

        ResponseEntity<SubjectResponse> response = controller.putSubject(randomSubjectId, request, null);

        assertEquals(200, response.getStatusCode().value());

        List<AuditEvent> events = enhancedClient.table("audit_events", TableSchema.fromBean(AuditEvent.class))
                .scan().items().stream()
                .filter(e -> e.getSubjectId().equals(randomSubjectId))
                .toList();

        assertEquals(2, events.size());

        AuditEvent requestedEvent = events.stream()
                .filter(e -> e.getEventType() == AuditEvent.EventType.CREATE_SUBJECT_REQUESTED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("CREATE_SUBJECT_REQUESTED event not found"));

        AuditEvent completedEvent = events.stream()
                .filter(e -> e.getEventType() == AuditEvent.EventType.CREATE_SUBJECT_COMPLETED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("CREATE_SUBJECT_COMPLETED event not found"));

        // Verify REQUESTED event
        assertEquals(AuditEvent.EventType.CREATE_SUBJECT_REQUESTED, requestedEvent.getEventType());
        assertEquals(randomSubjectId, requestedEvent.getSubjectId());
        assertNotNull(requestedEvent.getRequestId());
        assertEquals(clock.millis(), requestedEvent.getTimestamp());
        assertNotNull(requestedEvent.getTsUlid());
        assertTrue(requestedEvent.getTsUlid().startsWith(String.valueOf(clock.millis())));
        assertNotNull(requestedEvent.getHash());
        assertEquals("0".repeat(64), requestedEvent.getPrevHash());
        assertNull(requestedEvent.getItemKey());
        assertNull(requestedEvent.getPurpose());
        assertNull(requestedEvent.getDetails());

        // Verify COMPLETED event
        assertEquals(AuditEvent.EventType.CREATE_SUBJECT_COMPLETED, completedEvent.getEventType());
        assertEquals(randomSubjectId, completedEvent.getSubjectId());
        assertEquals(requestedEvent.getRequestId(), completedEvent.getRequestId());
        assertEquals(clock.millis(), completedEvent.getTimestamp());
        assertNotNull(completedEvent.getTsUlid());
        assertTrue(completedEvent.getTsUlid().startsWith(String.valueOf(clock.millis())));
        assertNotNull(completedEvent.getHash());
        assertEquals(requestedEvent.getHash(), completedEvent.getPrevHash());
        assertNull(completedEvent.getItemKey());
        assertNull(completedEvent.getPurpose());
        assertNull(completedEvent.getDetails());
    }

    @Test
    @DisplayName("DELETE subject with no records marks subject for erasure")
    void deleteSubjectNoRecords() {
        String subjectId = "test_delete_" + UUID.randomUUID().toString().substring(0, 8);

        // Create a subject first
        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .createdAt(clock.millis())
                .residency("US")
                .requestId("create_req")
                .build();
        subjectAccess.save(subject);

        // Delete the subject
        ResponseEntity<SubjectDeletionResponse> response = controller.deleteSubject(subjectId, "delete_req");

        assertEquals(200, response.getStatusCode().value());
        SubjectDeletionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(subjectId, body.subject().subjectId());
        assertTrue(body.subject().erasureInProgress());
        assertNotNull(body.subject().erasureRequestedAt());
        assertEquals(clock.millis(), body.subject().erasureRequestedAt());
        assertEquals(0, body.recordsDeleted());
        assertEquals(0, body.totalRecords());

        // Verify subject is marked for erasure in database
        Optional<Subject> persisted = subjectAccess.findBySubjectId(subjectId);
        assertTrue(persisted.isPresent());
        assertTrue(persisted.get().getErasureInProgress());
        assertEquals(clock.millis(), persisted.get().getErasureRequestedAt());
    }

    @Test
    @DisplayName("DELETE subject with non-existent ID throws GdprKvException")
    void deleteSubjectNotFound() {
        String nonExistentId = "nonexistent_" + UUID.randomUUID().toString().substring(0, 8);

        try {
            controller.deleteSubject(nonExistentId, "delete_req");
            throw new AssertionError("Expected GdprKvException to be thrown");
        } catch (com.example.gdprkv.service.GdprKvException ex) {
            assertEquals(com.example.gdprkv.service.GdprKvException.Code.SUBJECT_NOT_FOUND, ex.getCode());
            assertTrue(ex.getMessage().contains(nonExistentId));
        }
    }

    @Test
    @DisplayName("DELETE subject with records tombstones all records")
    void deleteSubjectWithRecords() {
        String subjectId = "test_delete_records_" + UUID.randomUUID().toString().substring(0, 8);

        // Create subject
        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .createdAt(clock.millis())
                .residency("US")
                .requestId("create_req")
                .build();
        subjectAccess.save(subject);

        // Create some records for this subject
        Record record1 = Record.builder()
                .subjectId(subjectId)
                .recordKey("key1")
                .valueObject("data1")
                .purpose("test_purpose")
                .version(1L)
                .createdAt(clock.millis())
                .updatedAt(clock.millis())
                .retentionDays(30)
                .requestId("rec1_req")
                .build();
        Record record2 = Record.builder()
                .subjectId(subjectId)
                .recordKey("key2")
                .valueObject("data2")
                .purpose("test_purpose")
                .version(1L)
                .createdAt(clock.millis())
                .updatedAt(clock.millis())
                .retentionDays(30)
                .requestId("rec2_req")
                .build();
        recordAccess.save(record1);
        recordAccess.save(record2);

        // Delete the subject
        ResponseEntity<SubjectDeletionResponse> response = controller.deleteSubject(subjectId, "delete_req");

        assertEquals(200, response.getStatusCode().value());
        SubjectDeletionResponse body = response.getBody();
        assertNotNull(body);
        assertEquals(2, body.recordsDeleted());
        assertEquals(2, body.totalRecords());
        assertTrue(body.subject().erasureInProgress());

        // Verify all records are tombstoned
        List<Record> records = recordAccess.findAllBySubjectId(subjectId);
        assertEquals(2, records.size());
        for (Record record : records) {
            assertTrue(record.getTombstoned());
            assertNotNull(record.getTombstonedAt());
        }
    }

    @Test
    @DisplayName("DELETE subject creates audit events for erasure workflow")
    void deleteSubjectCreatesAuditEvents() {
        String subjectId = "test_delete_audit_" + UUID.randomUUID().toString().substring(0, 8);

        // Create subject
        Subject subject = Subject.builder()
                .subjectId(subjectId)
                .createdAt(clock.millis())
                .residency("US")
                .requestId("create_req")
                .build();
        subjectAccess.save(subject);

        // Delete the subject
        ResponseEntity<SubjectDeletionResponse> response = controller.deleteSubject(subjectId, "delete_req");

        assertEquals(200, response.getStatusCode().value());

        // Verify audit events
        List<AuditEvent> events = enhancedClient.table("audit_events", TableSchema.fromBean(AuditEvent.class))
                .scan().items().stream()
                .filter(e -> e.getSubjectId().equals(subjectId) && e.getRequestId().equals("delete_req"))
                .toList();

        // Should have: REQUESTED, STARTED, COMPLETED
        assertEquals(3, events.size());

        AuditEvent requestedEvent = events.stream()
                .filter(e -> e.getEventType() == AuditEvent.EventType.SUBJECT_ERASURE_REQUESTED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("SUBJECT_ERASURE_REQUESTED event not found"));

        AuditEvent startedEvent = events.stream()
                .filter(e -> e.getEventType() == AuditEvent.EventType.SUBJECT_ERASURE_STARTED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("SUBJECT_ERASURE_STARTED event not found"));

        AuditEvent completedEvent = events.stream()
                .filter(e -> e.getEventType() == AuditEvent.EventType.SUBJECT_ERASURE_COMPLETED)
                .findFirst()
                .orElseThrow(() -> new AssertionError("SUBJECT_ERASURE_COMPLETED event not found"));

        assertEquals("delete_req", requestedEvent.getRequestId());
        assertEquals("delete_req", startedEvent.getRequestId());
        assertEquals("delete_req", completedEvent.getRequestId());
    }

    private void ensureSubjectsTable() {
        try {
            dynamo.describeTable(b -> b.tableName("subjects"));
        } catch (ResourceNotFoundException ex) {
            dynamo.createTable(CreateTableRequest.builder()
                    .tableName("subjects")
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("subject_id")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("subject_id")
                            .keyType(KeyType.HASH)
                            .build())
                    .billingMode("PAY_PER_REQUEST")
                    .build());
        }
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
                            .projection(b -> b.projectionType("KEYS_ONLY"))
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

    /**
     * In-memory implementation of PolicyAccess for testing.
     */
    private static class InMemoryPolicyAccess implements PolicyAccess {
        private final Map<String, Policy> store = new HashMap<>();

        void save(Policy policy) {
            store.put(policy.getPurpose(), policy);
        }

        @Override
        public Optional<Policy> findByPurpose(String purpose) {
            return Optional.ofNullable(store.get(purpose));
        }
    }
}
