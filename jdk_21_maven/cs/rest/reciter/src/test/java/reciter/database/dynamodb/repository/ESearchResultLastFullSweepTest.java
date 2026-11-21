package reciter.database.dynamodb.repository;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

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

/**
 * Covers the persisted lastFullSweep attribute (#696): the conditional, non-regressing
 * stamp; the projection read; the sortable timestamp encoding the DynamoDB-side
 * comparison depends on; and the putItem→updateItem switch that keeps the attribute
 * alive across ordinary strategy saves.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class ESearchResultLastFullSweepTest {

	@Mock
	private DynamoDbEnhancedClient enhancedClient;

	@Mock
	private DynamoDbTable<ESearchResult> table;

	@Mock
	private DynamoDbClient dynamoDbClient;

	private ESearchResultRepository repository;

	@BeforeEach
	public void setUp() {
		when(enhancedClient.table(eq("ESearchResult"), any(TableSchema.class))).thenReturn(table);
		when(table.tableName()).thenReturn("ESearchResult");
		repository = new ESearchResultRepository(enhancedClient, dynamoDbClient);
	}

	@Test
	public void stampIsConditionalAndNonRegressing() {
		repository.stampLastFullSweepIfNewer("cam4024", Instant.parse("2026-08-03T12:00:00Z"));

		ArgumentCaptor<UpdateItemRequest> captor = ArgumentCaptor.forClass(UpdateItemRequest.class);
		verify(dynamoDbClient).updateItem(captor.capture());
		UpdateItemRequest request = captor.getValue();

		assertEquals("ESearchResult", request.tableName());
		assertEquals("cam4024", request.key().get("uid").s());
		assertEquals("SET lastFullSweep = :ts", request.updateExpression());
		// The non-regression guard evaluates inside DynamoDB, so a racing writer can
		// never move the stamp backward regardless of interleaving; and a sweep must
		// never create a ghost item that consists only of a stamp.
		assertEquals("attribute_exists(uid) AND (attribute_not_exists(lastFullSweep) OR lastFullSweep < :ts)",
				request.conditionExpression());
		assertEquals("2026-08-03T12:00:00.000Z", request.expressionAttributeValues().get(":ts").s());
	}

	@Test
	public void refusingToRegressIsNotAnError() {
		when(dynamoDbClient.updateItem(any(UpdateItemRequest.class)))
				.thenThrow(ConditionalCheckFailedException.builder().message("existing stamp is newer").build());

		// A rejected write means a newer stamp already exists — the desired outcome,
		// and it must not disrupt the sweep that just completed.
		assertDoesNotThrow(() -> repository.stampLastFullSweepIfNewer("cam4024",
				Instant.parse("2026-08-03T12:00:00Z")));
	}

	@Test
	public void findLastFullSweepReadsTheProjectedAttribute() {
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("lastFullSweep", AttributeValue.builder().s("2026-05-15T04:30:00.250Z").build());
		when(dynamoDbClient.getItem(any(GetItemRequest.class)))
				.thenReturn(GetItemResponse.builder().item(item).build());

		assertEquals(Instant.parse("2026-05-15T04:30:00.250Z"), repository.findLastFullSweep("cam4024"));

		ArgumentCaptor<GetItemRequest> captor = ArgumentCaptor.forClass(GetItemRequest.class);
		verify(dynamoDbClient).getItem(captor.capture());
		assertEquals("lastFullSweep", captor.getValue().projectionExpression());
		assertEquals("cam4024", captor.getValue().key().get("uid").s());
	}

	@Test
	public void neverStampedOrMissingItemReadsAsNull() {
		when(dynamoDbClient.getItem(any(GetItemRequest.class)))
				.thenReturn(GetItemResponse.builder().build());
		assertNull(repository.findLastFullSweep("cam4024"));

		Map<String, AttributeValue> itemWithoutStamp = new HashMap<>();
		itemWithoutStamp.put("uid", AttributeValue.builder().s("cam4024").build());
		when(dynamoDbClient.getItem(any(GetItemRequest.class)))
				.thenReturn(GetItemResponse.builder().item(itemWithoutStamp).build());
		assertNull(repository.findLastFullSweep("cam4024"));
	}

	@Test
	public void unparseableStampReadsAsNeverSwept() {
		// Null errs toward an extra sweep (the caller falls back to inference), never
		// toward suppressing a due one.
		Map<String, AttributeValue> item = new HashMap<>();
		item.put("lastFullSweep", AttributeValue.builder().s("not-a-timestamp").build());
		when(dynamoDbClient.getItem(any(GetItemRequest.class)))
				.thenReturn(GetItemResponse.builder().item(item).build());
		assertNull(repository.findLastFullSweep("cam4024"));
	}

	@Test
	public void sortableTimestampOrderMatchesChronologicalOrder() {
		// The condition expression compares the strings lexicographically inside
		// DynamoDB. Instant.toString() would get the sub-second case wrong:
		// "…00Z" sorts AFTER "…00.123Z" despite being earlier. The fixed-width
		// rendering must keep lexicographic order chronological, including that case.
		Instant[] ascending = {
				Instant.parse("2026-08-03T11:59:59.999Z"),
				Instant.parse("2026-08-03T12:00:00Z"),
				Instant.parse("2026-08-03T12:00:00.123Z"),
				Instant.parse("2026-08-03T12:00:01Z"),
				Instant.parse("2027-01-01T00:00:00Z"),
		};
		for (int i = 0; i < ascending.length; i++) {
			for (int j = i + 1; j < ascending.length; j++) {
				String earlier = ESearchResultRepository.toSortableTimestamp(ascending[i]);
				String later = ESearchResultRepository.toSortableTimestamp(ascending[j]);
				assertTrue(earlier.compareTo(later) < 0,
						earlier + " must sort before " + later);
			}
		}
		// Round-trips through the parser used on read.
		assertEquals(Instant.parse("2026-08-03T12:00:00Z"),
				ESearchResultRepository.parseSweepTimestamp(
						ESearchResultRepository.toSortableTimestamp(Instant.parse("2026-08-03T12:00:00Z")), "cam4024"));
	}

	@Test
	public void saveUsesUpdateItemSoTheStampSurvivesStrategySaves() {
		// putItem would replace the whole item and erase lastFullSweep (which is not on
		// the bean schema) on every strategy save; updateItem touches only the schema's
		// own attributes.
		ESearchResult result = new ESearchResult();
		result.setUid("cam4024");

		repository.save(result);

		verify(table).updateItem(result);
	}
}
