package com.example.gdprkv.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.gdprkv.access.AuditEventAccess;
import com.example.gdprkv.access.DynamoAuditEventAccess;
import com.example.gdprkv.access.DynamoPolicyAccess;
import com.example.gdprkv.access.DynamoRecordAccess;
import com.example.gdprkv.access.DynamoSubjectAccess;
import com.example.gdprkv.access.PolicyAccess;
import com.example.gdprkv.access.RecordAccess;
import com.example.gdprkv.access.SubjectAccess;
import com.example.gdprkv.models.AuditEvent;
import com.example.gdprkv.models.Policy;
import com.example.gdprkv.models.Record;
import com.example.gdprkv.models.Subject;
import com.example.gdprkv.requests.PutRecordHttpRequest;
import com.example.gdprkv.service.AuditLogService;
import com.example.gdprkv.service.PolicyDrivenRecordService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
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
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.CreateTableRequest;
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

/**
 * Full-stack integration test for {@link AuditEventController}. Uses Testcontainers with LocalStack
 * to verify the GET /subjects/{subjectId}/audit-events endpoint returns audit events in the expected
 * format and order.
 */
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AuditEventControllerDynamoIntegrationTest {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.6");

    @Container
    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.DYNAMODB);

    private DynamoDbClient dynamo;
    private DynamoDbEnhancedClient enhancedClient;
    private PolicyAccess policyAccess;
    private RecordAccess recordAccess;
    private AuditEventAccess auditAccess;
    private SubjectAccess subjectAccess;
    private AuditLogService auditLogService;
    private PolicyDrivenRecordService recordService;
    private RecordController recordController;
    private AuditEventController auditEventController;
    private final ObjectMapper mapper = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
    private final Clock clock = Clock.fixed(Instant.parse("2024-10-02T08:00:00Z"), ZoneOffset.UTC);
    private Policy seededPolicy;

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
        ensureTables();

        policyAccess = new DynamoPolicyAccess(enhancedClient);
        recordAccess = new DynamoRecordAccess(enhancedClient);
        auditAccess = new DynamoAuditEventAccess(enhancedClient);
        subjectAccess = new DynamoSubjectAccess(enhancedClient);
        auditLogService = new AuditLogService(auditAccess, clock);
        recordService = new PolicyDrivenRecordService(policyAccess, recordAccess, subjectAccess, clock);
        recordController = new RecordController(recordService, auditLogService);
        auditEventController = new AuditEventController(auditAccess);
    }

    @BeforeEach
    void truncateTables() throws Exception {
        enhancedClient.table("records", TableSchema.fromBean(Record.class))
                .scan().items()
                .forEach(item -> enhancedClient.table("records", TableSchema.fromBean(Record.class)).deleteItem(item));

        enhancedClient.table("audit_events", TableSchema.fromBean(AuditEvent.class))
                .scan().items()
                .forEach(item -> enhancedClient.table("audit_events", TableSchema.fromBean(AuditEvent.class)).deleteItem(item));

        PutRecordFixture fixture = mapper.convertValue(readFixture("fixtures/put_record_request.json"), PutRecordFixture.class);
        Subject subject = Subject.builder()
                .subjectId(fixture.subjectId())
                .createdAt(clock.millis())
                .residency("US")
                .requestId("audit-test-req")
                .build();

        try {
            subjectAccess.save(subject);
        } catch (ConditionalCheckFailedException ignore) {
            // Subject already exists; reuse the existing record for consistency checks.
        }

        seededPolicy = mapper.convertValue(readFixture("fixtures/policy.json"), Policy.class);
        enhancedClient.table("policies", TableSchema.fromBean(Policy.class)).putItem(seededPolicy);
    }

    @Test
    @DisplayName("GET /subjects/{subjectId}/audit-events returns all audit events via API")
    void getAllAuditEventsViaApi() throws Exception {
        PutRecordFixture fixture = mapper.convertValue(readFixture("fixtures/put_record_request.json"), PutRecordFixture.class);
        String subjectId = fixture.subjectId();

        // Create three record operations to generate audit events
        PutRecordHttpRequest request1 = new PutRecordHttpRequest(fixture.purpose(), fixture.value());
        recordController.putRecord(subjectId, fixture.recordKey(), request1);

        JsonNode value2 = mapper.createObjectNode().put("email", "updated@example.com");
        PutRecordHttpRequest request2 = new PutRecordHttpRequest(fixture.purpose(), value2);
        recordController.putRecord(subjectId, fixture.recordKey(), request2);

        JsonNode value3 = mapper.createObjectNode().put("theme", "dark");
        PutRecordHttpRequest request3 = new PutRecordHttpRequest(fixture.purpose(), value3);
        recordController.putRecord(subjectId, "pref:theme", request3);

        // Retrieve audit events via API
        ResponseEntity<List<AuditEventResponse>> response = auditEventController.getAllAuditEvents(subjectId);

        assertEquals(200, response.getStatusCode().value());
        List<AuditEventResponse> events = response.getBody();
        assertNotNull(events);

        // Should have 6 events total:
        // - PUT_REQUESTED + PUT_NEW_ITEM_SUCCESS for first record
        // - PUT_REQUESTED + PUT_UPDATE_ITEM_SUCCESS for second record (update)
        // - PUT_REQUESTED + PUT_NEW_ITEM_SUCCESS for third record
        assertEquals(6, events.size());

        // Verify first event (genesis event with prev_hash = all zeros)
        AuditEventResponse firstEvent = events.stream()
                .filter(e -> e.prevHash().equals("0".repeat(64)))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No genesis event found"));

        assertEquals(subjectId, firstEvent.subjectId());
        assertNotNull(firstEvent.requestId());
        assertNotNull(firstEvent.hash());

        // Verify hash chain integrity - each event's prevHash should link to another event's hash
        for (AuditEventResponse event : events) {
            if (!event.prevHash().equals("0".repeat(64))) {
                // Find the previous event whose hash matches this event's prevHash
                boolean foundPrev = events.stream()
                        .anyMatch(e -> e.hash().equals(event.prevHash()));
                assertTrue(foundPrev, "Event " + event.tsUlid() + " has prevHash that doesn't match any event's hash");
            }
        }

        // Verify different event types are present
        long requestedCount = events.stream()
                .filter(e -> e.eventType() == AuditEvent.EventType.PUT_REQUESTED)
                .count();
        long newItemCount = events.stream()
                .filter(e -> e.eventType() == AuditEvent.EventType.PUT_NEW_ITEM_SUCCESS)
                .count();
        long updateItemCount = events.stream()
                .filter(e -> e.eventType() == AuditEvent.EventType.PUT_UPDATE_ITEM_SUCCESS)
                .count();

        assertEquals(3, requestedCount);
        assertEquals(2, newItemCount);
        assertEquals(1, updateItemCount);
    }

    private void ensureTables() {
        createSubjectTable();
        createPoliciesTable();
        createRecordsTable();
        createAuditEventsTable();
    }

    private void createSubjectTable() {
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

    private void createPoliciesTable() {
        try {
            dynamo.describeTable(b -> b.tableName("policies"));
        } catch (ResourceNotFoundException ex) {
            dynamo.createTable(CreateTableRequest.builder()
                    .tableName("policies")
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("purpose")
                            .attributeType(ScalarAttributeType.S)
                            .build())
                    .keySchema(KeySchemaElement.builder()
                            .attributeName("purpose")
                            .keyType(KeyType.HASH)
                            .build())
                    .billingMode("PAY_PER_REQUEST")
                    .build());
        }
    }

    private void createRecordsTable() {
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

    private void createAuditEventsTable() {
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

    private Map<String, Object> readFixture(String resourcePath) throws Exception {
        Path path = Path.of("src/test/resources", resourcePath);
        String json = Files.readString(path);
        return mapper.readValue(json, new TypeReference<>() {
        });
    }

    private record PutRecordFixture(
            @JsonProperty("subject_id") String subjectId,
            @JsonProperty("record_key") String recordKey,
            @JsonProperty("purpose") String purpose,
            @JsonProperty("value") JsonNode value
    ) {
    }
}
