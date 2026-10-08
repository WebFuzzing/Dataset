package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;
import java.util.List;

@DynamoDbBean
@Data
public class ValidationFlow {

    public static final String COL_SEQUENCES_VALIDATION_TIMESTAMP = "sequencesValidationTimestamp";
    public static final String COL_FINAL_EVENT_DEMAT_VALIDATION_TIMESTAMP = "finalEventDematValidationTimestamp";
    public static final String COL_REFINEMENT_DEMAT_VALIDATION_TIMESTAMP = "refinementDematValidationTimestamp";
    public static final String COL_FINAL_EVENT_BUILDER_TIMESTAMP = "finalEventBuilderTimestamp";
    public static final String COL_RECAG012_STATUS_TIMESTAMP = "recag012StatusTimestamp";
    public static final String COL_OCR_REQUESTS = "ocrRequests";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_SEQUENCES_VALIDATION_TIMESTAMP)}))
    private Instant sequencesValidationTimestamp;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FINAL_EVENT_DEMAT_VALIDATION_TIMESTAMP)}))
    private Instant finalEventDematValidationTimestamp;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REFINEMENT_DEMAT_VALIDATION_TIMESTAMP)}))
    private Instant refinementDematValidationTimestamp;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_FINAL_EVENT_BUILDER_TIMESTAMP)}))
    private Instant finalEventBuilderTimestamp;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_RECAG012_STATUS_TIMESTAMP)}))
    private Instant recag012StatusTimestamp;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_OCR_REQUESTS)}))
    private List<OcrRequest> ocrRequests;

}
