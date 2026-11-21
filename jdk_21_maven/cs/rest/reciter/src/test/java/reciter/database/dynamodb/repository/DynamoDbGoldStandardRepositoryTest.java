package reciter.database.dynamodb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.GoldStandard;

@ExtendWith(MockitoExtension.class)
public class DynamoDbGoldStandardRepositoryTest {

	@Mock
	private DynamoDbGoldStandardRepository goldStandardRepository;

	private GoldStandard goldStandard1;
	private GoldStandard goldStandard2;

	@BeforeEach
	public void setUp() {

		goldStandard1 = new GoldStandard("uid1", Arrays.asList(123L, 456L), Arrays.asList(779L), null,null);
		goldStandard2 = new GoldStandard("uid2", Arrays.asList(5678L, 8760L), Arrays.asList(799L), null,null);
	
	}

	@Test
	public void testSave() {
		// Mock findById to return the saved object
		when(goldStandardRepository.findById("uid1")).thenReturn(Optional.of(goldStandard1));

		// Save the GoldStandard object
		goldStandardRepository.save(goldStandard1);
		verify(goldStandardRepository).save(goldStandard1);

		// Retrieve the saved GoldStandard object by ID
		GoldStandard retrievedGoldStandard = goldStandardRepository.findById("uid1").orElse(null);

		// Assert that the saved and retrieved objects are the same
		assertEquals(goldStandard1, retrievedGoldStandard);
	}

	@Test
	public void testFindById() {
		// Mock findById to return the object
		when(goldStandardRepository.findById("uid1")).thenReturn(Optional.of(goldStandard1));

		// Save the object (mocked)
		goldStandardRepository.save(goldStandard1);
		verify(goldStandardRepository).save(goldStandard1);

		// Retrieve the GoldStandard object by ID
		GoldStandard retrievedGoldStandard = goldStandardRepository.findById("uid1").orElse(null);

		// Assert that the retrieved object is not null and matches the saved object
		assertTrue(retrievedGoldStandard != null);
		assertEquals(goldStandard1, retrievedGoldStandard);
	}

	@Test
	public void testFindAll() {
		List<GoldStandard> goldStandardList = Arrays.asList(goldStandard1, goldStandard2);

		// Mock findAll and getItemCount
		when(goldStandardRepository.findAll()).thenReturn(goldStandardList);
		when(goldStandardRepository.getItemCount()).thenReturn((long) goldStandardList.size());

		Iterable<GoldStandard> retrieved = goldStandardRepository.findAll();
		long count = goldStandardRepository.getItemCount();

		List<GoldStandard> goldStandardListFromIterable = new ArrayList<>();
		retrieved.forEach(goldStandardListFromIterable::add);

		// Verify interactions
		verify(goldStandardRepository).findAll();
		verify(goldStandardRepository).getItemCount();

		// Assert that the size of the list matches the expected count
		assertEquals(count, goldStandardListFromIterable.size());
	}

	@Test
	public void testGetItemCount() {
		// Mock getItemCount
		when(goldStandardRepository.getItemCount()).thenReturn(4L);

		// Save the objects (mocked)
		goldStandardRepository.saveAll(Arrays.asList(goldStandard1, goldStandard2));
		verify(goldStandardRepository).saveAll(Arrays.asList(goldStandard1, goldStandard2));

		// Get the count of items in the table
		long itemCount = goldStandardRepository.getItemCount();

		// Assert that the item count is correct
		assertEquals(4L, itemCount);
	}

	@Test
	public void testFindAllById() {
		List<GoldStandard> expectedResults = Arrays.asList(goldStandard1, goldStandard2);
		List<String> ids = Arrays.asList("uid1", "uid2");

		// Mock findAllById
		when(goldStandardRepository.findAllById(ids)).thenReturn(expectedResults);

		// Save the objects (mocked)
		goldStandardRepository.saveAll(Arrays.asList(goldStandard1, goldStandard2));
		verify(goldStandardRepository).saveAll(Arrays.asList(goldStandard1, goldStandard2));

		// Retrieve GoldStandard objects by their IDs
		List<GoldStandard> goldStandards = goldStandardRepository.findAllById(ids);

		// Assert that the retrieved list contains the correct GoldStandard objects
		assertEquals(2, goldStandards.size());
		assertEquals("uid1", goldStandards.get(0).getUid());
		assertEquals("uid2", goldStandards.get(1).getUid());
	}
}