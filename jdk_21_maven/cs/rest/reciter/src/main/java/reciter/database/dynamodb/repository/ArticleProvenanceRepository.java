package reciter.database.dynamodb.repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.ArticleProvenance;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
public class ArticleProvenanceRepository {

	private final DynamoDbTable<ArticleProvenance> articleProvenanceTable;
	private final DynamoDbClient lowLevelClient; // Injected explicitly to handle expression mapping

	// Spring will automatically inject both configured beans from your DynamoDbConfig
	public ArticleProvenanceRepository(DynamoDbEnhancedClient enhancedClient, DynamoDbClient lowLevelClient) {
		this.lowLevelClient = lowLevelClient;
		this.articleProvenanceTable = enhancedClient.table("ArticleProvenance", TableSchema.fromBean(ArticleProvenance.class));
	}

	public Optional<ArticleProvenance> findByIdWithConsistentRead(String uid, String articleId) {
		return Optional.ofNullable(
			articleProvenanceTable.getItem(r -> r.key(k -> k.partitionValue(uid).sortValue(articleId))
					.consistentRead(true))
		);
	}

	public void upsertRetrievalProvenance(String uid, String articleId, String strategyCode, String srcPm, long epochSeconds) {
		Map<String, AttributeValue> key = Map.of(
			"uid", AttributeValue.fromS(uid),
			"articleId", AttributeValue.fromS(articleId)
		);

		Map<String, AttributeValue> values = Map.of(
			":strategy", AttributeValue.fromS(strategyCode),
			":now", AttributeValue.fromN(String.valueOf(epochSeconds)),
			":strategySet", AttributeValue.builder().ss(strategyCode).build(),
			":pm", AttributeValue.fromS(srcPm)
		);

		UpdateItemRequest req = UpdateItemRequest.builder()
				.tableName("ArticleProvenance")
				.key(key)
				.updateExpression("SET rs = if_not_exists(rs, :strategy), frd = if_not_exists(frd, :now), src = if_not_exists(src, :pm) ADD ads :strategySet")
				.expressionAttributeValues(values)
				.build();

		lowLevelClient.updateItem(req);
	}

	public void writePmUiSearchRecord(String uid, String articleId, String pmUiSearchRs, String srcPm, long epochSeconds) {
		Map<String, AttributeValue> key = Map.of(
			"uid", AttributeValue.fromS(uid),
			"articleId", AttributeValue.fromS(articleId)
		);

		Map<String, AttributeValue> values = Map.of(
			":rs", AttributeValue.fromS(pmUiSearchRs),
			":pm", AttributeValue.fromS(srcPm),
			":ts", AttributeValue.fromN(String.valueOf(epochSeconds)),
			":rsSet", AttributeValue.builder().ss(pmUiSearchRs).build()
		);

		UpdateItemRequest req = UpdateItemRequest.builder()
				.tableName("ArticleProvenance")
				.key(key)
				.updateExpression("SET rs = if_not_exists(rs, :rs), src = if_not_exists(src, :pm), frd = if_not_exists(frd, :ts) ADD ads :rsSet")
				.expressionAttributeValues(values)
				.build();

		lowLevelClient.updateItem(req);
	}

	public void applyD11Update(String uid, String articleId, String newSrc, String existingSrc, long epochSeconds) {
		Map<String, AttributeValue> key = Map.of(
			"uid", AttributeValue.fromS(uid),
			"articleId", AttributeValue.fromS(articleId)
		);

		Map<String, AttributeValue> values = new HashMap<>();
		values.put(":ts", AttributeValue.fromN(String.valueOf(epochSeconds)));

		UpdateItemRequest.Builder reqBuilder = UpdateItemRequest.builder()
				.tableName("ArticleProvenance")
				.key(key);

		if (existingSrc == null) {
			values.put(":new", AttributeValue.fromS(newSrc));
			reqBuilder.updateExpression("SET src = :new, frd = if_not_exists(frd, :ts)")
					  .conditionExpression("attribute_not_exists(src)");
		} else if (!newSrc.equals(existingSrc)) {
			values.put(":new", AttributeValue.fromS(newSrc));
			values.put(":existing", AttributeValue.fromS(existingSrc));
			reqBuilder.updateExpression("SET src = :new, frd = if_not_exists(frd, :ts)")
					  .conditionExpression("src = :existing");
		} else {
			reqBuilder.updateExpression("SET frd = if_not_exists(frd, :ts)");
		}

		reqBuilder.expressionAttributeValues(values);
		lowLevelClient.updateItem(reqBuilder.build());
	}
	/**
	 * Authorship Review tab (PM_AUTHOR) marker write. Mirrors {@link #writePmUiSearchRecord}
	 * — sets {@code rs} if absent, ensures {@code frd}, and ADDs PM_AUTHOR to the {@code ads}
	 * strategy set — but deliberately does NOT seed {@code src='PM'}. The AAR queue contains
	 * authorships production either buried (already retrieved → an ArticleProvenance row with
	 * {@code src='PM'} already exists → D-11 lifts it to MAN_FROM_PM) or never scored at all
	 * (no row → D-11 sets {@code src='MAN'} = algo-missed, curator-found). Seeding {@code src='PM'}
	 * here would wrongly relabel a never-retrieved authorship as PM-sourced.
	 */
	public void writePmAuthorRecord(String uid, String articleId, String pmAuthorRs, long epochSeconds) {
		Map<String, AttributeValue> key = Map.of(
			"uid", AttributeValue.fromS(uid),
			"articleId", AttributeValue.fromS(articleId)
		);

		Map<String, AttributeValue> values = Map.of(
			":rs", AttributeValue.fromS(pmAuthorRs),
			":ts", AttributeValue.fromN(String.valueOf(epochSeconds)),
			":rsSet", AttributeValue.builder().ss(pmAuthorRs).build()
		);

		UpdateItemRequest req = UpdateItemRequest.builder()
				.tableName("ArticleProvenance")
				.key(key)
				.updateExpression(
						"SET rs  = if_not_exists(rs,  :rs), " +
						"    frd = if_not_exists(frd, :ts) " +
						"ADD ads :rsSet")
				.expressionAttributeValues(values)
				.build();

		lowLevelClient.updateItem(req);
	}
}