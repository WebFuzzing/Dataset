package it.pagopa.pn.papertracker.middleware.dao.dynamo.entity;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;

import java.time.Instant;

@DynamoDbBean
@Data
public class Attachment {

    public static final String COL_ID = "id";
    public static final String COL_DOCUMENT_TYPE = "documentType";
    public static final String COL_URI = "uri";
    public static final String COL_DATE = "date";
    public static final String COL_SHA256 = "sha256";
    public static final String COL_SOURCE_TYPE = "sourceType";
    public static final String COL_ORIGIN_TYPE = "originType";

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ID)}))
    private String id;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DOCUMENT_TYPE)}))
    private String documentType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_URI)}))
    private String uri;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_DATE)}))
    private Instant date;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_SHA256)}))
    private String sha256;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_SOURCE_TYPE)}))
    private String sourceType;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_ORIGIN_TYPE)}))
    private String originType;

}