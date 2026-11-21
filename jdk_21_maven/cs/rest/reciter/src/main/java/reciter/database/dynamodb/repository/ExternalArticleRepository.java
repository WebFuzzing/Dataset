package reciter.database.dynamodb.repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.ExternalArticle;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryEnhancedRequest;

@Repository
public class ExternalArticleRepository {

    private final DynamoDbTable<ExternalArticle> externalArticleTable;

    public ExternalArticleRepository(DynamoDbEnhancedClient enhancedClient) {
        this.externalArticleTable = enhancedClient.table("ExternalArticle", TableSchema.fromBean(ExternalArticle.class));
    }

    public void save(ExternalArticle externalArticle) {
        externalArticleTable.putItem(externalArticle);
    }

    /**
     * Query all ExternalArticle rows for a given uid (partition key).
     * Mirrors the v1 DynamoDBQueryExpression.withHashKeyValues(...).withConsistentRead(true) behavior.
     */
    public List<ExternalArticle> findByUid(String uid) {
        QueryConditional queryConditional =
                QueryConditional.keyEqualTo(Key.builder().partitionValue(uid).build());

        QueryEnhancedRequest request = QueryEnhancedRequest.builder()
                .queryConditional(queryConditional)
                .consistentRead(true)
                .build();

        return externalArticleTable.query(request)
                .items()
                .stream()
                .collect(Collectors.toList());
    }

    public Optional<ExternalArticle> find(String uid, String articleId) {
        return Optional.ofNullable(
                externalArticleTable.getItem(r -> r.key(k -> k.partitionValue(uid).sortValue(articleId)))
        );
    }

    public void delete(String uid, String articleId) {
        ExternalArticle key = new ExternalArticle();
        key.setUid(uid);
        key.setArticleId(articleId);
        externalArticleTable.deleteItem(key);
    }
}