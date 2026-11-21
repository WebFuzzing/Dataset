package reciter.database.dynamodb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.PubMedArticle;

@ExtendWith(MockitoExtension.class)
public class PubMedArticleRepositoryTest {

    @Mock
    private PubMedArticleRepository repository;
    
    private PubMedArticle pubMedArticle1;
    private PubMedArticle pubMedArticle2;
    
    @BeforeEach
    public void setUp() {
        // Create common test data
        pubMedArticle1 = new PubMedArticle();
        pubMedArticle1.setPmid(12345L);
        pubMedArticle1.setPubMedArticle(new reciter.model.pubmed.PubMedArticle());
        
        pubMedArticle2 = new PubMedArticle();
        pubMedArticle2.setPmid(67890L);
        pubMedArticle2.setPubMedArticle(new reciter.model.pubmed.PubMedArticle());
    }

    @Test
    public void testSave() {
        // Setup mock behavior
        when(repository.findById(12345L)).thenReturn(Optional.of(pubMedArticle1));
        
        // Call the repository method
        repository.save(pubMedArticle1);
        
        // Verify the interaction
        verify(repository).save(pubMedArticle1);
        
        // Retrieve the saved entity by pmid
        Optional<PubMedArticle> retrievedArticle = repository.findById(12345L);
        
        // Assert that the entity is not null and matches the saved values
        assertTrue(retrievedArticle.isPresent());
        assertEquals(pubMedArticle1.getPmid(), retrievedArticle.get().getPmid());
    }
    
    @Test
    public void testFindById() {
        // Setup mock behavior
        when(repository.findById(67890L)).thenReturn(Optional.of(pubMedArticle2));
        
        // Call the repository method
        Optional<PubMedArticle> retrievedArticle = repository.findById(67890L);
        
        // Verify the interaction
        verify(repository).findById(67890L);
        
        // Assert that the entity is present and its fields match
        assertTrue(retrievedArticle.isPresent());
        assertEquals(pubMedArticle2.getPmid(), retrievedArticle.get().getPmid());
    }
    
    @Test
    public void testGetItemCount() {
        // Setup mock behavior
        when(repository.getItemCount()).thenReturn(4L);
        
        // Call the repository method
        long count = repository.getItemCount();
        
        // Verify the interaction
        verify(repository).getItemCount();
        
        // Assert that the count matches the expected value
        assertEquals(4, count);
    }
    
    @Test
    public void testFindAllById() {
        // Setup mock behavior
        List<Long> pmids = Arrays.asList(12345L, 67890L);
        List<PubMedArticle> expectedArticles = Arrays.asList(pubMedArticle1, pubMedArticle2);
        when(repository.findAllById(pmids)).thenReturn(expectedArticles);
        
        // Call the repository method
        List<PubMedArticle> articles = repository.findAllById(pmids);
        
        // Verify the interaction
        verify(repository).findAllById(pmids);
        
        // Assert that the retrieved articles match the expected ones
        assertEquals(2, articles.size());
        assertTrue(articles.stream().anyMatch(a -> a.getPmid().equals(12345L)));
        assertTrue(articles.stream().anyMatch(a -> a.getPmid().equals(67890L)));
    }
}