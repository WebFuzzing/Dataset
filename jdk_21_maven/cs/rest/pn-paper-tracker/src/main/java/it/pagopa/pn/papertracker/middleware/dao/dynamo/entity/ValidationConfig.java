package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

import it.pagopa.pn.papertracker.model.FileType;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.util.List;

@DynamoDbBean
@Data
public class ValidationConfig {

    public static final String COL_OCR_ENABLED = "ocrEnabled";
    public static final String COL_OCR_FILE_TYPES = "ocrFileTypes";
    public static final String COL_REQUIRED_ATTACHMENTS_REFINEMENT_STOCK_890 = "requiredAttachmentsRefinementStock890";
    public static final String COL_SEND_OCR_REFINEMENT_STOCK_890 = "sendOcrAttachmentsRefinementStock890";
    public static final String COL_SEND_OCR_FINAL_VALIDATION_STOCK_890 = "sendOcrAttachmentsFinalValidationStock890";
    public static final String COL_SEND_OCR_FINAL_VALIDATION = "sendOcrAttachmentsFinalValidation";
    public static final String COL_STRICT_FINAL_VALIDATION_STOCK_890 = "strictFinalValidationStock890";
    public static final String COL_STRICT_DELIVERY_FAILURE_CAUSE = "strictDeliveryFailureCause";
    public static final String COL_OCR_FILTER_TEMPORAL = "ocrFilterTemporal";
    public static final String COL_OCR_FILTER_UNIFIED_DELIVERY_DRIVER = "ocrFilterUnifiedDeliveryDriver";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_OCR_ENABLED)}))
    private OcrStatusEnum ocrEnabled;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_OCR_FILE_TYPES)}))
    private List<String> ocrFileTypes;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_REQUIRED_ATTACHMENTS_REFINEMENT_STOCK_890)}))
    private List<String> requiredAttachmentsRefinementStock890;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_SEND_OCR_REFINEMENT_STOCK_890)}))
    private List<String> sendOcrAttachmentsRefinementStock890;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_SEND_OCR_FINAL_VALIDATION_STOCK_890)}))
    private List<String> sendOcrAttachmentsFinalValidationStock890;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_SEND_OCR_FINAL_VALIDATION)}))
    private List<String> sendOcrAttachmentsFinalValidation;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STRICT_FINAL_VALIDATION_STOCK_890)}))
    private Boolean strictFinalValidationStock890;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_OCR_FILTER_TEMPORAL)}))
    private String ocrFilterTemporal;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_OCR_FILTER_UNIFIED_DELIVERY_DRIVER)}))
    private List<String> ocrFilterUnifiedDeliveryDriver;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_STRICT_DELIVERY_FAILURE_CAUSE)}))
    private Boolean strictDeliveryFailureCause;
}
