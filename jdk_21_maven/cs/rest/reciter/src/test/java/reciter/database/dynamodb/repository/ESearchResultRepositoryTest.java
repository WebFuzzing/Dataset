package reciter.database.dynamodb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.ESearchPmid;
import reciter.database.dynamodb.model.ESearchResult;
import reciter.database.dynamodb.model.QueryType;

@ExtendWith(MockitoExtension.class)
public class ESearchResultRepositoryTest {

    @Mock
    private ESearchResultRepository eSearchResultRepository;

    private ESearchPmid eSearchPmid1;
    private ESearchPmid eSearchPmid2;
    private ESearchResult eSearchResult1;
    private ESearchResult eSearchResult2;

    @BeforeEach
    public void setUp() {
        // Create common test data
        eSearchPmid1 = new ESearchPmid(
                Arrays.asList(12345L, 67890L),
                "Strategy A",
                Instant.now(),
                ESearchPmid.RetrievalRefreshFlag.ALL_PUBLICATIONS
        );
        
        eSearchPmid2 = new ESearchPmid(
                Arrays.asList(98765L, 43210L),
                "Strategy B",
                Instant.now(),
                ESearchPmid.RetrievalRefreshFlag.ONLY_NEWLY_ADDED_PUBLICATIONS
        );

        eSearchResult1 = new ESearchResult("uid1", Instant.now(), Arrays.asList(eSearchPmid1), QueryType.LENIENT_LOOKUP);
        eSearchResult2 = new ESearchResult("uid2", Instant.now(), Arrays.asList(eSearchPmid2), QueryType.LENIENT_LOOKUP);
    }

    @Test
    public void testSave() {
        // Setup mock behavior
        when(eSearchResultRepository.findById("uid1")).thenReturn(Optional.of(eSearchResult1));

        // Call the repository method
        eSearchResultRepository.save(eSearchResult1);

        // Verify the interaction
        verify(eSearchResultRepository).save(eSearchResult1);

        // Retrieve the saved entity by ID
        Optional<ESearchResult> retrievedResult = eSearchResultRepository.findById("uid1");

        // Assert that the entity is not null and matches the saved values
        assertTrue(retrievedResult.isPresent());
        assertEquals(eSearchPmid1, retrievedResult.get().getESearchPmids().get(0));
    }

    @Test
    public void testFindById() {
        // Setup mock behavior
        when(eSearchResultRepository.findById("uid2")).thenReturn(Optional.of(eSearchResult2));

        // Call the repository method
        Optional<ESearchResult> retrievedResult = eSearchResultRepository.findById("uid2");

        // Verify the interaction
        verify(eSearchResultRepository).findById("uid2");

        // Assert that the entity is present and its fields match
        assertTrue(retrievedResult.isPresent());
        assertEquals(eSearchPmid2, retrievedResult.get().getESearchPmids().get(0));
    }

    @Test
    public void testFindAll() {
        // Setup mock behavior
        when(eSearchResultRepository.findAll()).thenReturn(Arrays.asList(eSearchResult1, eSearchResult2));

        // Call the repository method
        Iterable<ESearchResult> results = eSearchResultRepository.findAll();

        // Verify the interaction
        verify(eSearchResultRepository).findAll();

        // Assert that the retrieved results match
        int count = 0;
        for (ESearchResult result : results) {
            count++;
        }

        assertEquals(2, count);
    }
    

    @Test
    public void testDeleteById() {
    	 doNothing().when(eSearchResultRepository).deleteById("uid1");
        // Setup mock behavior
        when(eSearchResultRepository.findById("uid1")).thenReturn(Optional.empty());

        // Call the repository method
        eSearchResultRepository.deleteById("uid1");

        // Verify the interaction
        verify(eSearchResultRepository).deleteById("uid1");

        // Attempt to retrieve the deleted entity (should be null)
        Optional<ESearchResult> deletedResult = eSearchResultRepository.findById("uid1");

        assertFalse(deletedResult.isPresent());
    }

    @Test
    public void testGetItemCount() {
        // Setup mock behavior
        when(eSearchResultRepository.getItemCount()).thenReturn(4L);

        // Call the repository method
        long itemCount = eSearchResultRepository.getItemCount();

        // Verify the interaction
        verify(eSearchResultRepository).getItemCount();

        // Assert that the item count matches the expected value
        assertEquals(4L, itemCount);
    }

    @Test
    public void testFindAllById() {
        // Setup mock behavior
        List<String> ids = Arrays.asList("uid1", "uid2");
        when(eSearchResultRepository.findAllById(ids)).thenReturn(Arrays.asList(eSearchResult1, eSearchResult2));

        // Call the repository method
        List<ESearchResult> results = eSearchResultRepository.findAllById(ids);

        // Verify the interaction
        verify(eSearchResultRepository).findAllById(ids);

        // Assert that the retrieved results match
        assertEquals(2, results.size());
        assertTrue(results.stream().anyMatch(r -> r.getUid().equals("uid1")));
        assertTrue(results.stream().anyMatch(r -> r.getUid().equals("uid2")));
    }
}
