package it.pagopa.pn.mandate.middleware.db;

import it.pagopa.pn.commons.exceptions.PnInternalException;
import it.pagopa.pn.commons.log.PnAuditLogBuilder;
import it.pagopa.pn.commons.log.PnAuditLogEvent;
import it.pagopa.pn.commons.log.PnAuditLogEventType;
import it.pagopa.pn.mandate.config.PnMandateConfig;
import it.pagopa.pn.mandate.exceptions.*;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.CxTypeAuthFleet;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.DelegateType;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateByDelegatorRequestDto;
import it.pagopa.pn.mandate.generated.openapi.server.v1.dto.MandateDto.StatusEnum;
import it.pagopa.pn.mandate.mapper.StatusEnumMapper;
import it.pagopa.pn.mandate.middleware.db.entities.MandateEntity;
import it.pagopa.pn.mandate.middleware.db.entities.MandateSupportEntity;
import it.pagopa.pn.mandate.model.InputSearchMandateDto;
import it.pagopa.pn.mandate.model.WorkFlowType;
import it.pagopa.pn.mandate.utils.DateUtils;
import it.pagopa.pn.mandate.utils.MandateUtils;
import it.pagopa.pn.mandate.utils.RevocationCause;
import it.pagopa.pn.mandate.utils.TypeSegregatorFilter;
import org.springframework.context.annotation.Import;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.Select;

import java.time.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;

import static it.pagopa.pn.commons.utils.MDCUtils.*;
import static it.pagopa.pn.mandate.utils.PgUtils.buildExpressionGroupFilter;

@Repository
@lombok.CustomLog
@Import(PnAuditLogBuilder.class)
public class MandateDao extends BaseDao {


    private static final int MAX_DYNAMODB_BATCH_SIZE = 100;

    private static final String AND = " AND ";
    private static final String CONTAINS = "contains";
    private static final String EQ = "=";
    private static final String GT = ">";


    DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;
    DynamoDbAsyncClient dynamoDbAsyncClient;
    DynamoDbAsyncTable<MandateEntity> mandateTable;
    DynamoDbAsyncTable<MandateSupportEntity> mandateSupportTable;
    DynamoDbAsyncTable<MandateEntity> mandateHistoryTable;

    String table;
    Duration pendingExpire;
    Duration ciePendingExpire;

    public MandateDao(DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
                      DynamoDbAsyncClient dynamoDbAsyncClient,
                      PnMandateConfig awsConfigs) {
        this.mandateTable = dynamoDbEnhancedAsyncClient.table(awsConfigs.getDynamodbTable(), TableSchema.fromBean(MandateEntity.class));
        this.mandateSupportTable = dynamoDbEnhancedAsyncClient.table(awsConfigs.getDynamodbTable(), TableSchema.fromBean(MandateSupportEntity.class));
        this.mandateHistoryTable = dynamoDbEnhancedAsyncClient.table(awsConfigs.getDynamodbTableHistory(), TableSchema.fromBean(MandateEntity.class));
        this.table = awsConfigs.getDynamodbTable();
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
        this.pendingExpire = awsConfigs.getPendingDuration();
        this.ciePendingExpire= awsConfigs.getCiePendingDuration();
    }

    //#region public methods

    /**
     * Ritorna la lista delle deleghe per delegato
     *
     * @param searchMandateDto parametri di ricerca
     * @param typeSegregatorFilter segregatore da utilizzare per filtrare i tipi di delega (OPZIONALE, se null non viene applicato alcun filtro)
     * @return lista delle deleghe
     */
    public Flux<MandateEntity> listMandatesByDelegate(InputSearchMandateDto searchMandateDto, TypeSegregatorFilter typeSegregatorFilter) {
        // devo sempre filtrare. Se lo stato è passato, vuol dire che voglio filtrare solo per quello stato.
        // altrimenti, è IMPLICITO il fatto di filtrare per le deleghe pendenti e attive (ovvero < 20)
        // NB: listMandatesByDelegate e listMandatesByDelegator si assomigliano, ma a livello di query fanno
        // affidamento ad indici diversi e query diverse
        // listMandatesByDelegate si affida all'indice GSI delegate-state, che filtra per utente delegato E stato.
        log.info("listMandatesByDelegate searchMandateDto={}", searchMandateDto);

        QueryConditional queryConditional = QueryConditional.sortLessThanOrEqualTo(getKeyBuild(searchMandateDto.getDelegateId(), StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE)));
        if (searchMandateDto.getStatus() != null) {
            queryConditional = QueryConditional.keyEqualTo(getKeyBuild(searchMandateDto.getDelegateId(), searchMandateDto.getStatus()));    // si noti keyEqualTo al posto di sortLessThanOrEqualTo
        }

        // devo sempre mettere un filtro di salvaguardia per quanto riguarda la scadenza della delega.
        // infatti se una delega è scaduta, potrebbe rimanere a sistema per qualche ora/giorno prima di essere svecchiata
        // e quindi va sempre previsto un filtro sulla data di scadenza
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":now", AttributeValue.builder().s(Instant.now().toString()).build());
        String expression = getValidToFilterExpression();

        if (searchMandateDto.getMandateId() != null) {
            expressionValues.put(":mandateId", AttributeValue.builder().s(searchMandateDto.getMandateId()).build());
            expression += AND + getMandateFilterExpression();
        }

        if (CxTypeAuthFleet.PG == searchMandateDto.getCxType() && !CollectionUtils.isEmpty(searchMandateDto.getGroups())) {
            expression += AND + buildExpressionGroupFilter(searchMandateDto.getGroups(), expressionValues);
        }

        // Se indicato un segregatore, viene utilizzato per filtrare le deleghe in base al workflowType
        // In teoria dovrebbero indicarlo solo le API v1.
        if(typeSegregatorFilter != null) {
            String typeSegregatorExp = typeSegregatorFilter.buildExpression(expressionValues);
            if(!typeSegregatorExp.isEmpty()) {
                expression += AND + typeSegregatorExp;
            }
        }

        if(searchMandateDto.getIun() != null) {
            expression += AND + getIunFilterExpression(searchMandateDto.getIun(), expressionValues);
        }

        if(searchMandateDto.getNotificationSentAt() != null) {
            expression += AND + getNotificationSentAtFilterExpression(searchMandateDto.getNotificationSentAt(), expressionValues);
        }

        if(searchMandateDto.getRootSenderId() != null) {
            expression += AND + getSenderIdFilterExpression(searchMandateDto.getRootSenderId(), expressionValues);
        }

        log.debug("expression: {}", expression);

        Expression exp = Expression.builder()
                .expression(expression)
                .expressionValues(expressionValues)
                .build();


        // il filtro cambia in base al fatto se ho chiesto uno stato specifico (uso =)
        //   o se invece non chiedo lo stato (e quindi mi interessano pendenti e attive, uso <=)
        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(queryConditional)
                .filterExpression(exp)
                .scanIndexForward(true)
                .build();


        // viene volutamente ignorata la gestione della paginazione, che per ora non serve.
        // si suppone infatti che la lista delle deleghe non sia troppo lunga e quindi non vada a sforare il limite di 1MB di paginazione
        return Flux.from(mandateTable.index(GSI_INDEX_DELEGATE_STATE).query(qeRequest)
                .flatMapIterable(Page::items));
    }

    public Mono<Page<MandateEntity>> searchByDelegate(String delegateId,
                                                      @Nullable Integer status,
                                                      List<String> groups,
                                                      List<String> delegatorIds,
                                                      int size,
                                                      PnLastEvaluatedKey lastEvaluatedKey) {
        log.debug("searchByDelegate {}, status: {}, groups: {}, delegatorIds: {}, size: {}, lek: {}",
                delegateId, status, groups, delegatorIds, size, lastEvaluatedKey);

        Key.Builder keyBuilder = Key.builder().partitionValue(delegateId);
        if (status != null) {
            keyBuilder.sortValue(status);
        }
        Key key = keyBuilder.build();
        QueryConditional queryConditional = QueryConditional.keyEqualTo(key);
        log.debug("query conditional PK: {}, SK: {}", key.partitionKeyValue(), key.sortKeyValue());

        QueryEnhancedRequest queryEnhancedRequest = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .filterExpression(filterExpressionSearchByDelegate(groups, delegatorIds))
                .scanIndexForward(true)
                .limit(size)
                .exclusiveStartKey(lastEvaluatedKeySearchByDelegate(lastEvaluatedKey))
                .build();

        return Mono.from(mandateTable.index(GSI_INDEX_DELEGATE_STATE).query(queryEnhancedRequest));
    }

    private Expression filterExpressionSearchByDelegate(List<String> groups, List<String> delegatorIds) {
        Expression.Builder expressionBuilder = Expression.builder();
        StringBuilder filterExpression = new StringBuilder();
        String date = DateUtils.formatDate(ZonedDateTime.now().toInstant());
        addNewFilterExpression(filterExpression);
        addFilterExpression(List.of(date), MandateEntity.COL_D_VALIDTO, ":now",GT, expressionBuilder, filterExpression);

        if (!CollectionUtils.isEmpty(groups)) {
            addNewFilterExpression(filterExpression);
            addFilterExpression(groups, MandateEntity.COL_A_GROUPS, ":g", CONTAINS, expressionBuilder, filterExpression);
        }
        if (!CollectionUtils.isEmpty(delegatorIds)) {
            addNewFilterExpression(filterExpression);
            addFilterExpression(delegatorIds, MandateEntity.COL_PK, ":d", EQ, expressionBuilder, filterExpression);
        }

        addNewFilterExpression(filterExpression);
        List<String> workflowTypesToSegregate = TypeSegregatorFilter.STANDARD.getTypes().stream().map(Enum::name).toList();
        addWorkflowTypeStandardOrNotExistsFilter(workflowTypesToSegregate, expressionBuilder, filterExpression);

        if (!filterExpression.isEmpty()) {
            expressionBuilder.expression(filterExpression.toString());
        }
        Expression expression = expressionBuilder.build();
        log.debug("filterExpression: {}, values: {}", expression.expression(), expression.expressionValues());
        return expression;
    }

    private @Nullable Map<String, AttributeValue> lastEvaluatedKeySearchByDelegate(PnLastEvaluatedKey lastEvaluatedKey) {
        if (lastEvaluatedKey != null && !lastEvaluatedKey.getInternalLastEvaluatedKey().isEmpty()) {
            var map = new HashMap<>(lastEvaluatedKey.getInternalLastEvaluatedKey());
            map.computeIfPresent(MandateEntity.COL_I_STATE, (k, v) -> AttributeValue.builder()
                    .n(v.s() != null ? v.s() : v.n())
                    .build());
            log.debug("lastEvaluatedKey from: {}, to: {}", lastEvaluatedKey, map);
            return map;
        }
        return null;
    }

    private void addFilterExpression(List<String> values, String field, String prefix, String operator,
                                     Expression.Builder expressionBuilder, StringBuilder expression) {
        expression.append("(");
        for (int i = 0; i < values.size(); i++) {
            switch (operator) {
                case CONTAINS -> addContainsFilterExpression(field, prefix, i, expression);
                case EQ -> addEqFilterExpression(field, prefix, i, expression);
                case GT -> addGtFilterExpression(field, prefix, i, expression);
                default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
            }
            if (i < values.size() - 1) {
                expression.append(" OR ");
            }
            expressionBuilder.putExpressionValue(prefix + i, AttributeValue.builder().s(values.get(i)).build());
        }
        expression.append(")");
    }

    private void addGtFilterExpression(String field, String prefix, int i, StringBuilder expression) {
        expression.append(field).append(GT).append(prefix).append(i).append(" OR attribute_not_exists(").append(field).append(")");
    }

    private void addContainsFilterExpression(String field, String prefix, int idx, StringBuilder expression) {
        expression.append(CONTAINS).append("(").append(field).append(",").append(prefix).append(idx).append(")");
    }

    private void addEqFilterExpression(String field, String prefix, int idx, StringBuilder expression) {
        expression.append(field).append(EQ).append(prefix).append(idx);
    }

    private void addWorkflowTypeStandardOrNotExistsFilter(List<String> values,
                                                          Expression.Builder expressionBuilder, StringBuilder expression) {
        expression.append("(attribute_not_exists(").append(MandateEntity.COL_S_WORKFLOW_TYPE).append(") OR (");
        for (int i = 0; i < values.size(); i++) {
            expression.append(MandateEntity.COL_S_WORKFLOW_TYPE).append(EQ).append(":type").append(i);
            if (i < values.size() - 1) {
                expression.append(" OR ");
            }
            expressionBuilder.putExpressionValue(":type" + i, AttributeValue.builder().s(values.get(i)).build());
        }
        expression.append("))");
    }

    private void addNewFilterExpression(StringBuilder expression) {
        expression.append(!expression.isEmpty() ? AND : "");
    }

    /**
     * Ritorna la lista delle deleghe per delegante
     *
     * @param delegatorInternaluserid internaluserid del delegante
     * @param status                  stato da usare nel filtr (OPZIONALE)
     * @return lista delle deleghe
     */
    public Flux<MandateEntity> listMandatesByDelegator(String delegatorInternaluserid, Integer status, String mandateId, DelegateType delegateType) {
        // devo sempre filtrare. Se lo stato è passato, vuol dire che voglio filtrare solo per quello stato.
        // altrimenti, è IMPLICITO il fatto di filtrare per le deleghe pendenti e attive (ovvero < 20)
        // NB: listMandatesByDelegate e listMandatesByDelegator si assomigliano, ma a livello di query fanno
        // affidamento ad indici diversi e query diverse
        // listMandatesByDelegator si affida all'ordinamento principale, che filtra per utente e delega. Lo stato va previsto a parte nell'expressionfilter
        // Il delegateType è il tipo del delegato, utilizzato per trovare le deleghe dato il delegante restringendo la ricerca per tipo dei delegati.
        log.info("listMandatesByDelegator uid={} status={}", delegatorInternaluserid, status);

        int iState = StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE);
        String filterexp = getValidToFilterExpression() + " AND  " + getStatusFilterExpression(true);
        if (status != null) {
            filterexp = getValidToFilterExpression() + "  AND " + getStatusFilterExpression(false);   // si noti = status
            iState = status;
        }

        // devo sempre mettere un filtro di salvaguardia per quanto riguarda la scadenza della delega.
        // infatti se una delega è scaduta, potrebbe rimanere a sistema per qualche ora/giorno prima di essere svecchiata
        // e quindi va sempre previsto un filtro sulla data di scadenza
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":now", AttributeValue.builder().s(DateUtils.formatDate(ZonedDateTime.now().toInstant())).build());
        expressionValues.put(":status", AttributeValue.builder().n(iState + "").build());
        if (mandateId != null) {
            expressionValues.put(":mandateId", AttributeValue.builder().s(mandateId).build());
            filterexp += AND + getMandateFilterExpression();
        }

        if (delegateType != null) {
            filterexp += AND + getDelegateIsPersonExpression();
            expressionValues.put(":isPerson", AttributeValue.builder().bool(DelegateType.PF == delegateType).build());
        }

        TypeSegregatorFilter typeSegregatorFilter = TypeSegregatorFilter.STANDARD;
        String workflowTypeExpression = typeSegregatorFilter.buildExpression(expressionValues);
        if (!workflowTypeExpression.isEmpty()) {
            filterexp += AND + workflowTypeExpression;
        }

        Expression exp = Expression.builder()
                .expression(filterexp)
                .expressionValues(expressionValues)
                .build();


        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.sortBeginsWith(getKeyBuild(delegatorInternaluserid, MandateEntity.MANDATE_PREFIX)))
                .filterExpression(exp)
                .scanIndexForward(true)
                .build();

        return Flux.from(mandateTable.query(qeRequest).items());
    }

    /**
     * Ritorna la lista dei delegati
     * @param mandatesByDelegators lista dei deleganti
     * @return lista deleghe
     */
    public Flux<MandateEntity> listMandatesByDelegators(List<MandateByDelegatorRequestDto> mandatesByDelegators) {
        return Flux.fromIterable(mandatesByDelegators)
                .map(dto -> new MandateEntity(dto.getDelegatorId(), dto.getMandateId()))
                .collectList()
                .flatMapMany(this::batchGetMandate);
    }

    /**
     * Il metodo si occupa di:
     * - leggere l'item dal GSI delegato
     * - validare la richiesta
     * - aggiornarne il contenuto (stato e data accettazione) nell'entity
     * - creare un NUOVO record di supporto, con TTL pari a scadenza della delega (se è prevista). Questo record, quando scadrà, darà luogo ad un loopback
     * che mi permetterà di spostare il record principale nello storico. Il TTL NON viene messo nel record principale perchè se qualcosa va storto almeno
     * il record principale rimane (scaduto) e non viene perso.
     * - update dell'entity aggiornata in DB
     *
     * @param delegateInternaluserid internaluserid del delegato
     * @param mandateId              id della delega
     * @param verificationCode       codice di verifica della relativo all'accettazione
     * @return void
     */
    public Mono<MandateEntity> acceptMandate(String delegateInternaluserid,
                                             String mandateId,
                                             String verificationCode,
                                             List<String> groups,
                                             CxTypeAuthFleet cxTypeAuthFleet) {
        String logMessage = String.format("acceptMandate for delegate uid=%s mandateid=%s verificationCode=%s", delegateInternaluserid, mandateId, verificationCode);
        PnAuditLogEvent logEvent = new PnAuditLogBuilder()
                .before(PnAuditLogEventType.AUD_DL_ACCEPT, logMessage)
                .mdcEntry(MDC_PN_MANDATEID_KEY, mandateId)
                .build();

        logEvent.log();
        return retrieveMandateForDelegate(delegateInternaluserid, mandateId)
                .switchIfEmpty(Mono.error(new PnMandateNotFoundException()))
                .flatMap(mandate -> {
                    checkMandateSegregation(mandate, TypeSegregatorFilter.STANDARD);
                    editMandateState(mandate, verificationCode, cxTypeAuthFleet, groups);
                    return save(mandate);
                })
                .doOnSuccess(mandate -> {
                    String messageAction = String.format(
                            "mandate accepted delegator uid=%s delegate uid=%s mandateobj=%S",
                            mandate.getDelegator(), mandate.getDelegate(), mandate);
                    logEvent.generateSuccess(messageAction).log();
                })
                .onErrorResume(throwable -> {
                    if (throwable instanceof PnInvalidVerificationCodeException)
                        logEvent.generateSuccess("FAILURE {}", throwable.getMessage()).log();
                    else
                        logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable);
                });
    }

    public Mono<Tuple2<MandateEntity, MandateEntity>> updateMandate(String delegateId, String mandateId, Set<String> groups) {
        String logMessage = String.format("updateMandate for delegate uid=%s mandateId=%s", delegateId, mandateId);
        PnAuditLogEvent logEvent = new PnAuditLogBuilder()
                .before(PnAuditLogEventType.AUD_DL_UPDATE, logMessage)
                .mdcEntry(MDC_PN_MANDATEID_KEY, mandateId)
                .build();
        logEvent.log();
        return retrieveMandateForDelegate(delegateId, mandateId)
                .switchIfEmpty(Mono.error(new PnMandateNotFoundException()))
                .flatMap(mandate -> {
                    checkMandateSegregation(mandate, TypeSegregatorFilter.STANDARD);
                    if (StatusEnumMapper.fromValue(mandate.getState()) != StatusEnum.ACTIVE) {
                        log.warn("mandate is not ACTIVE, throw error");
                        return Mono.error(new PnInvalidMandateStatusException("update an inactive mandate is not permitted", "Invalid mandate status", 500, PnMandateExceptionCodes.ERROR_CODE_MANDATE_NOTUPDATABLE, "update an inactive mandate is not permitted"));
                    } else if (mandate.getValidto() != null && mandate.getValidto().isBefore(Instant.now())) {
                        log.warn("mandate is not ACTIVE, throw error");
                        return Mono.error(new PnMandateNotFoundException());
                    }
                    MandateEntity oldCopyOfMandate = new MandateEntity(mandate);
                    mandate.setGroups(groups);
                    return save(mandate)
                            .map(newCopyOfMandate -> Tuples.of(oldCopyOfMandate, newCopyOfMandate));
                })
                .doOnSuccess(t -> {
                    String msg = String.format("mandate updated delegator uid=%s delegate uid=%s mandateObj=%S", t.getT2().getDelegator(), t.getT2().getDelegate(), t.getT2());
                    logEvent.generateSuccess(msg).log();
                })
                .onErrorResume(throwable -> {
                    logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable);
                });
    }

    private void editMandateState(MandateEntity mandate,
                                  String verificationCode,
                                  CxTypeAuthFleet cxTypeAuthFleet,
                                  List<String> groups) {
        if (!mandate.getValidationcode().equals(verificationCode)) {
            throw new PnInvalidVerificationCodeException();
        }
        if (CxTypeAuthFleet.PG == cxTypeAuthFleet && !CollectionUtils.isEmpty(groups)) {
            mandate.setGroups(Set.copyOf(groups));
        }
        log.info("retrieved mandateobj={}", mandate);
        if (mandate.getState() == StatusEnumMapper.intValfromStatus(StatusEnum.PENDING)) {

            Instant pendingExpiredInstant = mandate.getCreated().plus(this.pendingExpire);
            if (pendingExpiredInstant.isBefore(Instant.now()))
            {
                // caso raro, in cui l'utente sta accettando una delega subito dopo la scadenza dello stato di pending permesso
                log.warn("mandate is PENDING but already expired and marked for deletion");
                throw new PnMandatePendingExpiredException();
            }

            // aggiorno lo stato, solo se era in pending. NB: non do errore
            mandate.setAccepted(Instant.now());
            mandate.setState(StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE));
        } else if (mandate.getState() == StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE))
            log.info("mandate is already ACTIVE accepting silently");
        else {
            // non dovrebbe veramente succedere, perchè vuol dire che è rimasta una delega scaduta e che qualcuno ci ha pure chiesto l'accettazione, cmq tiro eccezione
            log.warn("mandate is not PENDING or ACTIVE, throw error");
            throw new PnInternalException("accept a expired mandate is not permitted", PnMandateExceptionCodes.ERROR_CODE_MANDATE_NOTACCEPTABLE);
        }
    }

    public Mono<MandateEntity> save(MandateEntity mandate) {
        // Se la delega prevede una scadenza impostata dall'utente, creo un record di supporto con TTL
        // e quando questo verrà cancellato, dynamoDB invocherà la nostra logica che andrà a spostare il record principale nello storico.
        // Questo perchè se mettevo il TTL nel record principale e per qualche anomalia non veniva gestito l'evento di cancellazione
        // avrei perso definitivamente il record della delega (scaduta, ma che va mantenuta per 10 anni nello storico)
        TransactWriteItemsEnhancedRequest.Builder transactionBuilder = TransactWriteItemsEnhancedRequest.builder();
        if (mandate.getValidto() != null) {
            MandateSupportEntity support = new MandateSupportEntity(mandate);
            transactionBuilder.addUpdateItem(mandateSupportTable, TransactUpdateItemEnhancedRequest.builder(MandateSupportEntity.class).item(support).build());
            log.info("mandate has validto setted, creating also support entity for ttl expiration");
        }

        // aggiungo l'update delle deleghe e lancio la transazione
        TransactWriteItemsEnhancedRequest transaction = transactionBuilder
                .addUpdateItem(mandateTable, TransactUpdateItemEnhancedRequest.builder(MandateEntity.class).item(mandate).ignoreNulls(false).build())
                .build();
        return Mono.fromFuture(dynamoDbEnhancedAsyncClient.transactWriteItems(transaction)).thenReturn(mandate);
    }
    private Duration chooseExpiredConfig(WorkFlowType workFlowType)
    {
        if(workFlowType == null || workFlowType == WorkFlowType.STANDARD || workFlowType == WorkFlowType.REVERSE)
            return pendingExpire;
        else
            return ciePendingExpire;
    }

    private CompletableFuture<Void> savePendingWithExpireSupport(MandateEntity mandate) {
        // Prevedo un record di supporto con TTL con impostata la scadenza relativa all'accettazione della delega
        TransactWriteItemsEnhancedRequest.Builder transactionBuilder = TransactWriteItemsEnhancedRequest.builder();

        if (mandate.getCreated() == null) {
            mandate.setCreated(Instant.now());
        }
        Instant created = mandate.getCreated() != null ? mandate.getCreated() : Instant.now();
        Instant pendingExpiredInstant = created.plus(this.chooseExpiredConfig(mandate.getWorkflowType()));

        MandateSupportEntity support;
        if (mandate.getValidto() != null && pendingExpiredInstant.isBefore(mandate.getValidto())){
            support = new MandateSupportEntity(mandate, pendingExpiredInstant);
        } else {
            support = new MandateSupportEntity(mandate, mandate.getValidto() != null ?mandate.getValidto() :  pendingExpiredInstant);
        }
        var request = TransactPutItemEnhancedRequest.builder(MandateSupportEntity.class).item(support).build();
        transactionBuilder.addPutItem(mandateSupportTable, request);
        log.info("creating also support entity for pending ttl expiration pendingExpiredInstant={}", pendingExpiredInstant);

        // aggiungo l'update delle deleghe e lancio la transazione
        TransactWriteItemsEnhancedRequest transaction = transactionBuilder
                .addUpdateItem(mandateTable, TransactUpdateItemEnhancedRequest.builder(MandateEntity.class).item(mandate).ignoreNulls(false).build())
                .build();
        return dynamoDbEnhancedAsyncClient.transactWriteItems(transaction);
    }

    /**
     * Il metodo si occupa di:
     * - leggere l'item dal GSI delegato
     * - aggiornarne il contenuto (stato e data rifiuto) nell'entity
     * - creare una copia dell'entity nella tabella dello storico, impostandone il TTL a 10 anni
     * - eliminare l'entity dalla tabella principale
     *
     * @param delegateInternaluserid internaluserid del delegato
     * @param mandateId              id della delega
     * @return void
     */
    public Mono<MandateEntity> rejectMandate(final String delegateInternaluserid, final String mandateId) {
        // qui l'internaluserid è quello del DELEGATO, e quindi NON posso usare direttamente l'informazione per accedere al record.
        // devo passare quindi per l'indice sul delegato, recuperarmi la riga, aggiornare il contenuto e salvarla.
        // NB: si noti che ci può essere un problema legato alla concorrenza, ma dato che i dati sulle deleghe non cambiano così frequentemente, 
        // si accetta la possibilità dell'improbabilità che arrivino 2 scritture contemporanee nel piccolo lasso di tempo tra query e update...

        String logMessage = String.format("rejectMandate for delegate uid=%s mandateid=%s", delegateInternaluserid, mandateId);
        PnAuditLogEvent logEvent = new PnAuditLogBuilder()
                .before(PnAuditLogEventType.AUD_DL_REJECT, logMessage)
                .mdcEntry(MDC_PN_MANDATEID_KEY, mandateId)
                .build();
        logEvent.log();

        return retrieveMandateForDelegate(delegateInternaluserid, mandateId)
                .switchIfEmpty(Mono.error(new PnMandateNotFoundException()))
                .flatMap(mandate -> {
                    log.info("rejectMandate mandate for delegate retrieved mandateobj={}", mandate);
                    checkMandateSegregation(mandate, TypeSegregatorFilter.STANDARD);
                    if (mandate.getState() == StatusEnumMapper.intValfromStatus(StatusEnum.PENDING)
                            || mandate.getState() == StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE)) {
                        // aggiorno lo stato, solo se era in pending o active, ignoro eventuali altri stati (che NON dovrebbero essere presenti)
                        mandate.setRejected(Instant.now());
                        mandate.setState(StatusEnumMapper.intValfromStatus(StatusEnum.REJECTED));

                        return Mono.fromFuture(saveHistoryAndDeleteFromMain(mandate))
                                .map(m -> {
                                    logEvent.generateSuccess("mandate rejected mandate={}", m).log();
                                    return m;
                                });
                    } else
                        log.warn("no mandate found in pending,active or rejected state, fail silently");

                    return Mono.empty();
                })
                .onErrorResume(throwable -> {
                    logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable);
                });

    }

    /**
     * Il metodo si occupa di:
     * - leggere l'item dalla tabella principale
     * - aggiornarne il contenuto (stato e data revoca) nell'entity
     * - creare una copia dell'entity nella tabella dello storico, impostandone il TTL a 10 anni
     * - eliminare l'entity dalla tabella principale
     * - eliminare eventuale entity di supporto dalla tabella principale
     *
     * @param delegatorInternaluserid internaluserid del delegante
     * @param mandateId               id della delega
     * @param allowedSegregator       segregatore (utilizzato per filtrare la tipologia di delega sulla quale è permessa l'operazione)
     * @return void
     */
    public Mono<MandateEntity> revokeMandate(
            String delegatorInternaluserid,
            String mandateId,
            TypeSegregatorFilter allowedSegregator,
            RevocationCause cause
    ) {
        String logMessage = String.format("revokeMandate %s for delegate uid=%s mandateid=%s", cause.getLogSuffix(), delegatorInternaluserid, mandateId);
        PnAuditLogEvent logEvent = new PnAuditLogBuilder()
                .before(PnAuditLogEventType.AUD_DL_REVOKE, logMessage)
                .mdcEntry(MDC_PN_MANDATEID_KEY, mandateId)
                .build();

        logEvent.log();
        return Mono.fromFuture(retrieveMandateForDelegator(delegatorInternaluserid, mandateId)
                .thenCompose(mandate -> {
                            if (mandate == null)
                            {
                                log.error("mandate not found");
                                logEvent.generateFailure(String.format("revokeMandate skipped, mandate not found mandateid=%s", mandateId)).log();
                                return CompletableFuture.completedFuture(mandate);
                            }
                            checkMandateSegregation(mandate, allowedSegregator);
                            log.info("revokeMandate mandate for delegate retrieved mandateobj={}", mandate);
                            if (mandate.getState() == StatusEnumMapper.intValfromStatus(StatusEnum.PENDING)
                                    || mandate.getState() == StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE)) {
                                // aggiorno lo stato, solo se era in pending o active, ignoro eventuali altri stati (che NON dovrebbero essere presenti)
                                mandate.setRevoked(Instant.now());
                                mandate.setState(StatusEnumMapper.intValfromStatus(StatusEnum.REVOKED));
                                return saveHistoryAndDeleteFromMain(mandate);
                            }
                            return CompletableFuture.completedFuture(mandate);
                        })
                )
                .onErrorResume(throwable -> {
                    logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable);
                })
                .map(m -> {
                    logEvent.generateSuccess("mandate revoked mandate={}", m).log();
                    return m;
                });
    }

    /**
     * Il metodo si occupa di:
     * - leggere l'item dalla tabella principale
     * - aggiornarne il contenuto (stato) nell'entity
     * - creare una copia dell'entity nella tabella dello storico, impostandone il TTL a 10 anni
     * - eliminare l'entity dalla tabella principale
     * - eliminare eventuale entity di supporto dalla tabella principale
     *
     * @param delegatorInternaluserid internaluserid del delegante
     * @param delegatorUid            uid del delegante
     * @param cxType                  cxType del delegante
     * @param mandateId               id della delega
     * @return void
     */
    public Mono<MandateEntity> expireMandate(String delegatorInternaluserid, String delegatorUid, String cxType, String mandateId) {
        String logMessage = String.format("expireMandate for delegate internalId=%s uid=%s cxType=%s mandateid=%s", delegatorInternaluserid, delegatorUid, cxType, mandateId);
        PnAuditLogEvent logEvent = new PnAuditLogBuilder()
                .before(PnAuditLogEventType.AUD_DL_EXPIRE, logMessage)
                .mdcEntry(MDC_CX_ID_KEY, delegatorInternaluserid)
                .mdcEntry(MDC_PN_UID_KEY, delegatorUid)
                .mdcEntry(MDC_PN_CX_TYPE_KEY, cxType)
                .mdcEntry(MDC_PN_MANDATEID_KEY, mandateId)
                .build();

        logEvent.log();
        return Mono.fromFuture(retrieveMandateForDelegator(delegatorInternaluserid, mandateId)
                        .thenCompose(mandate -> {
                            if (mandate == null) {
                                throw new PnMandateNotFoundException();
                            }
                            log.info("expireMandate mandate for delegate retrieved mandateobj={}", mandate);
                            boolean skipHistory = false;
                            // se lo stato era active, lo porto ad expired
                            // ora invece potrà succedere che arrivi l'expired di deleghe in pending
                            if (mandate.getState() == StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE)) {
                                Instant validTo = mandate.getValidto() != null ? mandate.getValidto() :mandate.getCreated().plus(this.pendingExpire);
                                if (Instant.now().isAfter(validTo)){
                                    mandate.setState(StatusEnumMapper.intValfromStatus(StatusEnum.EXPIRED));
                                } else {
                                    //Se nel frattempo la delega è stata accettata ignoro l'expire date del pending
                                    log.warn("delegate is accepted in meanwhile {}", mandate);
                                    skipHistory = true;
                                }
                            }

                            return skipHistory ? CompletableFuture.completedFuture(mandate) : saveHistoryAndDeleteFromMain(mandate);
                        }))
                .onErrorResume(throwable -> {
                    logEvent.generateFailure(throwable.getMessage()).log();
                    return Mono.error(throwable);
                })
                .map(m -> {
                    logEvent.generateSuccess("mandate expired mandate={}", m).log();
                    return m;
                });
    }

    /**
     * Il metodo si occupa di:
     * - controllare che non siano presenti deleghe per la coppia delegante-delegato
     * - creare la delega nella tabella mandate
     *
     * @param mandate oggetto delega da creare
     * @return oggetto delega creato
     */
    public Mono<MandateEntity> createMandate(MandateEntity mandate) {
        return createMandate(mandate,TypeSegregatorFilter.STANDARD);
    }

    public Mono<MandateEntity> createMandate(MandateEntity mandate, TypeSegregatorFilter typeSegregatorFilter)
    {
        String logMessage = String.format("create mandate mandate=%s", mandate);
        String delegator = mandate.getDelegator();
        String iuns = MandateUtils.joinCollectionToString(",", mandate.getIuns());

        return Mono.fromFuture(countMandateForDelegateAndDelegator(mandate.getDelegator(), mandate.getDelegate(),typeSegregatorFilter,mandate.getIuns())
                        .thenCompose(total -> {
                            if (total == 0 || isSelfPgMandate(mandate)) {
                                log.info("no current mandate for delegator-delegate pair, can proceed to create mandate");
                                PnAuditLogEvent logEvent = new PnAuditLogBuilder()
                                        .before(PnAuditLogEventType.AUD_DL_CREATE, logMessage)
                                        .mdcEntry(MDC_PN_MANDATEID_KEY, mandate.getMandateId())
                                        .mdcEntry(MDC_PN_IUN_KEY, iuns)
                                        .mdcEntry(MDC_PN_DELEGATOR_ID_KEY, delegator)
                                        .mdcEntry(MDC_PN_MANDATE_WORKFLOW_TYPE_KEY, mandate.getWorkflowType().name())
                                        .build();

                                logEvent.log();


                                return this.savePendingWithExpireSupport(mandate)
                                        .exceptionally(throwable -> {
                                            logEvent.generateFailure(throwable.getMessage()).log();
                                            if (throwable instanceof RuntimeException runtimeException) {
                                                throw runtimeException;
                                            }
                                            if (throwable instanceof Error error) throw error;
                                            throw new AssertionError(throwable);
                                        })
                                        .thenApply(x -> {
                                            log.info("saved mandate mandateobj={}", mandate);
                                            logEvent.generateSuccess(String.format("created mandate mandateobj=%s", mandate)).log();
                                            return mandate;
                                        });
                            } else {
                                throw new PnMandateAlreadyExistsException();
                            }
                        }))
                .onErrorResume(Mono::error)
                .map(mandateCreated -> {
                    log.info("created mandate mandateobj={}", mandateCreated);


                    return mandateCreated;
                });
    }

    private boolean isSelfPgMandate(MandateEntity mandate) {
        return Boolean.FALSE.equals(mandate.getDelegatorisperson()) && mandate.getDelegator().equals(mandate.getDelegate());
    }

    //#endregion

    //#region private methods

    /**
     * Recupera una delega in base all'internaluserid del delegante e all'id della delega
     *
     * @param delegatorInternaluserid internaluserid del delegante
     * @param mandateId               id della delega
     * @return future contenente la delega
     */
    private CompletableFuture<MandateEntity> retrieveMandateForDelegator(String delegatorInternaluserid, String mandateId) {
        MandateEntity mandate = new MandateEntity(delegatorInternaluserid, mandateId);
        // qui l'internaluserid è quello del DELEGANTE, e quindi posso usare direttamente l'informazione per accedere al record
        GetItemEnhancedRequest getitemRequest = GetItemEnhancedRequest.builder()
                .key(getKeyBuild(mandate.getDelegator(), mandate.getSk()))
                .build();


        return mandateTable.getItem(getitemRequest);
    }

    /**
     * Recupera una delega in base all'internaluserid del delegato e all'id della delega
     *
     * @param delegateInternaluserid internaluserid del delegato
     * @param mandateId              id della delega
     * @return publisher contenente un solo record di delega
     */
    public Mono<MandateEntity> retrieveMandateForDelegate(String delegateInternaluserid, String mandateId) {
        // qui l'internaluserid è quello del DELEGATO, e quindi NON posso usare direttamente l'informazione per accedere al record.
        // devo passare quindi per l'indice sul delegato, recuperarmi la riga, aggiornare il contenuto e salvarla.
        // NB: si noti che ci può essere un problema legato alla concorrenza, ma dato che i dati sulle deleghe non cambiano così frequentemente, 
        // si accetta la possibilità dell'improbabilità che arrivino 2 scritture contemporanee nel piccolo lasso di tempo tra query e update...


        // uso l'expression filter per filtrare per mandateid, dato che su questo indice non fa parte della key.
        // si accetta il costo di leggere tutte le righe per un delegato per poi tornarne una
        // non viene filtrato lo stato, dato che questo metodo può essere usato per motivi generici
        MandateEntity mandate = new MandateEntity(delegateInternaluserid, mandateId);
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":mandateid", AttributeValue.builder().s(mandate.getSk()).build());

        Expression exp = Expression.builder()
                .expression(MandateEntity.COL_SK + " = :mandateid")
                .expressionValues(expressionValues)
                .build();

        QueryEnhancedRequest qeRequest = QueryEnhancedRequest
                .builder()
                .queryConditional(QueryConditional.keyEqualTo(getKeyBuild(delegateInternaluserid)))
                .filterExpression(exp)
                .scanIndexForward(true)
                .build();

        return Flux.from(mandateTable.index(GSI_INDEX_DELEGATE_STATE).query(qeRequest)
                .flatMapIterable(Page::items).limit(1)).take(1).next();
    }

    /**
     * Recupera il numero di deleghe presenti per la coppia delegante-delegato
     *
     * @param delegatorInternaluserid internaluserid del delegante
     * @param delegateInternaluserid  internaluserid del delegato
     * @return future contenente il conteggio delle deleghe
     */
    private CompletableFuture<Integer> countMandateForDelegateAndDelegator(String delegatorInternaluserid, String delegateInternaluserid, TypeSegregatorFilter typeSegregatorFilter, Set<String> iuns) {
        // qui ho entrambi gli id, mi serve sapere se ho già una delega per la coppia delegante-delegato, in pending/attiva e non scaduta ovviamente.

        // uso l'expression filter per filtrare le deleghe valide per il delegato
        // si accetta il costo di leggere più righe per niente
        Map<String, AttributeValue> expressionValues = new HashMap<>();
        expressionValues.put(":delegate", AttributeValue.builder().s(delegateInternaluserid).build());
        expressionValues.put(":delegator", AttributeValue.builder().s(delegatorInternaluserid).build());
        expressionValues.put(":mandateprefix", AttributeValue.builder().s(MandateEntity.MANDATE_PREFIX).build());
        if (typeSegregatorFilter == TypeSegregatorFilter.CIE)
            expressionValues.put(":now", AttributeValue.builder().s(ZonedDateTime.now().toInstant().toString()).build());
        else
            expressionValues.put(":now", AttributeValue.builder().s(DateUtils.formatDate(ZonedDateTime.now().toInstant())).build());

        expressionValues.put(":status", AttributeValue.builder().n(StatusEnumMapper.intValfromStatus(StatusEnum.ACTIVE) + "").build());

        String workFlowTypeExpression = typeSegregatorFilter.buildExpression(expressionValues);

        QueryRequest qeRequest = QueryRequest
                .builder()
                .select(Select.COUNT)
                .tableName(table)
                .filterExpression(getValidToFilterExpression() + AND + getStatusFilterExpression(true) + " AND (" + MandateEntity.COL_S_DELEGATE + " = :delegate)" + AND + workFlowTypeExpression + getIunsFilterExpression(iuns,expressionValues))
                .keyConditionExpression(MandateEntity.COL_PK + " = :delegator AND begins_with(" + MandateEntity.COL_SK + ", :mandateprefix)")
                .expressionAttributeValues(expressionValues)
                .build();

        return dynamoDbAsyncClient.query(qeRequest).thenApply(QueryResponse::count);
    }


    private CompletableFuture<MandateEntity> saveHistoryAndDeleteFromMain(MandateEntity mandate) {
        // aggiorno il TTL
        mandate.setTtl(LocalDateTime.now().plusYears(10).atZone(ZoneId.systemDefault()).toEpochSecond());

        MandateSupportEntity mandateSupport = new MandateSupportEntity(mandate);

        TransactWriteItemsEnhancedRequest transaction = TransactWriteItemsEnhancedRequest.builder()
                .addPutItem(mandateHistoryTable, TransactPutItemEnhancedRequest.builder(MandateEntity.class).item(mandate).build())
                .addDeleteItem(mandateTable, TransactDeleteItemEnhancedRequest.builder().key(getKeyBuild(mandate.getDelegator(), mandate.getSk())).build())
                .addDeleteItem(mandateSupportTable, TransactDeleteItemEnhancedRequest.builder().key(getKeyBuild(mandateSupport.getDelegator(), mandateSupport.getSk())).build())
                .build();

        return dynamoDbEnhancedAsyncClient.transactWriteItems(transaction).thenApply(x -> {
            if (log.isInfoEnabled())
                log.info("mandate saved in history and deleted from main table mandateobj={}", mandate);

            return mandate;
        });
    }

    private String getValidToFilterExpression() {
        return "(" + MandateEntity.COL_D_VALIDTO + " > :now OR attribute_not_exists(" + MandateEntity.COL_D_VALIDTO + ")) ";
    }

    private String getIunsFilterExpression(Set<String> iuns, Map<String, AttributeValue> expressionValues) {
        if (iuns != null && !iuns.isEmpty()) {
            StringBuilder sb = new StringBuilder(AND + " (");
            int idx = 0;
            for (String iun : iuns) {
                if (idx > 0) {
                    sb.append(" OR ");
                }
                String placeholder = ":iun" + idx;
                sb.append("contains(")
                        .append(MandateEntity.COL_A_IUNS)
                        .append(", ").append(placeholder).append(")");
                expressionValues.put(placeholder, AttributeValue.builder().s(iun).build());
                idx++;
            }
            sb.append(")");
            return sb.toString();
        }
        return "";
    }


    private String getStatusFilterExpression(boolean lessEqualThan) {
        return " (" + MandateEntity.COL_I_STATE + " " + (lessEqualThan ? "<=" : "=") + " :status) ";
    }

    private String getMandateFilterExpression() {
        return "(" + MandateEntity.COL_S_MANDATEID + " = :mandateId) ";
    }

    private String getDelegateIsPersonExpression() {
        return "(" + MandateEntity.COL_B_DELEGATEISPERSON + " = :isPerson) ";
    }

    private String getIunFilterExpression(String iun, Map<String, AttributeValue> expressionValues) {
        /*
         Lo iun è indicato solo per le deleghe di tipo CIE, però non vogliamo escludere con questo filtro eventuali deleghe
         standard, la cui validità per accedere alla risorsa (notifica, documento) dipende da altri filtri.
         */
        String expressionForStandardSegregator = TypeSegregatorFilter.STANDARD.buildExpression(expressionValues);
        expressionValues.put(":cie", AttributeValue.builder().s(WorkFlowType.CIE.name()).build());
        expressionValues.put(":iun", AttributeValue.builder().s(iun).build());
        String expressionForCieWithIun = "(" + MandateEntity.COL_S_WORKFLOW_TYPE + " = :cie AND contains (" + MandateEntity.COL_A_IUNS + ", :iun))";
        return "(" + expressionForStandardSegregator + " OR " + expressionForCieWithIun + ")";
    }

    private String getNotificationSentAtFilterExpression(Instant notificationSentAt, Map<String, AttributeValue> expressionValues) {
        expressionValues.put(":notificationSentAt", AttributeValue.builder().s(notificationSentAt.toString()).build());
        return "(" + MandateEntity.COL_D_VALIDFROM + " <= :notificationSentAt) ";
    }

    private String getSenderIdFilterExpression(String senderId, Map<String, AttributeValue> expressionValues) {
        expressionValues.put(":senderId", AttributeValue.builder().s(senderId).build());
        return "(attribute_not_exists("+ MandateEntity.COL_A_VISIBILITYIDS+ ") OR contains(" + MandateEntity.COL_A_VISIBILITYIDS + ", :senderId))";
    }

    private Flux<MandateEntity> batchGetMandate(List<MandateEntity> entities) {
        return Flux.fromIterable(entities)
                .window(MAX_DYNAMODB_BATCH_SIZE)
                .flatMap(chunk -> {
                    ReadBatch.Builder<MandateEntity> builder = ReadBatch.builder(MandateEntity.class)
                            .mappedTableResource(mandateTable);
                    Mono<BatchGetResultPage> deferred = Mono.defer(() ->
                            Mono.from(dynamoDbEnhancedAsyncClient.batchGetItem(BatchGetItemEnhancedRequest.builder()
                                    .readBatches(builder.build())
                                    .build())));
                    return chunk
                            .doOnNext(item -> {
                                Key key = Key.builder().partitionValue(item.getDelegator()).sortValue(item.getSk()).build();
                                builder.addGetItem(key);
                            })
                            .then(deferred);
                })
                .flatMap(page -> {
                    List<MandateEntity> results = page.resultsForTable(mandateTable);
                    log.debug("request size: {}, query result size: {}", entities.size(), results.size());
                    if (!page.unprocessedKeysForTable(mandateTable).isEmpty()) {
                        List<Key> unprocessedKeys = page.unprocessedKeysForTable(mandateTable);
                        List<MandateEntity> unprocessedEntities = filterMandateAlreadyProcessed(entities, unprocessedKeys);
                        log.info("unprocessed entities {} over total entities {}", unprocessedEntities.size(), entities.size());
                        return Flux.fromIterable(results)
                                .concatWith(batchGetMandate(unprocessedEntities));
                    }
                    return Flux.fromIterable(results);
                });
    }

    private List<MandateEntity> filterMandateAlreadyProcessed(List<MandateEntity> entities, List<Key> unprocessedKeys) {
        Set<Key> setKeys = new HashSet<>(unprocessedKeys);
        return entities.stream()
                .filter(entity -> {
                    Key key = Key.builder().partitionValue(entity.getDelegator()).sortValue(entity.getSk()).build();
                    return setKeys.contains(key);
                })
                .toList();
    }
    //#endregion

    private void checkMandateSegregation(MandateEntity mandate, TypeSegregatorFilter allowedSegregator) {
        if (!allowedSegregator.isIncluded(mandate.getWorkflowType())) {
            log.warn("mandate with workflowType {} does not respect {} segregation, throw error", mandate.getWorkflowType(), allowedSegregator.name());
            throw new PnMandateBadRequestException();
        }
    }
}
