package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbIgnoreNulls;

import java.time.Instant;
import java.util.List;

@DynamoDbBean
@Data
public class Event {

    public static final String COL_REQUEST_TIMESTAMP = "requestTimestamp";
    public static final String COL_STATUS_CODE = "statusCode";
    public static final String COL_STATUS_DESCRIPTION = "statusDescription";
    public static final String COL_STATUS_TIMESTAMP = "statusTimestamp";
    public static final String COL_PRODUCT_TYPE = "productType";
    public static final String COL_DELIVERY_FAILURE_CAUSE = "deliveryFailureCause";
    public static final String COL_DISCOVERED_ADDRESS = "discoveredAddress";
    public static final String COL_ANONYMIZED_DISCOVERED_ADDRESS_ID = "anonymizedDiscoveredAddressId";
    public static final String COL_ATTACHMENTS = "attachments";
    public static final String COL_REGISTERED_LETTER_CODE = "registeredLetterCode";
    public static final String COL_ID = "id";
    public static final String COL_DRY_RUN = "dryRun";
    public static final String COL_IUN = "iun";
    public static final String COL_CREATED_AT = "createdAt";
    public static final String COL_REWORK_ID = "reworkId";
    public static final String COL_PRINTER = "printer";
    public static final String COL_DU = "du";


    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ID)}))
    private String id;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_IUN)}))
    private String iun;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REQUEST_TIMESTAMP)}))
    private Instant requestTimestamp;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_CODE)}))
    private String statusCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DESCRIPTION)}))
    private String statusDescription;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_TIMESTAMP)}))
    private Instant statusTimestamp;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRODUCT_TYPE)}))
    private String productType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DELIVERY_FAILURE_CAUSE)}))
    private String deliveryFailureCause;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ANONYMIZED_DISCOVERED_ADDRESS_ID)}))
    private String anonymizedDiscoveredAddressId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ATTACHMENTS), @DynamoDbIgnoreNulls}))
    private List<Attachment> attachments;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REGISTERED_LETTER_CODE)}))
    private String registeredLetterCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DRY_RUN)}))
    private Boolean dryRun;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CREATED_AT)}))
    private Instant createdAt;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REWORK_ID)}))
    private String notificationReworkId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PRINTER)}))
    private String printer;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DU)}))
    private String du;

}
