package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;
import java.util.List;

@DynamoDbBean
@Data
public class PaperTrackerDryRunOutputs {
    public static final String COL_TRACKING_ID = "trackingId";
    public static final String COL_CREATED = "created";
    public static final String COL_REGISTERED_LETTER_CODE = "registeredLetterCode";
    public static final String COL_STATUS_CODE = "statusCode";
    public static final String COL_STATUS_DETAIL = "statusDetail";
    public static final String COL_STATUS_DESCRIPTION = "statusDescription";
    public static final String COL_STATUS_DATE_TIME = "statusDateTime";
    public static final String COL_DELIVERY_FAILURE_CAUSE = "deliveryFailureCause";
    public static final String COL_ATTACHMENTS = "attachments";
    public static final String COL_DISCOVERED_ADDRESS = "discoveredAddress";
    public static final String COL_CLIENT_REQUEST_TIMESTAMP = "clientRequestTimestamp";

    @Getter(onMethod = @__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_TRACKING_ID)}))
    private String trackingId;

    @Getter(onMethod = @__({@DynamoDbSortKey, @DynamoDbAttribute(COL_CREATED)}))
    private Instant created;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REGISTERED_LETTER_CODE)}))
    private String registeredLetterCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_CODE)}))
    private String statusCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DETAIL)}))
    private String statusDetail;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DESCRIPTION)}))
    private String statusDescription;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STATUS_DATE_TIME)}))
    private String statusDateTime;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DELIVERY_FAILURE_CAUSE)}))
    private String deliveryFailureCause;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ATTACHMENTS)}))
    private List<Attachment> attachments;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DISCOVERED_ADDRESS)}))
    private String anonymizedDiscoveredAddressId;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_CLIENT_REQUEST_TIMESTAMP)}))
    private String clientRequestTimestamp;

}
