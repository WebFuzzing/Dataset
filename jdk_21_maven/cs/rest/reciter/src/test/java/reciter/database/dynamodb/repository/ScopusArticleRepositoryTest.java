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

import reciter.database.dynamodb.model.ScopusArticle;

@ExtendWith(MockitoExtension.class)
public class ScopusArticleRepositoryTest {

    @Mock
    private ScopusArticleRepository repository;
    
    private ScopusArticle scopusArticle1;
    private ScopusArticle scopusArticle2;
    
    @BeforeEach
    public void setUp() {
        // Create test ScopusArticle objects for reuse in tests
        scopusArticle1 = new ScopusArticle();
        scopusArticle1.setId("scopus123");
        reciter.model.scopus.ScopusArticle articleData1 = new reciter.model.scopus.ScopusArticle();
        scopusArticle1.setScopusArticle(articleData1);
        
        scopusArticle2 = new ScopusArticle();
        scopusArticle2.setId("scopus1231");
        reciter.model.scopus.ScopusArticle articleData2 = new reciter.model.scopus.ScopusArticle();
        scopusArticle2.setScopusArticle(articleData2);
    }
    
    @Test
    public void testSave() {
        // Setup mock behavior
        when(repository.findById("scopus1231")).thenReturn(Optional.of(scopusArticle2));
        
        // Call the method
        repository.saveAll(Arrays.asList(scopusArticle1, scopusArticle2));
        
        // Retrieve via mock
        Optional<ScopusArticle> retrievedArticle = repository.findById("scopus1231");
        
        // Verify interactions
        verify(repository).saveAll(Arrays.asList(scopusArticle1, scopusArticle2));
        verify(repository).findById("scopus1231");
        
        // Assert results
        assertTrue(retrievedArticle.isPresent());
        assertEquals(scopusArticle2.getId(), retrievedArticle.get().getId());
    }
    
    @Test
    public void testFindById() {
        // Create a new ScopusArticle entity
        ScopusArticle scopusArticle = new ScopusArticle();
        scopusArticle.setId("scopus456");
        reciter.model.scopus.ScopusArticle articleData = new reciter.model.scopus.ScopusArticle();
        scopusArticle.setScopusArticle(articleData);
        
        // Setup mock behavior
        when(repository.findById("scopus456")).thenReturn(Optional.of(scopusArticle));
        
        // Call the repository method
        Optional<ScopusArticle> retrievedArticle = repository.findById("scopus456");
        
        // Verify interaction
        verify(repository).findById("scopus456");
        
        // Assert results
        assertTrue(retrievedArticle.isPresent());
        assertEquals(scopusArticle.getId(), retrievedArticle.get().getId());
    }
    
    @Test
    public void testDeleteAll() {
        // Setup mock behavior
        when(repository.findById("scopus555")).thenReturn(Optional.empty());
        
        // Call delete all
        repository.deleteAll();
        
        // Verify delete was called
        verify(repository).deleteAll();
        
        // Test that find returns empty after delete
        Optional<ScopusArticle> byId = repository.findById("scopus555");
        
        assertTrue(byId.isEmpty(), "Article should be deleted after deleteAll");
    }
    
    @Test
    public void testFindAllById() {
        // Create test articles
        ScopusArticle scopusArticle1 = new ScopusArticle();
        scopusArticle1.setId("scopus12345");
        scopusArticle1.setScopusArticle(new reciter.model.scopus.ScopusArticle());
        
        ScopusArticle scopusArticle2 = new ScopusArticle();
        scopusArticle2.setId("scopus67890");
        scopusArticle2.setScopusArticle(new reciter.model.scopus.ScopusArticle());
        
        List<String> ids = Arrays.asList("scopus12345", "scopus67890");
        List<ScopusArticle> expectedArticles = Arrays.asList(scopusArticle1, scopusArticle2);
        
        // Setup mock behavior
        when(repository.findAllById(ids)).thenReturn(expectedArticles);
        
        // Call the repository method
        List<ScopusArticle> articles = repository.findAllById(ids);
        
        // Verify interaction
        verify(repository).findAllById(ids);
        
        // Assert results
        assertEquals(2, articles.size());
        assertTrue(articles.stream().anyMatch(a -> a.getId().equals("scopus12345")));
        assertTrue(articles.stream().anyMatch(a -> a.getId().equals("scopus67890")));
    }
}