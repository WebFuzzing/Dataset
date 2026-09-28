package com.example.gdprkv.access;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.example.gdprkv.models.Record;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import software.amazon.awssdk.services.dynamodb.model.GlobalSecondaryIndex;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ProvisionedThroughput;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;

@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamoRecordAccessTest {

    private static final DockerImageName LOCALSTACK_IMAGE = DockerImageName.parse("localstack/localstack:3.6");
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2024-10-02T08:00:00Z"), ZoneOffset.UTC);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Container
    private static final LocalStackContainer LOCALSTACK = new LocalStackContainer(LOCALSTACK_IMAGE)
            .withServices(LocalStackContainer.Service.DYNAMODB);

    private DynamoDbClient dynamo;
    private DynamoDbEnhancedClient enhancedClient;
    private RecordAccess recordAccess;

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

        recordAccess = new DynamoRecordAccess(enhancedClient);
    }

    @BeforeEach
    void cleanup() {
        enhancedClient.table("records", TableSchema.fromBean(Record.class))
                .scan().items()
                .forEach(item -> enhancedClient.table("records", TableSchema.fromBean(Record.class))
                        .deleteItem(item));
    }

    @Test
    @DisplayName("findAllBySubjectId returns all records for subject ordered by record key")
    void findAllBySubjectId() {
        long now = CLOCK.millis();

        Record record1 = createRecord("sub1", "pref:email", now);
        Record record2 = createRecord("sub1", "pref:language", now + 1000);
        Record record3 = createRecord("sub1", "pref:theme", now + 2000);
        Record otherSubjectRecord = createRecord("sub2", "pref:email", now);

        recordAccess.save(record1);
        recordAccess.save(record2);
        recordAccess.save(record3);
        recordAccess.save(otherSubjectRecord);

        List<Record> records = recordAccess.findAllBySubjectId("sub1");

        assertEquals(3, records.size());
        assertEquals("pref:email", records.get(0).getRecordKey());
        assertEquals("pref:language", records.get(1).getRecordKey());
        assertEquals("pref:theme", records.get(2).getRecordKey());
    }

    @Test
    @DisplayName("findAllBySubjectId returns empty list when subject has no records")
    void findAllBySubjectIdEmpty() {
        List<Record> records = recordAccess.findAllBySubjectId("nonexistent");
        assertTrue(records.isEmpty());
    }

    @Test
    @DisplayName("findAllBySubjectId returns records in record key order")
    void findAllBySubjectIdOrdering() {
        long now = CLOCK.millis();

        Record recordZ = createRecord("sub1", "z:last", now);
        Record recordA = createRecord("sub1", "a:first", now + 1000);
        Record recordM = createRecord("sub1", "m:middle", now + 2000);

        // Insert in non-alphabetical order
        recordAccess.save(recordZ);
        recordAccess.save(recordA);
        recordAccess.save(recordM);

        List<Record> records = recordAccess.findAllBySubjectId("sub1");

        assertEquals(3, records.size());
        assertEquals("a:first", records.get(0).getRecordKey());
        assertEquals("m:middle", records.get(1).getRecordKey());
        assertEquals("z:last", records.get(2).getRecordKey());
    }

    private Record createRecord(String subjectId, String recordKey, long timestamp) {
        ObjectNode value = MAPPER.createObjectNode();
        value.put("data", "test-value-" + recordKey);

        return Record.builder()
                .subjectId(subjectId)
                .recordKey(recordKey)
                .value(value)
                .purpose("preferences")
                .retentionDays(365)
                .createdAt(timestamp)
                .updatedAt(timestamp)
                .version(1L)
                .requestId("req-test")
                .build();
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
}
