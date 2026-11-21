package reciter.database.dynamodb.repository;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.ESearchResult;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.GetItemResponse;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

@Repository
public class ESearchResultRepository {

	private static final Logger log = LoggerFactory.getLogger(ESearchResultRepository.class);

	/**
	 * When the uid's last clean ALL_PUBLICATIONS sweep completed (#696). Lives on the
	 * ESearchResult item but deliberately NOT on the bean schema: the model class comes
	 * from the reciter-dynamodb-model artifact, so the attribute is managed here with
	 * the low-level client instead — written only by {@link #stampLastFullSweepIfNewer}
	 * and read only by {@link #findLastFullSweep}. For that to be safe, {@link #save}
	 * must not use putItem (which replaces the whole item and would erase any attribute
	 * the bean schema does not know about); it uses updateItem, which touches only the
	 * schema's own attributes.
	 */
	static final String LAST_FULL_SWEEP = "lastFullSweep";

	/**
	 * Fixed-width UTC timestamp for the lastFullSweep attribute. The non-regression
	 * condition compares timestamps inside DynamoDB, where string comparison is
	 * lexicographic — {@code Instant.toString()} is unusable for that because it drops
	 * trailing zeros ("...00Z" sorts after "...00.123Z"). A constant-width rendering
	 * makes lexicographic order chronological order.
	 */
	private static final DateTimeFormatter SWEEP_TIMESTAMP =
			DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

	private final DynamoDbTable<ESearchResult> eSearchResultTable;
	private final DynamoDbClient dynamoDbClient;

	public ESearchResultRepository(DynamoDbEnhancedClient enhancedClient, DynamoDbClient dynamoDbClient) {

		this.eSearchResultTable = enhancedClient.table("ESearchResult", TableSchema.fromBean(ESearchResult.class));
		this.dynamoDbClient = dynamoDbClient;

	}

	public void save(ESearchResult eSearchResult) {
		// updateItem, not putItem: putItem replaces the entire item, which would erase the
		// lastFullSweep attribute (not on the bean schema — see LAST_FULL_SWEEP) on every
		// strategy save. updateItem writes exactly the schema's attributes — including
		// removing ones that are null on the bean, matching putItem's end state for them —
		// and leaves everything else on the item alone. Like putItem, it creates the item
		// when none exists.
		eSearchResultTable.updateItem(eSearchResult);
	}

	public void saveAll(List<ESearchResult> eSearchResults) {
		eSearchResults.forEach(eSearchResult -> eSearchResultTable.updateItem(eSearchResult));
	}

	/**
	 * Record that a clean full sweep for this uid completed at {@code sweepTime} — but
	 * never move an existing stamp backward. The condition runs inside DynamoDB, so a
	 * Publication Manager request racing the batch cannot regress the stamp no matter
	 * how the two writes interleave. Requires the item to exist: a sweep that persisted
	 * nothing must not create a ghost item that consists only of a stamp.
	 */
	public void stampLastFullSweepIfNewer(String uid, Instant sweepTime) {
		String value = toSortableTimestamp(sweepTime);
		Map<String, AttributeValue> key = new HashMap<>();
		key.put("uid", AttributeValue.builder().s(uid).build());
		Map<String, AttributeValue> values = new HashMap<>();
		values.put(":ts", AttributeValue.builder().s(value).build());
		try {
			dynamoDbClient.updateItem(UpdateItemRequest.builder()
					.tableName(eSearchResultTable.tableName())
					.key(key)
					.updateExpression("SET " + LAST_FULL_SWEEP + " = :ts")
					.conditionExpression("attribute_exists(uid) AND (attribute_not_exists(" + LAST_FULL_SWEEP
							+ ") OR " + LAST_FULL_SWEEP + " < :ts)")
					.expressionAttributeValues(values)
					.build());
			log.info("Stamped lastFullSweep=[{}] for uid=[{}]", value, uid);
		} catch (ConditionalCheckFailedException e) {
			// A newer stamp already exists (a concurrent sweep finished later) or the item
			// is gone — either way, skipping is the non-regressing behavior we want.
			log.info("Skipped lastFullSweep stamp for uid=[{}]: existing stamp is newer or item absent", uid);
		}
	}

	/**
	 * The uid's persisted lastFullSweep, or null when the item has never been stamped
	 * (or does not exist). Read with a projection on the low-level client because the
	 * attribute is not on the bean schema.
	 */
	public Instant findLastFullSweep(String uid) {
		Map<String, AttributeValue> key = new HashMap<>();
		key.put("uid", AttributeValue.builder().s(uid).build());
		GetItemResponse response = dynamoDbClient.getItem(GetItemRequest.builder()
				.tableName(eSearchResultTable.tableName())
				.key(key)
				.projectionExpression(LAST_FULL_SWEEP)
				.build());
		if (!response.hasItem() || !response.item().containsKey(LAST_FULL_SWEEP)) {
			return null;
		}
		return parseSweepTimestamp(response.item().get(LAST_FULL_SWEEP).s(), uid);
	}

	static String toSortableTimestamp(Instant instant) {
		return SWEEP_TIMESTAMP.format(instant);
	}

	static Instant parseSweepTimestamp(String value, String uid) {
		if (value == null) {
			return null;
		}
		try {
			return Instant.parse(value);
		} catch (DateTimeParseException e) {
			// Unparseable is treated as never-stamped: the caller falls back to the
			// lookupType inference, which errs toward an extra sweep, never a missed one.
			log.warn("Unparseable lastFullSweep=[{}] for uid=[{}]; treating as never swept", value, uid);
			return null;
		}
	}

	public Optional<ESearchResult> findById(String id) {
		return Optional.ofNullable(eSearchResultTable.getItem(r -> r.key(k -> k.partitionValue(id))));
	}

	public Iterable<ESearchResult> findAll() {
		return eSearchResultTable.scan().items();
	}

	public void deleteById(String id) {
		ESearchResult eSearchResult = new ESearchResult();
		eSearchResult.setUid(id);
		eSearchResultTable.deleteItem(eSearchResult);
	}

	public void deleteAll() {
		eSearchResultTable.scan().items().forEach(entity -> eSearchResultTable.deleteItem(entity));
	}

	@SuppressWarnings("unused")
	public long getItemCount() {
		long count = 0;

		for (ESearchResult item : eSearchResultTable.scan().items()) {
			count++;
		}
		return count;
	}

	public List<ESearchResult> findAllById(List<String> uids) {List<ESearchResult> eSearchResults = new ArrayList<>();
	for (String uid : uids) {
		ESearchResult eSearchResult = eSearchResultTable.getItem(r -> r.key(k -> k.partitionValue(uid)));
		if(eSearchResult != null) {
			eSearchResults.add(eSearchResult);
		}
	}
	return eSearchResults;}
}
