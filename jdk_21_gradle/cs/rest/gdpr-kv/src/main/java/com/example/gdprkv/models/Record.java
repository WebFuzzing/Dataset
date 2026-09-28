package com.example.gdprkv.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondaryPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSecondarySortKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

@JsonInclude(Include.NON_NULL)
@DynamoDbBean
@NoArgsConstructor                     // needed for DynamoDB Enhanced Client reflection
@AllArgsConstructor(access = AccessLevel.PRIVATE) // used by Lombok @Builder
@Builder(toBuilder = true)
@Getter @Setter
public class Record {

    private static final long MILLIS_PER_DAY = Duration.ofDays(1).toMillis();
    private static final DateTimeFormatter PURGE_BUCKET_FORMATTER =
            DateTimeFormatter.ofPattern("'h#'yyyyMMdd'T'HH").withZone(ZoneOffset.UTC);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    // Required fields — Lombok @NonNull enforces runtime null checks in builder
    @NonNull
    private String subjectId;

    @NonNull
    private String recordKey;

    @NonNull
    @NotBlank
    private String purpose;

    @NonNull
    private Long createdAt;

    @NonNull
    private Long version;

    @NonNull
    private String requestId;

    // Mutable fields
    private JsonNode value;

    @NonNull
    @Default
    private Boolean tombstoned = Boolean.FALSE;
    @NonNull
    private Long updatedAt;
    @NonNull
    private Integer retentionDays;
    private Long tombstonedAt;
    private Long purgeDueAt;
    private String purgeBucket;

    // ----- DynamoDB Enhanced annotations on getters -----

    @DynamoDbPartitionKey
    @DynamoDbAttribute("subject_id")
    public String getSubjectId() { return subjectId; }

    @DynamoDbSortKey
    @DynamoDbAttribute("record_key")
    public String getRecordKey() { return recordKey; }

    @DynamoDbAttribute("purpose")
    public String getPurpose() { return purpose; }

    @DynamoDbAttribute("value")
    @DynamoDbConvertedBy(JsonNodeAttributeConverter.class)
    public JsonNode getValue() { return value; }

    @DynamoDbAttribute("created_at")
    public Long getCreatedAt() { return createdAt; }

    @DynamoDbAttribute("updated_at")
    public Long getUpdatedAt() { return updatedAt; }

    @DynamoDbAttribute("version")
    public Long getVersion() { return version; }

    @DynamoDbAttribute("tombstoned")
    public Boolean getTombstoned() { return tombstoned; }

    @DynamoDbAttribute("tombstoned_at")
    public Long getTombstonedAt() { return tombstonedAt; }

    @DynamoDbAttribute("retention_days")
    public Integer getRetentionDays() { return retentionDays; }

    @DynamoDbAttribute("purge_due_at")
    @DynamoDbSecondarySortKey(indexNames = "records_by_purge_due")
    public Long getPurgeDueAt() { return purgeDueAt; }

    @DynamoDbAttribute("purge_bucket")
    @DynamoDbSecondaryPartitionKey(indexNames = "records_by_purge_due")
    public String getPurgeBucket() { return purgeBucket; }

    @DynamoDbAttribute("request_id")
    public String getRequestId() { return requestId; }

    // ----- Domain helpers -----

    public Record markTombstoned(long tombstonedAtMillis, int retentionDaysValue) {
        this.tombstoned = Boolean.TRUE;
        this.tombstonedAt = tombstonedAtMillis;
        this.retentionDays = retentionDaysValue;
        this.purgeDueAt = calculatePurgeDueAt(tombstonedAtMillis, retentionDaysValue);
        this.purgeBucket = formatPurgeBucket(this.purgeDueAt);
        return this;
    }

    public Record clearTombstoneMetadata() {
        this.tombstoned = Boolean.FALSE;
        this.tombstonedAt = null;
        this.purgeDueAt = null;
        this.purgeBucket = null;
        return this;
    }

    public Record withValueObject(Object payload) {
        this.value = payload == null ? null : OBJECT_MAPPER.valueToTree(payload);
        return this;
    }

    public static long calculatePurgeDueAt(long tombstonedAtMillis, int retentionDaysValue) {
        if (retentionDaysValue < 0) {
            throw new IllegalArgumentException("retentionDays must be >= 0");
        }
        return tombstonedAtMillis + MILLIS_PER_DAY * retentionDaysValue;
    }

    public static String formatPurgeBucket(long purgeDueAtMillis) {
        return PURGE_BUCKET_FORMATTER.format(Instant.ofEpochMilli(purgeDueAtMillis));
    }

    public static class RecordBuilder {
        public RecordBuilder value(JsonNode node) {
            this.value = node;
            return this;
        }

        public RecordBuilder valueObject(Object payload) {
            this.value = payload == null ? null : OBJECT_MAPPER.valueToTree(payload);
            return this;
        }
    }
}
