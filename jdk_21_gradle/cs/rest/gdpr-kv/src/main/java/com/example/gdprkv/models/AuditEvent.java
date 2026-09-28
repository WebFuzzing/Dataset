package com.example.gdprkv.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.Map;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@JsonInclude(Include.NON_NULL)
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@Getter @Setter
public class AuditEvent {

    // Required fields — Lombok @NonNull enforces runtime null checks in builder
    @NonNull private String subjectId;   // PK
    @NonNull private String tsUlid;      // SK "{millis}_{ULID}"
    @NonNull private EventType eventType;
    @NonNull private String requestId;
    @NonNull private Long timestamp;
    @NonNull private String prevHash;

    // no @NonNull here — builder will fill it automatically
    private String hash;

    // Optional fields
    private String itemKey;
    private String purpose;
    private Map<String, Object> details;

    // ----- DynamoDB annotations on getters -----
    @DynamoDbPartitionKey
    @DynamoDbAttribute("subject_id")
    public String getSubjectId() { return subjectId; }

    @DynamoDbSortKey
    @DynamoDbAttribute("ts_ulid")
    public String getTsUlid() { return tsUlid; }

    @DynamoDbAttribute("event_type")
    public EventType getEventType() { return eventType; }

    @DynamoDbAttribute("request_id")
    public String getRequestId() { return requestId; }

    @DynamoDbAttribute("timestamp")
    public Long getTimestamp() { return timestamp; }

    @DynamoDbAttribute("prev_hash")
    public String getPrevHash() { return prevHash; }

    @DynamoDbAttribute("hash")
    public String getHash() { return hash; }

    @DynamoDbAttribute("item_key")
    public String getItemKey() { return itemKey; }

    @DynamoDbAttribute("purpose")
    public String getPurpose() { return purpose; }

    @DynamoDbConvertedBy(JsonStringMapAttributeConverter.class)
    @DynamoDbAttribute("details")
    public Map<String, Object> getDetails() { return details; }

    public enum EventType {
        CREATE_SUBJECT,
        CREATE_SUBJECT_REQUESTED,
        CREATE_SUBJECT_COMPLETED,
        CREATE_SUBJECT_FAILED,

        PUT_REQUESTED,
        PUT_FAILED,
        PUT_SUCCESS,
        PUT_NEW_ITEM_SUCCESS,
        PUT_UPDATE_ITEM_SUCCESS,

        GET_REQUESTED,
        GET_FAILURE,
        GET_SUCCESS,

        DELETE_ITEM_REQUESTED,
        DELETE_ITEM_FAILURE,
        DELETE_ITEM_ALREADY_TOMBSTONED,
        DELETE_ITEM_SUCCESSFUL,

        DELETE_SUBJECT_REQUESTED,
        DELETE_SUBJECT_NO_SUBJECT,
        DELETE_SUBJECT_FAILURE,
        DELETE_SUBJECT_SUCCESS,

        SUBJECT_ERASURE_REQUESTED,
        SUBJECT_ERASURE_STARTED,
        SUBJECT_ERASURE_FAILED,
        SUBJECT_ERASURE_COMPLETED,

        PURGE_CANDIDATE_IDENTIFIED,
        PURGE_CANDIDATE_SUCCESSFUL,
        PURGE_CANDIDATE_FAILED;

        @JsonCreator
        public static EventType fromString(String v) {
            for (EventType t : values()) {
                if (t.name().equals(v)) {
                    return t;
                }
            }
            throw new IllegalArgumentException("Unknown AuditEvent.EventType: " + v);
        }
    }

    // hash chain helpers
    public static String computeHash(AuditEvent e) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            String detailsJson = JsonStringMapAttributeConverter.toJsonString(
                    e.details == null ? Collections.emptyMap() : e.details
            );
            String canon = String.join("|",
                    nn(e.subjectId),
                    nn(e.tsUlid),
                    e.eventType == null ? "" : e.eventType.name(),
                    nn(e.requestId),
                    nn(e.itemKey),
                    nn(e.purpose),
                    e.timestamp == null ? "" : String.valueOf(e.timestamp),
                    detailsJson,
                    nn(e.prevHash)
            );
            byte[] digest = md.digest(canon.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to compute AuditEvent hash", ex);
        }
    }

    private static String nn(String s) { return s == null ? "" : s; }

    private static String bytesToHex(byte[] bytes) {
        final char[] HEX = "0123456789abcdef".toCharArray();
        char[] out = new char[bytes.length * 2];
        for (int i = 0, j = 0; i < bytes.length; i++) {
            int v = bytes[i] & 0xFF;
            out[j++] = HEX[v >>> 4];
            out[j++] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    public static class AuditEventBuilder {
        public AuditEvent build() {
            AuditEvent e = new AuditEvent(
                    subjectId, tsUlid, eventType, requestId, timestamp, prevHash,
                    null, itemKey, purpose, details
            );
            e.hash = computeHash(e);
            return e;
        }
    }
}
