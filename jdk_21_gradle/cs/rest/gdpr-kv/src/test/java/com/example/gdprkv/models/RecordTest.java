package com.example.gdprkv.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

class RecordTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    @Test
    @DisplayName("Lifecycle: create → update → tombstone computes purge metadata")
    void lifecyclePutUpdateTombstone() {
        long createdAt = Instant.parse("2024-09-01T10:15:30Z").toEpochMilli();
        long updatedAt = Instant.parse("2024-09-01T11:00:00Z").toEpochMilli();
        int retentionDays = 30;

        Record record = Record.builder()
                .subjectId("sub_123")
                .recordKey("pref:email")
                .purpose("FULFILLMENT")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .version(1L)
                .requestId("req-001")
                .retentionDays(retentionDays)
                .valueObject(Map.of("email", "jess@example.com", "opt_in", true))
                .build();

        assertEquals(Boolean.FALSE, record.getTombstoned(), "new record should not be tombstoned");
        assertEquals(1L, record.getVersion());
        assertEquals("jess@example.com", record.getValue().get("email").asText());

        Record updated = record.toBuilder()
                .updatedAt(updatedAt)
                .version(2L)
                .requestId("req-002")
                .valueObject(Map.of(
                        "email", "jess@example.com",
                        "opt_in", false,
                        "channels", List.of("email", "sms")
                ))
                .build();

        JsonNode updatedValue = updated.getValue();
        assertEquals(2L, updated.getVersion());
        assertTrue(updatedValue.get("channels").isArray());
        assertEquals("sms", updatedValue.get("channels").get(1).asText());
        assertEquals(createdAt, record.getCreatedAt());
        assertEquals(updatedAt, updated.getUpdatedAt());

        long tombstonedAt = Instant.parse("2024-09-10T08:00:00Z").toEpochMilli();
        long expectedPurgeDue = Record.calculatePurgeDueAt(tombstonedAt, retentionDays);
        String expectedBucket = Record.formatPurgeBucket(expectedPurgeDue);

        updated.markTombstoned(tombstonedAt, retentionDays);

        assertEquals(Boolean.TRUE, updated.getTombstoned());
        assertEquals(tombstonedAt, updated.getTombstonedAt());
        assertEquals(retentionDays, updated.getRetentionDays());
        assertEquals(expectedPurgeDue, updated.getPurgeDueAt());
        assertEquals(expectedBucket, updated.getPurgeBucket());
    }

    @Test
    @DisplayName("valueObject helper supports scalar payloads")
    void valueObjectSupportsScalar() throws Exception {
        Record record = Record.builder()
                .subjectId("sub_987")
                .recordKey("token:refresh")
                .purpose("SECURITY")
                .createdAt(1L)
                .updatedAt(1L)
                .version(1L)
                .requestId("req-123")
                .retentionDays(7)
                .valueObject("opaque:value")
                .build();

        JsonNode node = record.getValue();
        assertNotNull(node);
        assertTrue(node.isTextual());
        assertEquals("opaque:value", node.asText());

        String json = MAPPER.writeValueAsString(record);
        JsonNode tree = MAPPER.readTree(json);
        assertEquals("opaque:value", tree.get("value").asText());
    }

    @Test
    @DisplayName("calculatePurgeDueAt rejects negative retention")
    void calculatePurgeDueAtRejectsNegativeRetention() {
        assertThrows(IllegalArgumentException.class, () -> Record.calculatePurgeDueAt(1L, -1));
    }

    @Test
    @DisplayName("DynamoDB annotations align with table and GSI schema")
    void dynamoAnnotations() throws Exception {
        Method getSubjectId = Record.class.getMethod("getSubjectId");
        Method getRecordKey = Record.class.getMethod("getRecordKey");
        Method getCreatedAt = Record.class.getMethod("getCreatedAt");
        Method getUpdatedAt = Record.class.getMethod("getUpdatedAt");
        Method getPurgeBucket = Record.class.getMethod("getPurgeBucket");
        Method getPurgeDueAt = Record.class.getMethod("getPurgeDueAt");

        assertNotNull(getSubjectId.getAnnotation(DynamoDbPartitionKey.class));
        assertEquals("subject_id", getSubjectId.getAnnotation(DynamoDbAttribute.class).value());

        assertNotNull(getRecordKey.getAnnotation(DynamoDbSortKey.class));
        assertEquals("record_key", getRecordKey.getAnnotation(DynamoDbAttribute.class).value());

        assertEquals("created_at", getCreatedAt.getAnnotation(DynamoDbAttribute.class).value());
        assertEquals("updated_at", getUpdatedAt.getAnnotation(DynamoDbAttribute.class).value());

        assertNotNull(getPurgeBucket.getAnnotation(DynamoDbSecondaryPartitionKey.class));
        assertEquals("purge_bucket", getPurgeBucket.getAnnotation(DynamoDbAttribute.class).value());

        assertNotNull(getPurgeDueAt.getAnnotation(DynamoDbSecondarySortKey.class));
        assertEquals("purge_due_at", getPurgeDueAt.getAnnotation(DynamoDbAttribute.class).value());
    }

    @Test
    @DisplayName("clearTombstoneMetadata resets purge fields and flags")
    void clearTombstoneMetadataResetsFields() {
        Record record = Record.builder()
                .subjectId("sub_321")
                .recordKey("pref:sms")
                .purpose("NOTIFICATIONS")
                .createdAt(10L)
                .updatedAt(10L)
                .version(1L)
                .requestId("req-777")
                .retentionDays(45)
                .build();

        long tombstonedAt = Instant.parse("2024-09-15T00:00:00Z").toEpochMilli();
        record.markTombstoned(tombstonedAt, record.getRetentionDays());

        assertEquals(Boolean.TRUE, record.getTombstoned());
        assertNotNull(record.getPurgeDueAt());
        assertNotNull(record.getPurgeBucket());

        record.clearTombstoneMetadata();

        assertFalse(record.getTombstoned());
        assertNull(record.getTombstonedAt());
        assertNull(record.getPurgeDueAt());
        assertNull(record.getPurgeBucket());
    }

    @Test
    @DisplayName("Builder value(JsonNode) stores provided tree")
    void builderValueStoresProvidedJsonNode() {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("type", "profile");
        node.put("active", true);

        Record record = Record.builder()
                .subjectId("sub_json")
                .recordKey("profile:base")
                .purpose("PROFILE")
                .createdAt(5L)
                .updatedAt(5L)
                .version(1L)
                .requestId("req-json")
                .retentionDays(365)
                .value(node)
                .build();

        assertEquals("profile", record.getValue().get("type").asText());
        assertTrue(record.getValue().get("active").asBoolean());
    }

    @Test
    @DisplayName("withValueObject mutates payload and supports null reset")
    void withValueObjectMutatesPayload() {
        Record record = Record.builder()
                .subjectId("sub_mut")
                .recordKey("prefs:push")
                .purpose("MARKETING")
                .createdAt(100L)
                .updatedAt(100L)
                .version(1L)
                .requestId("req-mut")
                .retentionDays(60)
                .build();

        record.withValueObject(Map.of("enabled", true, "frequency", "weekly"));

        assertTrue(record.getValue().get("enabled").asBoolean());
        assertEquals("weekly", record.getValue().get("frequency").asText());

        record.withValueObject(null);
        assertNull(record.getValue());
    }
}
