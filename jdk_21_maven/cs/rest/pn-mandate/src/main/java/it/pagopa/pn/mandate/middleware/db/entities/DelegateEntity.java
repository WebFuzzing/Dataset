package it.pagopa.pn.mandate.middleware.db.entities;

import lombok.Data;
import lombok.Getter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

/**
 * Entity Delegato
 */
@DynamoDbBean
@Data
public class DelegateEntity {

    public static final String COL_I_PENDINGCOUNT = "i_pendingcount";

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_I_PENDINGCOUNT)})) private int pendingcount;
}
