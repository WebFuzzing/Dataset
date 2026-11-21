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

import reciter.database.dynamodb.model.InstitutionAfid;

@ExtendWith(MockitoExtension.class)
public class DynamoDbInstitutionAfidRepositoryTest {

    @Mock
    private DynamoDbInstitutionAfidRepository institutionAfidRepository;

    private InstitutionAfid institutionAfid1;
    private InstitutionAfid institutionAfid2;

    @BeforeEach
    public void setUp() {
        // Create common test data
        institutionAfid1 = new InstitutionAfid("University A", Arrays.asList("AFID1", "AFID2"));
        institutionAfid2 = new InstitutionAfid("University B", Arrays.asList("AFID3", "AFID4"));
    }

    @Test
    public void testSave() {
        // Setup mock behavior
        when(institutionAfidRepository.findById("University A")).thenReturn(Optional.of(institutionAfid1));

        // Call the repository method
        institutionAfidRepository.save(institutionAfid1);

        // Verify the interaction
        verify(institutionAfidRepository).save(institutionAfid1);

        // Retrieve the saved entity by ID
        Optional<InstitutionAfid> retrievedInstitutionAfid = institutionAfidRepository.findById("University A");

        // Assert that the entity is not null and matches the saved values
        assertTrue(retrievedInstitutionAfid.isPresent());
        assertEquals(institutionAfid1, retrievedInstitutionAfid.get());
    }

    @Test
    public void testFindById() {
        // Setup mock behavior
        when(institutionAfidRepository.findById("University B")).thenReturn(Optional.of(institutionAfid2));

        // Call the repository method
        Optional<InstitutionAfid> retrievedInstitutionAfid = institutionAfidRepository.findById("University B");

        // Verify the interaction
        verify(institutionAfidRepository).findById("University B");

        // Assert that the entity is present and its fields match
        assertTrue(retrievedInstitutionAfid.isPresent());
        assertEquals(institutionAfid2, retrievedInstitutionAfid.get());
    }

    @Test
    public void testFindAll() {
        // Setup mock behavior
        when(institutionAfidRepository.findAll()).thenReturn(Arrays.asList(institutionAfid1, institutionAfid2));

        // Call the repository method
        Iterable<InstitutionAfid> institutionAfids = institutionAfidRepository.findAll();

        // Verify the interaction
        verify(institutionAfidRepository).findAll();

        // Assert that the retrieved results match
        int count = 0;
        for (InstitutionAfid ia : institutionAfids) {
            count++;
        }

        assertEquals(2, count);
    }

    @Test
    public void testGetItemCount() {
        // Setup mock behavior
        when(institutionAfidRepository.count()).thenReturn(4L);

        // Call the repository method
        long itemCount = institutionAfidRepository.count();

        // Verify the interaction
        verify(institutionAfidRepository).count();

        // Assert that the item count is correct
        assertEquals(4L, itemCount);
    }
}
