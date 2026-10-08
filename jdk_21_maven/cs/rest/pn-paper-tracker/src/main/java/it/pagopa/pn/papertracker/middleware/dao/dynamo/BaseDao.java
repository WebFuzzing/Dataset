package it.pagopa.pn.papertracker.middleware.dao.dynamo;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.*;
import software.amazon.awssdk.enhanced.dynamodb.model.*;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemResponse;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class BaseDao<T> {
    private final DynamoDbAsyncTable<T> tableAsync;
    private final Class<T> tClass;
    private final DynamoDbAsyncClient dynamoDbAsyncClient;
    private final DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient;

    protected BaseDao(
            DynamoDbEnhancedAsyncClient dynamoDbEnhancedAsyncClient,
            DynamoDbAsyncClient dynamoDbAsyncClient,
            String tableName,
            Class<T> tClass
    ) {
        this.tableAsync = dynamoDbEnhancedAsyncClient.table(tableName, TableSchema.fromBean(tClass));
        this.tClass = tClass;
        this.dynamoDbAsyncClient = dynamoDbAsyncClient;
        this.dynamoDbEnhancedAsyncClient = dynamoDbEnhancedAsyncClient;
    }

    protected Mono<T> putItem(T entity) {
        return Mono.fromFuture(this.tableAsync.putItem(entity))
                .thenReturn(entity);
    }

    public Flux<T> retrieveByRequestId(String pk) {
        log.info("retrieve Items from [{}] table, for requestId={}", tableAsync.tableName(), pk);
        return Flux.from(tableAsync.query(r -> r.queryConditional(
                        QueryConditional.keyEqualTo(Key.builder().partitionValue(pk).build()))))
                .flatMap(page -> Flux.fromIterable(page.items()))
                .doOnError(e -> log.error("Error retrieving items for requestId {}: {}", pk, e.getMessage()));
    }

    public Mono<T> getByKey(Key key) {
        return Mono.fromFuture(tableAsync.getItem(key));
    }

    public Flux<T> queryByIndex(Key build, String indexName) {
        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(QueryConditional.keyEqualTo(build))
                .build();

        return Mono.from(tableAsync.index(indexName).query(request))
                .map(Page::items)
                .flatMapMany(Flux::fromIterable)
                .doOnError(e -> log.error("Error querying index {}: {}", indexName, e.getMessage()));
    }

    public Flux<T> retrieveFromIndex(String indexName, QueryConditional queryConditional) {
        return Mono.from(tableAsync.index(indexName)
                        .query(QueryEnhancedRequest.builder().queryConditional(queryConditional).build()))
                .map(Page::items)
                .flatMapMany(Flux::fromIterable);
    }

    public Mono<T> putIfAbsent(String expression, T entity) {

        Expression conditionExpressionPut = Expression.builder()
                .expression(expression)
                .build();

        PutItemEnhancedRequest<T> request = PutItemEnhancedRequest.builder(tClass)
                .item(entity)
                .conditionExpression(conditionExpressionPut)
                .build();

        return Mono.fromFuture(tableAsync.putItem(request))
                .thenReturn(entity);
    }

    public Mono<UpdateItemResponse> updateIfExists(Map<String, AttributeValue> key, String updateExpression, Map<String, AttributeValue> expressionValues, Map<String, String> expressionNames, String conditionExpression) {
        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                .tableName(tableAsync.tableName())
                .key(key)
                .updateExpression(updateExpression)
                .expressionAttributeValues(expressionValues)
                .expressionAttributeNames(expressionNames)
                .conditionExpression(conditionExpression)
                .returnValues("ALL_NEW")
                .build();

        return Mono.fromFuture(dynamoDbAsyncClient.updateItem(updateItemRequest));
    }

    protected Flux<T> findAllByKeys(List<String> partitionKeys) {
        ReadBatch.Builder<T> builder = ReadBatch.builder(tClass)
                .mappedTableResource(this.tableAsync);

        partitionKeys.forEach(partitionKey ->
                builder.addGetItem(Key.builder().partitionValue(partitionKey).build())
        );

        BatchGetItemEnhancedRequest request = BatchGetItemEnhancedRequest.builder()
                .readBatches(builder.build())
                .build();

        return Flux.from(dynamoDbEnhancedAsyncClient.batchGetItem(request)
                        .map(result -> result.resultsForTable(tableAsync)))
                .flatMap(Flux::fromIterable);
    }

    /**
     * Costruisce updateExpression per DynamoDB in base al tipo di attributo fornito.
     * Per gli attributi di tipo lista, genera un'espressione per aggiungere alla lista utilizzando {@code list_append} e {@code if_not_exists}.
     * Per gli attributi di tipo mappa (oggetto annidato), genera espressioni per aggiornare ciascuna coppia chiave-valore interna.
     * Per altri tipi di attributo, genera una semplice espressione di assegnazione.
     *
     * @param key     La chiave dell'attributo da aggiornare
     * @param value   Il valore dell'attributo da aggiornare
     * @param counter Un contatore atomico utilizzato per generare indici univoci per i parametri
     * @param names   Mappa dei nomi degli attributi per l'espressione DynamoDB
     * @param values  Mappa dei valori degli attributi per l'espressione DynamoDB
     * @return Una lista di espressioni di aggiornamento da utilizzare in una richiesta di update su DynamoDB
     */
    protected List<String> buildUpdateExpressions(String key, AttributeValue value, AtomicInteger counter, Map<String, String> names, Map<String, AttributeValue> values) {
        List<String> expressions = new ArrayList<>();

        if(key.equalsIgnoreCase("validationConfig")){
            return expressions;
        }

        if (isListAttributeType(value)) {
            int idx = counter.getAndIncrement();
            names.put("#k" + idx, key);
            values.put(":v" + idx, AttributeValue.builder().l(value.l()).build());
            values.put(":empty" + idx, AttributeValue.builder().l(Collections.emptyList()).build());
            expressions.add("#k" + idx + " = list_append(if_not_exists(" + "#k" + idx + ", " + ":empty" + idx + "), " + ":v" + idx + ")");

        } else if (isInnerObjectAttributeType(value)) {
            value.m().forEach((innerKey, innerVal) -> {
                int idx = counter.getAndIncrement();
                if(key.equalsIgnoreCase("paperStatus")){
                    names.put("#paperStatus", key);
                    names.put("#paperStatus_" + idx + "_inner", innerKey);
                    values.put(":v" + idx, innerVal);
                    expressions.add("#paperStatus.#paperStatus_" + idx + "_inner" + " = " + ":v" + idx);
                }
                else if(key.equalsIgnoreCase("validationFlow")){
                    names.put("#validationFlow" , key);
                    names.put("#validationFlow_" + idx + "_inner", innerKey);
                    values.put(":v" + idx, innerVal);
                    expressions.add("#validationFlow.#validationFlow_" + idx + "_inner" + " = " + ":v" + idx);
                }
                else {
                    names.put("#k" + idx, key);
                    names.put("#k" + idx + "_inner", innerKey);
                    values.put(":v" + idx, innerVal);
                    expressions.add("#k" + idx + "." + "#k" + idx + "_inner" + " = " + ":v" + idx);
                }
            });
        } else {
            if(!key.equalsIgnoreCase("paperStatus") && !key.equalsIgnoreCase("validationFlow")){
                int idx = counter.getAndIncrement();
                names.put("#k" + idx, key);
                values.put(":v" + idx, value);
                expressions.add("#k" + idx + " = " + ":v" + idx);
            }
        }
        return expressions;
    }

    private static boolean isInnerObjectAttributeType(AttributeValue value) {
        return value.m() != null && !value.m().isEmpty();
    }

    private boolean isListAttributeType(AttributeValue value) {
        return value.l() != null && !value.l().isEmpty();
    }

}
