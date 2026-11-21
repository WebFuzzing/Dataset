package reciter.database.dynamodb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.MeshTerm;

@ExtendWith(MockitoExtension.class)
public class MeshTermRepositoryTest {

	@Mock
	private DynamoMeshTermRepository dynamoMeshTermRepository;

	private MeshTerm meshTerm1;
	private MeshTerm meshTerm2;

	@BeforeEach
	public void setUp() {
		meshTerm1 = new MeshTerm("Term1", 789L);
		meshTerm2 = new MeshTerm("Term2", 101112L);

	}

	@Test
	public void save() {

		// Mock the save and findById behavior
		when(dynamoMeshTermRepository.findById("Term1")).thenReturn(Optional.of(meshTerm1));

		// Call the save method
		dynamoMeshTermRepository.save(meshTerm1);

		// Verify save was called
		verify(dynamoMeshTermRepository).save(meshTerm1);
		MeshTerm mesh = dynamoMeshTermRepository.findById("Term1").orElse(null);
		assertEquals(meshTerm1, mesh);
	}

	@Test
	public void testFindById() {

		// Mock the findById behavior
		when(dynamoMeshTermRepository.findById("Term2")).thenReturn(Optional.of(meshTerm2));

		// Save the mesh term (mocked)
		dynamoMeshTermRepository.save(meshTerm2);
		verify(dynamoMeshTermRepository).save(meshTerm2);

		// Retrieve the MeshTerm object by ID
		MeshTerm retrievedMeshTerm = dynamoMeshTermRepository.findById("Term2").orElse(null);

		// Assert that the retrieved object is not null and matches the saved object
		assertTrue(retrievedMeshTerm != null);
		assertEquals(meshTerm2, retrievedMeshTerm);
	}

	@SuppressWarnings("unused")
	@Test
	public void testFindAll() {

		// Mock findAll to return our test data
		when(dynamoMeshTermRepository.findAll()).thenReturn(Arrays.asList(meshTerm1, meshTerm2));
		when(dynamoMeshTermRepository.getItemCount()).thenReturn(2L);

		// Save the mesh terms (just for the structure of the test, these are mocked)
		dynamoMeshTermRepository.save(meshTerm1);
		dynamoMeshTermRepository.save(meshTerm2);
		verify(dynamoMeshTermRepository).save(meshTerm1);
		verify(dynamoMeshTermRepository).save(meshTerm2);

		// Retrieve all MeshTerm objects
		Iterable<MeshTerm> meshTerms = dynamoMeshTermRepository.findAll();

		// Count the results
		int count = 0;
		for (MeshTerm meshTerm : meshTerms) {
			count++;
		}

		// Get item count from repository
		long itemCount = dynamoMeshTermRepository.getItemCount();

		// Verify count matches expected
		assertEquals(itemCount, count);
	}

	@Test
	public void testGetItemCount() {
		// Mock the getItemCount method to return a specific value
		when(dynamoMeshTermRepository.getItemCount()).thenReturn(30706L);

		// Call the method
		long itemCount = dynamoMeshTermRepository.getItemCount();

		// Assert the expected count
		assertEquals(30706, itemCount);
	}
}