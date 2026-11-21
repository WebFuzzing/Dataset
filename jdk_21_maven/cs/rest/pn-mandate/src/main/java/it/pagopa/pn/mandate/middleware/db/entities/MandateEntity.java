package it.pagopa.pn.mandate.middleware.db.entities;

import it.pagopa.pn.mandate.model.WorkFlowType;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import software.amazon.awssdk.enhanced.dynamodb.mapper.annotations.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static it.pagopa.pn.mandate.middleware.db.BaseDao.GSI_INDEX_DELEGATE_STATE;

/**
 * Entity Delega
 */
@DynamoDbBean
@Data
@NoArgsConstructor
public class MandateEntity {

    public static final String MANDATE_PREFIX = "MANDATE#";

    public static final String COL_PK = "pk";
    public static final String COL_SK = "sk";
    public static final String COL_S_DELEGATE = "s_delegate";
    public static final String COL_S_DELEGATORUID = "s_delegatoruid";
    public static final String COL_I_STATE = "i_state";
    public static final String COL_B_DELEGATORISPERSON = "b_delegatorisperson";
    public static final String COL_B_DELEGATEISPERSON = "b_delegateisperson";
    public static final String COL_D_VALIDFROM = "d_validfrom";
    public static final String COL_D_VALIDTO = "d_validto";
    public static final String COL_S_MANDATEID = "s_mandateid";
    public static final String COL_T_CREATED = "t_created";
    public static final String COL_T_ACCEPTED = "t_accepted";
    public static final String COL_T_REJECTED = "t_rejected";
    public static final String COL_T_REVOKED = "t_revoked";
    public static final String COL_S_VALIDATIONCODE = "s_validationcode";
    public static final String COL_A_VISIBILITYIDS = "a_visibilityids";
    public static final String COL_I_TTL = "i_ttl";
    public static final String COL_A_GROUPS = "a_groups";
    public static final String COL_S_WORKFLOW_TYPE = "s_workflowtype";
    public static final String COL_A_IUNS = "a_iuns";
    public static final String COL_A_TAXONOMY_CODES = "a_taxonomycodes";
    public static final String COL_S_SRC_CHANNEL = "s_srcchannel";

    public MandateEntity(String delegator, String mandateId) {
        this.setDelegator(delegator);
        this.setMandateId(mandateId);
    }

    public MandateEntity(MandateEntity mandateEntity) {
        this.setDelegator(mandateEntity.getDelegator());
        this.setMandateId(mandateEntity.getMandateId());
        this.setDelegate(mandateEntity.getDelegate());
        this.setState(mandateEntity.getState());
        this.setDelegatorisperson(mandateEntity.getDelegatorisperson());
        this.setDelegateisperson(mandateEntity.getDelegateisperson());
        this.setValidfrom(mandateEntity.getValidfrom());
        this.setValidto(mandateEntity.getValidto());
        this.setCreated(mandateEntity.getCreated());
        this.setAccepted(mandateEntity.getAccepted());
        this.setRejected(mandateEntity.getRejected());
        this.setRevoked(mandateEntity.getRevoked());
        this.setValidationcode(mandateEntity.getValidationcode());
        if (mandateEntity.getVisibilityIds() != null) {
            this.setVisibilityIds(new HashSet<>(mandateEntity.getVisibilityIds()));
        }
        this.setDelegatorUid(mandateEntity.getDelegatorUid());
        if (mandateEntity.getGroups() != null) {
            this.setGroups(new HashSet<>(mandateEntity.getGroups()));
        }
        this.setTtl(mandateEntity.getTtl());
        this.setWorkflowType(mandateEntity.getWorkflowType());
        this.setIuns(mandateEntity.getIuns());
        this.setSrcChannel(mandateEntity.getSrcChannel());
        this.setTaxonomyCodes(mandateEntity.getTaxonomyCodes());
    }

    @DynamoDbAttribute(COL_S_MANDATEID)
    public String getMandateId(){
        return this.sk.replace(MANDATE_PREFIX, "");
    }
    public void setMandateId(String id){
        this.sk = MANDATE_PREFIX + id;
    }

    @Getter(onMethod=@__({@DynamoDbPartitionKey, @DynamoDbAttribute(COL_PK)})) private String delegator;
    @Getter(onMethod=@__({@DynamoDbSortKey, @DynamoDbAttribute(COL_SK)}))  private String sk;


    @Getter(onMethod=@__({@DynamoDbSecondaryPartitionKey(indexNames = { GSI_INDEX_DELEGATE_STATE}), @DynamoDbAttribute(COL_S_DELEGATE)}))  private String delegate;
    @Getter(onMethod=@__({@DynamoDbSecondarySortKey(indexNames = { GSI_INDEX_DELEGATE_STATE }), @DynamoDbAttribute(COL_I_STATE)}))  private int state;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_B_DELEGATORISPERSON)}))  private Boolean delegatorisperson;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_B_DELEGATEISPERSON)}))  private Boolean delegateisperson;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_D_VALIDFROM)}))  private Instant validfrom;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_D_VALIDTO)}))  private Instant validto;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_T_CREATED)}))  private Instant created;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_T_ACCEPTED)}))  private Instant accepted;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_T_REJECTED)}))  private Instant rejected;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_T_REVOKED)}))  private Instant revoked;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_S_VALIDATIONCODE)}))  private String validationcode;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_A_VISIBILITYIDS)}))  private Set<String> visibilityIds;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_S_DELEGATORUID)}))  private String delegatorUid;

    @Getter(onMethod = @__({@DynamoDbAttribute(COL_A_GROUPS)})) private Set<String> groups;

    @Getter(onMethod=@__({@DynamoDbAttribute(COL_S_WORKFLOW_TYPE)}))  private WorkFlowType workflowType;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_S_SRC_CHANNEL)}))  private String srcChannel;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_A_IUNS)}))  private Set<String> iuns;
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_A_TAXONOMY_CODES)}))  private Set<String> taxonomyCodes;

    // per lo storico e per la struttura dati di appoggio
    @Getter(onMethod=@__({@DynamoDbAttribute(COL_I_TTL)}))  private Long ttl;
    
}
