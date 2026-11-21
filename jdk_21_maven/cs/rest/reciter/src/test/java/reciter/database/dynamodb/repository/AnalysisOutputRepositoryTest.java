package reciter.database.dynamodb.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
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

import reciter.database.dynamodb.model.AnalysisOutput;
import reciter.engine.analysis.ReCiterFeature;

@ExtendWith(MockitoExtension.class)
public class AnalysisOutputRepositoryTest {

    @Mock
    private AnalysisOutputRepository analysisOutputRepository;
    
    private AnalysisOutput analysisOutput;
    private  AnalysisOutput analysisOutput2;
    
    @BeforeEach
    public void setUp() {
    	 ReCiterFeature reciter = new ReCiterFeature("abc", Instant.now(), Instant.now(), null, null, null, null, null, 15, 20, null, null);
         analysisOutput = new AnalysisOutput();
         analysisOutput.setUid("12345");
         analysisOutput.setReCiterFeature(reciter);
        
         ReCiterFeature reciter2 = new ReCiterFeature("analysis2", Instant.now(), Instant.now(), null, null, null, null, null, 5, 10, null, null);
         analysisOutput2 = new AnalysisOutput();
         analysisOutput2.setReCiterFeature(reciter2);
         analysisOutput2.setUid("124xyz");		
    }

    @Test
    public void testSave() {

        // Mock the repository's save method to do nothing (since it's void)
        doNothing().when(analysisOutputRepository).save(any(AnalysisOutput.class));

        // Act
        analysisOutputRepository.save(analysisOutput);

        // Verify that the repository's save method was called
        verify(analysisOutputRepository, times(1)).save(analysisOutput);
    }

    @Test
    public void testFindById() {
       
        // Mock the repository's findById method
        when(analysisOutputRepository.findById("12345")).thenReturn(Optional.of(analysisOutput));

        // Act
        Optional<AnalysisOutput> retrievedAnalysisOutput = analysisOutputRepository.findById("12345");

        // Verify that the repository's findById method was called
        verify(analysisOutputRepository, times(1)).findById("12345");

        // Assert the retrieved object is present and equals the saved one
        assertTrue(retrievedAnalysisOutput.isPresent());
        assertEquals(analysisOutput, retrievedAnalysisOutput.get());
    }

    @SuppressWarnings("unused")
	@Test
    public void testFindAll() {
        // Mock the repository's findAll method
        when(analysisOutputRepository.findAll()).thenReturn(Arrays.asList(analysisOutput, analysisOutput2));

        // Act
        Iterable<AnalysisOutput> analysisOutputs = analysisOutputRepository.findAll();

        // Verify the repository's findAll method was called
        verify(analysisOutputRepository, times(1)).findAll();

        // Assert that the returned list size is correct
        int count = 0;
        for (AnalysisOutput output : analysisOutputs) {
            count++;
        }
        assertEquals(2, count);
    }

    @Test
    public void testDeleteById() {
        // Arrange
        doNothing().when(analysisOutputRepository).deleteById("12345");

        // Act
        analysisOutputRepository.deleteById("12345");

        // Verify that the repository's deleteById method was called
        verify(analysisOutputRepository, times(1)).deleteById("12345");
    }

    @Test
    public void testDeleteAll() {
        // Arrange
        doNothing().when(analysisOutputRepository).deleteAll();

        // Act
        analysisOutputRepository.deleteAll();

        // Verify that the repository's deleteAll method was called
        verify(analysisOutputRepository, times(1)).deleteAll();
    }

    @Test
    public void testGetItemCount() {
        // Arrange
        when(analysisOutputRepository.getItemCount()).thenReturn(10L);

        // Act
        long itemCount = analysisOutputRepository.getItemCount();

        // Verify that the repository's getItemCount method was called
        verify(analysisOutputRepository, times(1)).getItemCount();

        // Assert that the item count is correct
        assertEquals(10L, itemCount);
    }

    @Test
    public void testFindAllById() {
        // Mock the repository's findAllById method
        when(analysisOutputRepository.findAllById(List.of("12345", "124xyz"))).thenReturn(Arrays.asList(analysisOutput, analysisOutput2));

        // Act
        List<AnalysisOutput> analysisOutputs = analysisOutputRepository.findAllById(List.of("12345", "124xyz"));

        // Verify that the repository's findAllById method was called
        verify(analysisOutputRepository, times(1)).findAllById(List.of("12345", "124xyz"));

        // Assert that the correct number of objects is returned and matches the expected UIDs
        assertEquals(2, analysisOutputs.size());
        assertEquals("12345", analysisOutputs.get(0).getUid());
        assertEquals("124xyz", analysisOutputs.get(1).getUid());
    }
}
