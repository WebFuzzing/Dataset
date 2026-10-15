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
public class PaperStatus {

    public static final String COL_REGISTERED_LETTER_CODE = "registeredLetterCode";
    public static final String COL_DELIVERY_FAILURE_CAUSE = "deliveryFailureCause";
    public static final String COL_ANONYMIZED_DISCOVERED_ADDRESS = "anonymizedDiscoveredAddress";
    public static final String COL_FINAL_STATUS_CODE = "finalStatusCode";
    public static final String COL_VALIDATED_SEQUENCE_TIMESTAMP = "validatedSequenceTimestamp";
    public static final String COL_VALIDATED_EVENTS = "validatedEvents";
    public static final String COL_FINAL_DEMAT_FOUND = "finalDematFound";
    public static final String COL_PAPER_DELIVERY_TIMESTAMP = "paperDeliveryTimestamp";
    public static final String COL_PREDICTED_REFINEMENT_TYPE = "predictedRefinementType";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REGISTERED_LETTER_CODE)}))
    private String registeredLetterCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DELIVERY_FAILURE_CAUSE)}))
    private String deliveryFailureCause;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ANONYMIZED_DISCOVERED_ADDRESS)}))
    private String anonymizedDiscoveredAddress;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FINAL_STATUS_CODE)}))
    private String finalStatusCode;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_VALIDATED_SEQUENCE_TIMESTAMP)}))
    private Instant validatedSequenceTimestamp;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_VALIDATED_EVENTS), @DynamoDbIgnoreNulls}))
    private List<String> validatedEvents;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FINAL_DEMAT_FOUND)}))
    private Boolean finalDematFound;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PAPER_DELIVERY_TIMESTAMP)}))
    private Instant paperDeliveryTimestamp;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_PREDICTED_REFINEMENT_TYPE)}))
    private String predictedRefinementType;

}
