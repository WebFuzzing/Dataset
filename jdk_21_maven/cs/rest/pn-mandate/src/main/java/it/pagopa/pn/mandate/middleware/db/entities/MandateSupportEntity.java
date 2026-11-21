package it.pagopa.pn.mandate.middleware.db.entities;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbAttribute;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbBean;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbPartitionKey;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.DynamoDbSortKey;

import java.time.Instant;

/**
 * Entity di supporto alla delega
 */
@DynamoDbBean
@Data
@Setter
@NoArgsConstructor
public class MandateSupportEntity {

    public static final String MANDATE_TRIGGERHELPER_PREFIX = "MANDATETRIGGERHELPER#";


    public static final String COL_PK = "pk";
    public static final String COL_SK = "sk";
    public static final String COL_DELEGATOR_UID = "s_delegatoruid";
    public static final String COL_DELEGATOR_TYPE = "s_delegatortype";
    public static final String COL_I_TTL = "i_ttl";

    public MandateSupportEntity(MandateEntity source)
    {
       this(source, source.getValidto());
    }


    public MandateSupportEntity(MandateEntity source, Instant expire)
    {
        this.setDelegator(source.getDelegator());
        this.setMandateId(source.getMandateId());
        this.setDelegatorUid(source.getDelegatorUid());
        this.setDelegatorType(Boolean.TRUE.equals(source.getDelegatorisperson())?"PF":"PG");
        if (expire != null) {
            this.setTtl(expire.getEpochSecond());
        }
    }


    public String getMandateId(){
        return this.sk.replace(MANDATE_TRIGGERHELPER_PREFIX, "");
    }
    public void setMandateId(String id){
        this.sk = MANDATE_TRIGGERHELPER_PREFIX + id;
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String delegator;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))  private String sk;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_DELEGATOR_UID)}))  private String delegatorUid;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_DELEGATOR_TYPE)}))  private String delegatorType;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_I_TTL)}))  private Long ttl;

}
