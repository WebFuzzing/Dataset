package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.AdditionalDetailsConverter;
import lombok.*;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbConvertedBy;

import java.util.Map;

@Setter
@Getter
@DynamoDbBean
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ErrorDetails {

    public static final String COL_CAUSE = "cause";
    public static final String COL_MESSAGE = "message";
    public static final String COL_ADDITIONAL_DETAILS = "additionalDetails";

    private ErrorCause cause;
    private String message;
    @Getter(onMethod = @__({ @DynamoDbConvertedBy(AdditionalDetailsConverter.class) }))
    private Map<String, Object> additionalDetails;
}
