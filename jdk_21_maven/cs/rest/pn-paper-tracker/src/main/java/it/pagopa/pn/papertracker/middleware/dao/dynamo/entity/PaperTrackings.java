package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@DynamoDbBean
@Data
public class PaperTrackings {

    public static final String COL_TRACKING_ID = "trackingId";
    public static final String COL_ATTEMPT_ID = "attemptId";
    public static final String COL_PC_RETRY = "pcRetry";
    public static final String COL_EVENTS = "events";
    public static final String COL_PAPER_STATUS = "paperStatus";
    public static final String COL_VALIDATION_FLOW = "validationFlow";
    public static final String COL_VALIDATION_CONFIG = "validationConfig";
    public static final String COL_PRODUCT_TYPE = "productType";
    public static final String COL_NEXT_REQUEST_ID_PC_RETRY = "nextRequestIdPcretry";
    public static final String COL_UNIFIED_DELIVERY_DRIVER = "unifiedDeliveryDriver";
    public static final String COL_CREATED_AT = "createdAt";
    public static final String COL_UPDATED_AT = "updatedAt";
    public static final String COL_STATE = "state";
    public static final String COL_BUSINESS_STATE = "businessState";
    public static final String COL_TTL = "ttl";
    public static final String COL_RECAG012_STATUS_TIMESTAMP = "recag012StatusTimestamp";
    public static final String COL_PENDING_FINAL_EVENT_ID = "pendingFinalEventId";
    public static final String ATTEMPT_ID_PCRETRY_INDEX = "attemptId-pcRetry-index";
    public static final String COL_REWORK_ID = "reworkId";
    public static final String COL_REWORK_REQUEST_TIMESTAMP = "reworkRequestTimestamp";
    public static final String COL_PROCESSING_MODE = "processingMode";
    public static final String COL_ANALOG_REQUEST_CLIENT_ID = "analogRequestClientId";

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_TRACKING_ID)}))
    private String trackingId;

    @Getter(onMethod = @__({@DynamoDbSecondaryPartitionKey(indexNames = ATTEMPT_ID_PCRETRY_INDEX), @DynamoDbAttribute(COL_ATTEMPT_ID)}))
    private String attemptId;

    @Getter(onMethod = @__({@DynamoDbSecondarySortKey(indexNames = ATTEMPT_ID_PCRETRY_INDEX), @DynamoDbAttribute(COL_PC_RETRY)}))
    private String pcRetry;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRODUCT_TYPE)}))
    private String productType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_UNIFIED_DELIVERY_DRIVER)}))
    private String unifiedDeliveryDriver;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_EVENTS), @DynamoDbIgnoreNulls}))
    private List<Event> events;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PAPER_STATUS), @DynamoDbIgnoreNulls}))
    private PaperStatus paperStatus;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_VALIDATION_FLOW), @DynamoDbIgnoreNulls}))
    private ValidationFlow validationFlow;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_VALIDATION_CONFIG), @DynamoDbIgnoreNulls}))
    private ValidationConfig validationConfig;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_NEXT_REQUEST_ID_PC_RETRY)}))
    private String nextRequestIdPcretry;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATE)}))
    private PaperTrackingsState state;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_BUSINESS_STATE)}))
    private BusinessState businessState;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PENDING_FINAL_EVENT_ID)}))
    private String pendingFinalEventId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CREATED_AT)}))
    private Instant createdAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_UPDATED_AT)}))
    private Instant updatedAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REWORK_REQUEST_TIMESTAMP)}))
    private Instant notificationReworkRequestTimestamp;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REWORK_ID)}))
    private String notificationReworkId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PROCESSING_MODE)}))
    private ProcessingMode processingMode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ANALOG_REQUEST_CLIENT_ID)}))
    private String analogRequestClientId;

    // Costruito UNA volta sola
    private static final TableSchema<PaperTrackings> SCHEMA =
            TableSchema.fromBean(PaperTrackings.class);

    /**
     * Converte l'entity PaperTrackings in una mappa {@code Map<String, AttributeValue>} utilizzata da DynamoDB.
     */
    public static Map<String, AttributeValue> paperTrackingsToAttributeValueMap(PaperTrackings p) {
        return SCHEMA.itemToMap(p, true);
    }

    /**
     * Converte una mappa {@code Map<String, AttributeValue>} utilizzata da DynamoDB in una entity PaperTrackings.
     */
    public static PaperTrackings attributeValueMapToPaperTrackings(Map<String, AttributeValue> item) {
        if (item == null || item.isEmpty()) return null;
        return SCHEMA.mapToItem(item);
    }

}
