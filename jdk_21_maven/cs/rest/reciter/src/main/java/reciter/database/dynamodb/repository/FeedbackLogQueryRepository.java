package reciter.database.dynamodb.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.FeedbackLog;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

@Repository
public class FeedbackLogQueryRepository {

	private static final String ARTICLE_PROVENANCE_TABLE = "ArticleProvenance";

	private final DynamoDbTable<FeedbackLog> feedbackLogTable;
	private final DynamoDbClient lowLevelClient; // used for ArticleProvenance enrichment reads

	public FeedbackLogQueryRepository(DynamoDbEnhancedClient enhancedClient, DynamoDbClient lowLevelClient) {
		this.feedbackLogTable = enhancedClient.table("FeedbackLog", TableSchema.fromBean(FeedbackLog.class));
		this.lowLevelClient = lowLevelClient;
	}

	/**
	 * Query all FeedbackLog rows for a uid, newest sk (highest epoch) first.
	 * Uses the enhanced client via DynamoDbTable — no raw attribute map needed
	 * since FeedbackLog is a proper @DynamoDbBean model.
	 */
	public List<FeedbackLog> findFeedbackLogsByUid(String uid) {
		QueryConditional queryConditional = QueryConditional.keyEqualTo(k -> k.partitionValue(uid));

		return feedbackLogTable.query(r -> r
				.queryConditional(queryConditional)
				.scanIndexForward(false)) // newest sk (highest epoch) first
				.items()
				.stream()
				.toList();
	}

	/**
	 * Query all ArticleProvenance rows for a uid.
	 * Uses lowLevelClient since ArticleProvenance enrichment reads return raw
	 * attribute maps for lightweight projection (no full model deserialization needed).
	 * Pagination is handled internally; returns a flat list.
	 */
	public List<Map<String, AttributeValue>> findProvenanceByUid(String uid) {
		List<Map<String, AttributeValue>> items = new ArrayList<>();

		Map<String, AttributeValue> expressionValues = Map.of(
			":uid", AttributeValue.fromS(uid)
		);

		Map<String, AttributeValue> lastKey = null;
		do {
			QueryRequest.Builder reqBuilder = QueryRequest.builder()
					.tableName(ARTICLE_PROVENANCE_TABLE)
					.keyConditionExpression("uid = :uid")
					.expressionAttributeValues(expressionValues);

			if (lastKey != null) {
				reqBuilder.exclusiveStartKey(lastKey);
			}

			QueryResponse response = lowLevelClient.query(reqBuilder.build());
			items.addAll(response.items());
			lastKey = response.lastEvaluatedKey().isEmpty() ? null : response.lastEvaluatedKey();

		} while (lastKey != null);

		return items;
	}
}
