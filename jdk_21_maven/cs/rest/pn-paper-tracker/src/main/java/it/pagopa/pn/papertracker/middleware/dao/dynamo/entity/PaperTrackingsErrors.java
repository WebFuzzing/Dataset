package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

@Setter
@Getter
@ToString
@DynamoDbBean
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaperTrackingsErrors {

    public static final String COL_TRACKING_ID = "trackingId";
    public static final String COL_CREATED = "created";
    public static final String COL_CATEGORY = "category";
    public static final String COL_DETAILS = "details";
    public static final String COL_FLOW_THROW = "flowThrow";
    public static final String COL_EVENT_THROW = "eventThrow";
    public static final String COL_EVENT_ID_THROW = "eventIdThrow";
    public static final String COL_PRODUCT_TYPE = "productType";
    public static final String COL_TYPE = "type";
    public static final String COL_TTL = "ttl";
    public static final String COL_UNIFIED_DELIVERY_DRIVER = "unifiedDeliveryDriver";
    public static final String COL_REGISTERED_LETTER_CODE = "registeredLetterCode";

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_TRACKING_ID)}))
    private String trackingId;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_CREATED)}))
    private Instant created;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CATEGORY)}))
    private ErrorCategory errorCategory;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DETAILS)}))
    private ErrorDetails details;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FLOW_THROW)}))
    private FlowThrow flowThrow;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_EVENT_THROW)}))
    private String eventThrow;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_EVENT_ID_THROW)}))
    private String eventIdThrow;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRODUCT_TYPE)}))
    private String productType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TYPE)}))
    private ErrorType type;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_TTL)}))
    private Long ttl;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_UNIFIED_DELIVERY_DRIVER)}))
    private String unifiedDeliveryDriver;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REGISTERED_LETTER_CODE)}))
    private String registeredLetterCode;

}
