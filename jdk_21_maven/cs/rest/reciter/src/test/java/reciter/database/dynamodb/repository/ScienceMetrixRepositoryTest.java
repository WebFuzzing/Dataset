package reciter.database.dynamodb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.ScienceMetrix;

@ExtendWith(MockitoExtension.class)
public class ScienceMetrixRepositoryTest {

    @Mock
    private ScienceMetrixRepository scienceMetrixRepository;
    
    private ScienceMetrix scienceMetrix1;
    private ScienceMetrix scienceMetrix2;
    
    @BeforeEach
    public void setUp() {
        // Create common test data
        scienceMetrix1 = new ScienceMetrix(12345L, "1234-5678", "2345-6789", "Journal of Science", "1",
                "Science", "Physics", "Quantum Mechanics");
                
        scienceMetrix2 = new ScienceMetrix(12346L, "1234-5679", "2345-6790", "Journal of Research", "2",
                "Technology", "Engineering", "Machine Learning");
    }

    @Test
    public void testSave() {
        // Setup mock behavior
        when(scienceMetrixRepository.findBySmsid(12345L)).thenReturn(scienceMetrix1);
        
        // Save the ScienceMetrix object
        scienceMetrixRepository.saveAll(Arrays.asList(scienceMetrix1));
        
        // Retrieve it by SMID (primary key)
        ScienceMetrix retrieved = scienceMetrixRepository.findBySmsid(12345L);
        
        // Verify interactions
        verify(scienceMetrixRepository).saveAll(Arrays.asList(scienceMetrix1));
        verify(scienceMetrixRepository).findBySmsid(12345L);
        
        // Assert that the saved object is the same as the retrieved one
        assertNotNull(retrieved);
        assertEquals(scienceMetrix1.getSmsid(), retrieved.getSmsid());
        assertEquals(scienceMetrix1.getEissn(), retrieved.getEissn());
        assertEquals(scienceMetrix1.getIssn(), retrieved.getIssn());
    }

    @Test
    public void testFindBySmsid() {
        // Setup mock behavior
        when(scienceMetrixRepository.findBySmsid(12346L)).thenReturn(scienceMetrix2);
        
        // Call the repository method
        ScienceMetrix retrieved = scienceMetrixRepository.findBySmsid(12346L);
        
        // Verify interaction
        verify(scienceMetrixRepository).findBySmsid(12346L);
        
        // Assert that the retrieved ScienceMetrix object is not null and matches the expected one
        assertNotNull(retrieved);
        assertEquals(scienceMetrix2.getSmsid(), retrieved.getSmsid());
    }

    @Test
    public void testFindByEissn() {
        // Create a ScienceMetrix object for this test
        ScienceMetrix scienceMetrix = new ScienceMetrix(12347L, "1234-5680", "2345-6801", "Journal of Chemistry", "3",
                "Chemistry", "Organic Chemistry", "Catalysis");
                
        // Setup mock behavior
        when(scienceMetrixRepository.findByEissn("1234-5680")).thenReturn(scienceMetrix);
        
        // Call the repository method
        ScienceMetrix retrieved = scienceMetrixRepository.findByEissn("1234-5680");
        
        // Verify interaction
        verify(scienceMetrixRepository).findByEissn("1234-5680");
        
        // Assert that the retrieved object matches the expected one
        assertNotNull(retrieved);
        assertEquals(scienceMetrix.getEissn(), retrieved.getEissn());
    }

    @Test
    public void testFindByIssn() {
        // Create a ScienceMetrix object for this test
        ScienceMetrix scienceMetrix = new ScienceMetrix(12348L, "1234-5681", "2345-6812", "Journal of Biology", "4",
                "Biology", "Genetics", "Evolutionary Biology");
                
        // Setup mock behavior
        when(scienceMetrixRepository.findByIssn("2345-6812")).thenReturn(scienceMetrix);
        
        // Call the repository method
        ScienceMetrix retrieved = scienceMetrixRepository.findByIssn("2345-6812");
        
        // Verify interaction
        verify(scienceMetrixRepository).findByIssn("2345-6812");
        
        // Assert that the retrieved object matches the expected one
        assertNotNull(retrieved);
        assertEquals(scienceMetrix.getIssn(), retrieved.getIssn());
    }

    @Test
    public void testFindAll() {
        // Create ScienceMetrix objects for this test
        ScienceMetrix scienceMetrix1 = new ScienceMetrix(123491L, "1234-5682", "2345-6823", "Journal of Mathematics",
                "5", "Mathematics", "Statistics", "Probability Theory");

        ScienceMetrix scienceMetrix2 = new ScienceMetrix(123501L, "1234-5683", "2345-6834",
                "Journal of Computer Science", "6", "Computer Science", "Artificial Intelligence", "Machine Learning");
                
        List<ScienceMetrix> allScienceMetrix = Arrays.asList(scienceMetrix1, scienceMetrix2);
        
        // Setup mock behavior
        when(scienceMetrixRepository.findAll()).thenReturn(allScienceMetrix);
        when(scienceMetrixRepository.count()).thenReturn((long) allScienceMetrix.size());
        
        // Call the repository methods
        List<ScienceMetrix> retrieved = scienceMetrixRepository.findAll();
        long count = scienceMetrixRepository.count();
        
        // Verify interactions
        verify(scienceMetrixRepository).findAll();
        verify(scienceMetrixRepository).count();
        
        // Assert that the size of the list matches the expected count
        assertEquals(count, retrieved.size());
    }

}
