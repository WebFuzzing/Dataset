package it.pagopa.pn.papertracker.middleware.dao.dynamo;

import com.sngular.apigenerator.asyncapi.business_model.model.event.Data;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerConflictException;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerNotFoundException;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static it.pagopa.pn.commons.abstractions.impl.AbstractDynamoKeyValueStore.ATTRIBUTE_NOT_EXISTS;
import static it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings.ATTEMPT_ID_PCRETRY_INDEX;

@Component
@Slf4j
public class PaperTrackingsDAOImpl extends BaseDao<PaperTrackings> implements PaperTrackingsDAO {

    private final String ERROR_CODE_PAPER_TRACKER_DUPLICATED_ITEM = "PN_PAPER_TRACKER_DUPLICATED_ITEM";
    private final String ERROR_CODE_PAPER_TRACKER_NOT_FOUND = "PN_PAPER_TRACKER_NOT_FOUND";

    private final PnPaperTrackerConfigs pnPaperTrackerConfigs;

    public PaperTrackingsDAOImpl(DynamoDbEnhancedAsyncClient dynamoDbEnhancedClient, PnPaperTrackerConfigs cfg, DynamoDbAsyncClient dynamoDbAsyncClient) {
        super(dynamoDbEnhancedClient,
                dynamoDbAsyncClient,
                cfg.getDao().getPaperTrackingsTable(),
                PaperTrackings.class
        );
        this.pnPaperTrackerConfigs = cfg;
    }

    @Override
    public Mono<PaperTrackings> retrieveEntityByTrackingId(String trackingId) {
        return getByKey(Key.builder().partitionValue(trackingId).build());
    }

    @Override
    public Flux<PaperTrackings> retrieveEntityByAttemptId(String attemptId, String pcRetry) {
        Key.Builder key = Key.builder().partitionValue(attemptId);

        if(StringUtils.hasText(pcRetry)){
            key.sortValue(pcRetry);
        }

        return queryByIndex(key.build(), ATTEMPT_ID_PCRETRY_INDEX);
    }

    @Override
    public Mono<PaperTrackings> putIfAbsent(PaperTrackings entity) {
        String expression = String.format("%s(%s)", ATTRIBUTE_NOT_EXISTS, PaperTrackings.COL_TRACKING_ID);
        entity.setTtl(Instant.now().plus(pnPaperTrackerConfigs.getPaperTrackingsTtlDuration()).getEpochSecond());

        return putIfAbsent(expression, entity)
                .onErrorMap(ConditionalCheckFailedException.class, ex -> {
                    log.error("Conditional check exception on PaperTrackingsDAOImpl putTrackings trackingId={} exmessage={}", entity.getTrackingId(), ex.getMessage());
                    log.warn("Duplicate trackingId detected in putIfAbsent: {}", entity.getTrackingId());
                    return new PnPaperTrackerConflictException(ERROR_CODE_PAPER_TRACKER_DUPLICATED_ITEM, String.format("RequestId %s already exists", entity.getTrackingId()));
                });
    }

    public Mono<PaperTrackings> updateOcrRequests(Integer ocrRequestIndex, String trackingId, Data.ValidationStatus validationStatus) {
        log.debug("updateOcrRequests for item with trackingId: {}", trackingId);
        Instant now = Instant.now();
        Map<String, String> expressionAttributeNames = new HashMap<>();
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();

        String updateExpr = "SET #updatedAt = :updatedAt";

        expressionAttributeNames.put("#updatedAt", PaperTrackings.COL_UPDATED_AT);
        expressionAttributeNames.put("#validationFlow", PaperTrackings.COL_VALIDATION_FLOW);
        expressionAttributeNames.put("#ocr", ValidationFlow.COL_OCR_REQUESTS);

        expressionAttributeValues.put(":updatedAt", AttributeValue.builder().s(now.toString()).build());

        if (Objects.nonNull(ocrRequestIndex) && ocrRequestIndex >= 0) {
            expressionAttributeNames.put("#responseTimestamp", OcrRequest.COL_RESPONSE_TIMESTAMP);
            expressionAttributeValues.put(":responseTimestamp", AttributeValue.builder().s(now.toString()).build());
            expressionAttributeNames.put("#responseStatus", OcrRequest.COL_RESPONSE_STATUS);
            expressionAttributeValues.put(":responseStatus", AttributeValue.builder().s(validationStatus.getValue()).build());
            updateExpr += ", #validationFlow.#ocr[" + ocrRequestIndex + "].#responseStatus = :responseStatus, " +
                    "#validationFlow.#ocr[" + ocrRequestIndex + "].#responseTimestamp = :responseTimestamp";
        }

        String conditionExpression = String.format("%s(%s)", "attribute_exists", PaperTrackings.COL_TRACKING_ID);
        return updateIfExists(trackingId, updateExpr, expressionAttributeValues, expressionAttributeNames, conditionExpression);

    }

    @Override
    public Mono<PaperTrackings> updateItem(String trackingId, PaperTrackings paperTrackings) {
        return updateItem(trackingId, paperTrackings, false);
    }

    @Override
    public Mono<PaperTrackings> updateItemForRework(String trackingId, PaperTrackings paperTrackings) {
        return updateItem(trackingId, paperTrackings, true);
    }

    /**
     * Aggiorna un elemento PaperTrackings nel database DynamoDB, identificato dal trackingId.
     * L'aggiornamento viene eseguito solo se l'elemento esiste (condizione attribute_exists).
     * Viene aggiornato anche il campo "updatedAt" con il timestamp corrente.
     *
     * @param trackingId pk dell'oggetto PaperTrackings da aggiornare
     * @param paperTrackings l'oggetto PaperTrackings con i nuovi valori da aggiornare
     * @return un Mono contenente l'oggetto PaperTrackings aggiornato
     * @throws PnPaperTrackerNotFoundException se l'elemento con il trackingId specificato non esiste
     */
    public Mono<PaperTrackings> updateItem(String trackingId, PaperTrackings paperTrackings, boolean resetPaperStatus) {
        log.debug("Updating item with trackingId: {}", trackingId);

        Map<String, String> expressionAttributeNames = new HashMap<>();
        Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
        List<String> updateExpressions = new ArrayList<>();

        expressionAttributeNames.put("#updatedAt", PaperTrackings.COL_UPDATED_AT);
        expressionAttributeValues.put(":updatedAt", AttributeValue.builder().s(Instant.now().toString()).build());
        updateExpressions.add("#updatedAt = :updatedAt");

        String updateExpr = "SET ";

        Optional.ofNullable(paperTrackings.getValidationFlow())
                .ifPresent(flow -> {
                    List<OcrRequest> requests = flow.getOcrRequests();
                    if (!CollectionUtils.isEmpty(requests)) {
                        expressionAttributeNames.put("#validationFlow", PaperTrackings.COL_VALIDATION_FLOW);
                        expressionAttributeNames.put("#ocr", ValidationFlow.COL_OCR_REQUESTS);
                        expressionAttributeValues.put(":requests", AttributeValue.builder()
                                .l(PaperTrackings.paperTrackingsToAttributeValueMap(paperTrackings).get("validationFlow").m().get("ocrRequests").l()).build());
                        updateExpressions.add("#validationFlow.#ocr = list_append(#validationFlow.#ocr, :requests) ");
                    }

                    flow.setOcrRequests(null);
                });


        Map<String, AttributeValue> attributeValueMap = new HashMap<>(PaperTrackings.paperTrackingsToAttributeValueMap(paperTrackings));

        /*
         * Reset completo di paperStatus.
         * In questo modo DynamoDB sovrascrive l'intero attributo paperStatus
         * con una mappa vuota, eliminando tutti i campi precedenti.
         */
        if (resetPaperStatus) {
            expressionAttributeNames.put("#paperStatus", PaperTrackings.COL_PAPER_STATUS);
            expressionAttributeValues.put(":emptyPaperStatus", AttributeValue.builder()
                    .m(Collections.emptyMap())
                    .build());

            updateExpressions.add("#paperStatus = :emptyPaperStatus");
            attributeValueMap.remove(PaperTrackings.COL_PAPER_STATUS);
        }

        AtomicInteger counter = new AtomicInteger(0);

        attributeValueMap.forEach((key, value) -> {
            List<String> expressions = buildUpdateExpressions(
                    key, value, counter, expressionAttributeNames, expressionAttributeValues
            );
            updateExpressions.addAll(expressions);
        });

        updateExpr += String.join(", ", updateExpressions);

        String conditionExpression = String.format("%s(%s)", "attribute_exists", PaperTrackings.COL_TRACKING_ID);

        log.debug("updateExpr {}", updateExpr);
        log.debug("expressionAttributeValues {}", expressionAttributeValues);
        log.debug("expressionAttributeNames {}", expressionAttributeNames);

        return updateIfExists(trackingId, updateExpr, expressionAttributeValues, expressionAttributeNames, conditionExpression);
    }

    private Mono<PaperTrackings> updateIfExists(String trackingId, String updateExpr, Map<String, AttributeValue> expressionAttributeValues, Map<String, String> expressionAttributeNames, String conditionExpression) {
        return updateIfExists(Map.of(PaperTrackings.COL_TRACKING_ID, AttributeValue.builder().s(trackingId).build()), updateExpr, expressionAttributeValues, expressionAttributeNames, conditionExpression)
                .map(updateItemResponse -> PaperTrackings.attributeValueMapToPaperTrackings(updateItemResponse.attributes()))
                .doOnError(e -> log.error("Error updating item with trackingId {}: {}", trackingId, e.getMessage()))
                .onErrorMap(ConditionalCheckFailedException.class, e -> {
                    log.info("Item with trackingId {} not found — cannot update", trackingId);
                    return new PnPaperTrackerNotFoundException(
                            ERROR_CODE_PAPER_TRACKER_NOT_FOUND,
                            String.format("PaperTracking with trackingId %s not found", trackingId)
                    );
                });
    }

    @Override
    public Flux<PaperTrackings> retrieveAllByTrackingIds(List<String> trackingIds) {
        return findAllByKeys(trackingIds);
    }

}
