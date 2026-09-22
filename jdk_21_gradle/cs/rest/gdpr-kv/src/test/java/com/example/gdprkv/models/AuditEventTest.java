package com.example.gdprkv.models;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.exc.ValueInstantiationException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

class AuditEventTest {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);

    private static final String FIXTURE = "/fixtures/auditEvent.json";

    private static String readFixture(String path) throws Exception {
        try (InputStream in = AuditEventTest.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing test fixture: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Builder auto-computes hash (callers never set it)")
    void builderAutoComputesHash() {
        AuditEvent e = AuditEvent.builder()
                .subjectId("s1")
                .tsUlid("1000_01HABCDEFGABCDEFGABCDEFG")
                .eventType(AuditEvent.EventType.CREATE_SUBJECT)
                .requestId("r1")
                .timestamp(1000L)
                .prevHash("00")
                .details(Map.of("status", "STARTED"))
                .build();

        assertNotNull(e.getHash(), "Builder should compute a non-null hash");
        assertEquals(AuditEvent.computeHash(e), e.getHash(), "Hash should match computeHash()");
    }

    @Test
    @DisplayName("Deserialize fixture → fields populated (hash preserved from JSON)")
    void deserializeFixture() throws Exception {
        String json = readFixture(FIXTURE);
        AuditEvent e = MAPPER.readValue(json, AuditEvent.class);

        assertEquals("subj-123", e.getSubjectId());
        assertEquals("1726768765000_01JABCDE1234XYZ7890ABCD", e.getTsUlid());
        assertEquals(AuditEvent.EventType.PUT_SUCCESS, e.getEventType());
        assertEquals("req-456", e.getRequestId());
        assertEquals("orders#2025-09-20", e.getItemKey());
        assertEquals("ORDER_FULFILLMENT", e.getPurpose());
        assertEquals(1726768765123L, e.getTimestamp());
        assertNotNull(e.getDetails());
        assertEquals("OK", e.getDetails().get("status"));
        assertEquals(3, e.getDetails().get("count"));
        assertEquals(
                "0000000000000000000000000000000000000000000000000000000000000000",
                e.getPrevHash()
        );
        assertNotNull(e.getHash());
    }

    @Test
    @DisplayName("Serialize ↔ matches fixture semantically (by injecting computed hash)")
    void serializeMatchesFixtureSemantically() throws Exception {
        // Build the event; builder computes hash automatically
        AuditEvent e = AuditEvent.builder()
                .subjectId("subj-123")
                .tsUlid("1726768765000_01JABCDE1234XYZ7890ABCD")
                .eventType(AuditEvent.EventType.PUT_SUCCESS)
                .requestId("req-456")
                .itemKey("orders#2025-09-20")
                .purpose("ORDER_FULFILLMENT")
                .timestamp(1726768765123L)
                .details(Map.of("status", "OK", "count", 3))
                .prevHash("0000000000000000000000000000000000000000000000000000000000000000")
                .build();

        // Load fixture and replace its placeholder hash with computed one
        JsonNode expected = MAPPER.readTree(readFixture(FIXTURE));
        ((ObjectNode) expected).put("hash", e.getHash());

        JsonNode actual = MAPPER.readTree(MAPPER.writeValueAsString(e));
        assertEquals(expected, actual, "Serialized JSON should match fixture with computed hash");
    }

    @Test
    @DisplayName("JSON round-trip preserves semantic equality")
    void jsonRoundTrip() throws Exception {
        AuditEvent original = AuditEvent.builder()
                .subjectId("subj-xyz")
                .tsUlid("1726768000000_01JABCDEFFFFFFFFFFFFFF")
                .eventType(AuditEvent.EventType.GET_SUCCESS)
                .requestId("req-999")
                .timestamp(1726768000123L)
                .details(Map.of("status", "OK", "count", 1))
                .prevHash("aa")
                .itemKey("orders#42")
                .purpose("FULFILLMENT")
                .build();

        String json = MAPPER.writeValueAsString(original);
        AuditEvent back = MAPPER.readValue(json, AuditEvent.class);

        assertEquals(original.getSubjectId(), back.getSubjectId());
        assertEquals(original.getTsUlid(), back.getTsUlid());
        assertEquals(original.getEventType(), back.getEventType());
        assertEquals(original.getRequestId(), back.getRequestId());
        assertEquals(original.getTimestamp(), back.getTimestamp());
        assertEquals(original.getPrevHash(), back.getPrevHash());
        assertEquals(original.getItemKey(), back.getItemKey());
        assertEquals(original.getPurpose(), back.getPurpose());
        assertEquals(original.getDetails().get("status"), back.getDetails().get("status"));
        assertEquals(original.getDetails().get("count"), back.getDetails().get("count"));
        assertEquals(original.getHash(), back.getHash());
    }

    @Test
    @DisplayName("Builder enforces @NonNull on required fields")
    void builderEnforcesNonNull() {
        // Required: subjectId, tsUlid, eventType, requestId, timestamp, prevHash
        assertThrows(NullPointerException.class, () -> AuditEvent.builder().build());
        assertThrows(NullPointerException.class, () -> AuditEvent.builder()
                .subjectId("s").build());
        assertThrows(NullPointerException.class, () -> AuditEvent.builder()
                .subjectId("s").tsUlid("1_01H...").build());
        assertThrows(NullPointerException.class, () -> AuditEvent.builder()
                .subjectId("s").tsUlid("1_01H...").eventType(AuditEvent.EventType.GET_REQUESTED).build());
        assertThrows(NullPointerException.class, () -> AuditEvent.builder()
                .subjectId("s").tsUlid("1_01H...").eventType(AuditEvent.EventType.GET_REQUESTED)
                .requestId("r").build());
        assertThrows(NullPointerException.class, () -> AuditEvent.builder()
                .subjectId("s").tsUlid("1_01H...").eventType(AuditEvent.EventType.GET_REQUESTED)
                .requestId("r").timestamp(1L).build());
    }

    @Test
    @DisplayName("Hash chain: deterministic and sensitive to all canonical inputs (incl. optionals)")
    void hashDeterminismAndSensitivity() {
        AuditEvent a = AuditEvent.builder()
                .subjectId("S")
                .tsUlid("1000_01HABCDEFGABCDEFGABCDEFG")
                .eventType(AuditEvent.EventType.CREATE_SUBJECT)
                .requestId("r1")
                .timestamp(1000L)
                .details(Map.of("status", "STARTED", "attempt", 1))
                .prevHash("00")
                .build();
        String h1 = a.getHash();

        // Same values -> same hash
        AuditEvent b = a.toBuilder().build();
        assertEquals(h1, b.getHash());

        // Change prevHash -> different hash
        AuditEvent c = a.toBuilder().prevHash("01").build();
        assertNotEquals(h1, c.getHash());

        // Add optional fields -> hash changes
        AuditEvent withOptionals = a.toBuilder()
                .itemKey("k1").purpose("TESTING").build();
        assertNotEquals(h1, withOptionals.getHash());
    }

    @Test
    @DisplayName("Enum validation: unknown value throws during deserialization")
    void enumValidationUnknownThrows() {
        String json = """
            {
              "subject_id":"s",
              "ts_ulid":"1_01HABCDEFGABCDEFGABCDEFG",
              "event_type":"SOMETHING_UNKNOWN",
              "request_id":"r",
              "timestamp":1,
              "details":{"status":"X"},
              "prev_hash":"00",
              "hash":"11"
            }
            """;

        ValueInstantiationException ex =
                assertThrows(ValueInstantiationException.class, () -> MAPPER.readValue(json, AuditEvent.class));

        assertNotNull(ex.getCause());
        assertTrue(ex.getCause() instanceof IllegalArgumentException);
        assertTrue(ex.getCause().getMessage().contains("Unknown AuditEvent.EventType"),
                "message should mention unknown enum value");
    }

    @Test
    @DisplayName("DynamoDB annotations present with expected attribute names")
    void dynamoAnnotationsPresent() throws Exception {
        Method getSubjectId = AuditEvent.class.getMethod("getSubjectId");
        Method getTsUlid    = AuditEvent.class.getMethod("getTsUlid");
        Method getEventType = AuditEvent.class.getMethod("getEventType");
        Method getRequestId = AuditEvent.class.getMethod("getRequestId");
        Method getItemKey   = AuditEvent.class.getMethod("getItemKey");
        Method getPurpose   = AuditEvent.class.getMethod("getPurpose");
        Method getTimestamp = AuditEvent.class.getMethod("getTimestamp");
        Method getDetails   = AuditEvent.class.getMethod("getDetails");
        Method getPrevHash  = AuditEvent.class.getMethod("getPrevHash");
        Method getHash      = AuditEvent.class.getMethod("getHash");

        // PK/SK
        assertNotNull(getSubjectId.getAnnotation(DynamoDbPartitionKey.class));
        assertEquals("subject_id", getSubjectId.getAnnotation(DynamoDbAttribute.class).value());
        assertNotNull(getTsUlid.getAnnotation(DynamoDbSortKey.class));
        assertEquals("ts_ulid", getTsUlid.getAnnotation(DynamoDbAttribute.class).value());

        // Attribute names
        assertEquals("event_type", getEventType.getAnnotation(DynamoDbAttribute.class).value());
        assertEquals("request_id", getRequestId.getAnnotation(DynamoDbAttribute.class).value());
        assertEquals("item_key", getItemKey.getAnnotation(DynamoDbAttribute.class).value());
        assertEquals("purpose", getPurpose.getAnnotation(DynamoDbAttribute.class).value());
        assertEquals("timestamp", getTimestamp.getAnnotation(DynamoDbAttribute.class).value());
        assertEquals("prev_hash", getPrevHash.getAnnotation(DynamoDbAttribute.class).value());
        assertEquals("hash", getHash.getAnnotation(DynamoDbAttribute.class).value());

        // details uses a converter and has the expected attribute name
        assertEquals("details", getDetails.getAnnotation(DynamoDbAttribute.class).value());
        assertNotNull(getDetails.getAnnotation(DynamoDbConvertedBy.class));
        assertEquals(JsonStringMapAttributeConverter.class,
                getDetails.getAnnotation(DynamoDbConvertedBy.class).value());
    }
}
