package reciter.service.dynamo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.ESearchPmid;
import reciter.database.dynamodb.model.ESearchResult;
import reciter.database.dynamodb.repository.ESearchResultRepository;

/**
 * Covers the uid -&gt; sorted, deduplicated pmids projection that moved here from
 * the controller (#734 review).
 */
@ExtendWith(MockitoExtension.class)
public class ESearchResultServiceImplTest {

	@Mock
	private ESearchResultRepository eSearchResultRepository;

	@InjectMocks
	private ESearchResultServiceImpl eSearchResultServiceImpl;

	@Test
	public void findRetrievedPmidsByUidsDedupesAndSortsAcrossStrategies() {
		// Arrange: two strategies for testUid overlapping on 23456L, plus one uid with no record
		ESearchPmid strategyOne = new ESearchPmid();
		strategyOne.setPmids(Arrays.asList(23456L, 12345L));
		strategyOne.setRetrievalStrategyName("FullNameRetrievalStrategy");

		ESearchPmid strategyTwo = new ESearchPmid();
		strategyTwo.setPmids(Arrays.asList(23456L, 34567L));
		strategyTwo.setRetrievalStrategyName("FirstNameInitialRetrievalStrategy");

		ESearchResult resultForTestUid = new ESearchResult();
		resultForTestUid.setUid("test123");
		resultForTestUid.setESearchPmids(Arrays.asList(strategyOne, strategyTwo));

		when(eSearchResultRepository.findAllById(Arrays.asList("test123", "missingUid")))
				.thenReturn(Arrays.asList(resultForTestUid, null));

		// Act
		Map<String, List<Long>> result =
				eSearchResultServiceImpl.findRetrievedPmidsByUids(Arrays.asList("test123", "missingUid"));

		// Assert: missingUid is omitted (no ESearchResult record), not present as an empty array
		assertEquals(1, result.size());
		assertEquals(Arrays.asList(12345L, 23456L, 34567L), result.get("test123"));
	}

	@Test
	public void findRetrievedPmidsByUidsOmitsNullsAndToleratesEmptyOrNullPmids() {
		// Arrange
		ESearchPmid strategyOne = new ESearchPmid();
		strategyOne.setPmids(Arrays.asList(300L, 100L));

		ESearchPmid strategyTwo = new ESearchPmid();
		strategyTwo.setPmids(Arrays.asList(100L, 200L));

		ESearchPmid strategyWithNullPmids = new ESearchPmid();
		strategyWithNullPmids.setPmids(null);

		ESearchResult withPmids = new ESearchResult();
		withPmids.setUid("uidWithData");
		withPmids.setESearchPmids(Arrays.asList(strategyOne, strategyTwo, strategyWithNullPmids));

		ESearchResult withNoStrategies = new ESearchResult();
		withNoStrategies.setUid("uidWithNoStrategies");
		withNoStrategies.setESearchPmids(null);

		when(eSearchResultRepository.findAllById(Arrays.asList("uidWithData", "missingUid", "uidWithNoStrategies")))
				.thenReturn(Arrays.asList(withPmids, null, withNoStrategies));

		// Act
		Map<String, List<Long>> result = eSearchResultServiceImpl
				.findRetrievedPmidsByUids(Arrays.asList("uidWithData", "missingUid", "uidWithNoStrategies"));

		// Assert
		assertEquals(2, result.size());
		assertEquals(Arrays.asList(100L, 200L, 300L), result.get("uidWithData"));
		assertEquals(Collections.emptyList(), result.get("uidWithNoStrategies"));
	}
}
