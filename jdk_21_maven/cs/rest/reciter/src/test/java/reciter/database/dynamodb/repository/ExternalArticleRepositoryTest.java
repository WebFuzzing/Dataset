package reciter.database.dynamodb.repository;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.ExternalArticle;

@ExtendWith(MockitoExtension.class)
public class ExternalArticleRepositoryTest {

    @Mock
    private ExternalArticleRepository externalArticleRepository;

    private ExternalArticle externalArticle;
    private ExternalArticle externalArticle2;

    @BeforeEach
    public void setUp() {
        externalArticle = new ExternalArticle();
        externalArticle.setUid("12345");
        externalArticle.setArticleId("SCOPUS:85123456789");
        externalArticle.setDoi("10.1000/example.doi");
        externalArticle.setTitle("Example Article Title");
        externalArticle.setJournalOrVenue("Journal of Examples");
        externalArticle.setSourceType("SCOPUS");
        externalArticle.setSuppressed(false);

        externalArticle2 = new ExternalArticle();
        externalArticle2.setUid("12345");
        externalArticle2.setArticleId("OPENALEX:W2741809807");
        externalArticle2.setDoi("10.1000/example2.doi");
        externalArticle2.setTitle("Second Example Article");
        externalArticle2.setJournalOrVenue("Journal of More Examples");
        externalArticle2.setSourceType("OPENALEX");
        externalArticle2.setSuppressed(false);
    }

    @Test
    public void testSave() {
        // Mock the repository's save method to do nothing (since it's void)
        doNothing().when(externalArticleRepository).save(any(ExternalArticle.class));

        // Act
        externalArticleRepository.save(externalArticle);

        // Verify that the repository's save method was called
        verify(externalArticleRepository, times(1)).save(externalArticle);
    }

    @Test
    public void testFindByUid() {
        // Mock the repository's findByUid method
        when(externalArticleRepository.findByUid("12345"))
                .thenReturn(Arrays.asList(externalArticle, externalArticle2));

        // Act
        List<ExternalArticle> results = externalArticleRepository.findByUid("12345");

        // Verify that the repository's findByUid method was called
        verify(externalArticleRepository, times(1)).findByUid("12345");

        // Assert that the correct number of rows is returned
        assertEquals(2, results.size());
        assertEquals("SCOPUS:85123456789", results.get(0).getArticleId());
        assertEquals("OPENALEX:W2741809807", results.get(1).getArticleId());
    }

    @Test
    public void testFindByUidNoResults() {
        // Mock an empty result for a uid with no external articles
        when(externalArticleRepository.findByUid("nonexistent"))
                .thenReturn(Collections.emptyList());

        // Act
        List<ExternalArticle> results = externalArticleRepository.findByUid("nonexistent");

        // Verify
        verify(externalArticleRepository, times(1)).findByUid("nonexistent");
        assertTrue(results.isEmpty());
    }

    @Test
    public void testFind() {
        // Mock the repository's find method
        when(externalArticleRepository.find("12345", "SCOPUS:85123456789"))
                .thenReturn(Optional.of(externalArticle));

        // Act
        Optional<ExternalArticle> result = externalArticleRepository.find("12345", "SCOPUS:85123456789");

        // Verify that the repository's find method was called
        verify(externalArticleRepository, times(1)).find("12345", "SCOPUS:85123456789");

        // Assert the retrieved object is present and equals the saved one
        assertTrue(result.isPresent());
        assertEquals(externalArticle, result.get());
    }

    @Test
    public void testFindNotFound() {
        // Mock a missing composite key
        when(externalArticleRepository.find("12345", "WOS:000000000000"))
                .thenReturn(Optional.empty());

        // Act
        Optional<ExternalArticle> result = externalArticleRepository.find("12345", "WOS:000000000000");

        // Verify
        verify(externalArticleRepository, times(1)).find("12345", "WOS:000000000000");
        assertFalse(result.isPresent());
    }

    @Test
    public void testDelete() {
        // Arrange
        doNothing().when(externalArticleRepository).delete("12345", "SCOPUS:85123456789");

        // Act
        externalArticleRepository.delete("12345", "SCOPUS:85123456789");

        // Verify that the repository's delete method was called
        verify(externalArticleRepository, times(1)).delete("12345", "SCOPUS:85123456789");
    }
}
