package reciter.database.dynamodb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.ArticleProvenance;

@ExtendWith(MockitoExtension.class)
public class ArticleProvenanceRepositoryTest {

    @Mock
    private ArticleProvenanceRepository articleProvenanceRepository;

    private ArticleProvenance provenance1;
    private ArticleProvenance provenance2;

    @BeforeEach
    public void setUp() {
        provenance1 = new ArticleProvenance();
        provenance1.setUid("uid1");
        provenance1.setArticleId("article1");

        provenance2 = new ArticleProvenance();
        provenance2.setUid("uid2");
        provenance2.setArticleId("article2");
    }

    @Test
    public void testFindByIdWithConsistentRead_Found() {
        when(articleProvenanceRepository.findByIdWithConsistentRead("uid1", "article1"))
                .thenReturn(Optional.of(provenance1));

        Optional<ArticleProvenance> result =
                articleProvenanceRepository.findByIdWithConsistentRead("uid1", "article1");

        verify(articleProvenanceRepository).findByIdWithConsistentRead("uid1", "article1");
        assertTrue(result.isPresent());
        assertEquals("uid1", result.get().getUid());
        assertEquals("article1", result.get().getArticleId());
    }

    @Test
    public void testFindByIdWithConsistentRead_NotFound() {
        when(articleProvenanceRepository.findByIdWithConsistentRead("uid_missing", "article_missing"))
                .thenReturn(Optional.empty());

        Optional<ArticleProvenance> result =
                articleProvenanceRepository.findByIdWithConsistentRead("uid_missing", "article_missing");

        verify(articleProvenanceRepository).findByIdWithConsistentRead("uid_missing", "article_missing");
        assertFalse(result.isPresent());
    }

    @Test
    public void testUpsertRetrievalProvenance() {
        doNothing().when(articleProvenanceRepository)
                .upsertRetrievalProvenance("uid1", "article1", "S1", "pubmed", 1700000000L);

        articleProvenanceRepository.upsertRetrievalProvenance("uid1", "article1", "S1", "pubmed", 1700000000L);

        verify(articleProvenanceRepository)
                .upsertRetrievalProvenance("uid1", "article1", "S1", "pubmed", 1700000000L);
    }

    @Test
    public void testWritePmUiSearchRecord() {
        doNothing().when(articleProvenanceRepository)
                .writePmUiSearchRecord("uid1", "article1", "pmUiSearch", "pubmed", 1700000000L);

        articleProvenanceRepository.writePmUiSearchRecord("uid1", "article1", "pmUiSearch", "pubmed", 1700000000L);

        verify(articleProvenanceRepository)
                .writePmUiSearchRecord("uid1", "article1", "pmUiSearch", "pubmed", 1700000000L);
    }

    @Test
    public void testApplyD11Update_NewSource() {
        doNothing().when(articleProvenanceRepository)
                .applyD11Update("uid1", "article1", "newSrc", null, 1700000000L);

        articleProvenanceRepository.applyD11Update("uid1", "article1", "newSrc", null, 1700000000L);

        verify(articleProvenanceRepository)
                .applyD11Update("uid1", "article1", "newSrc", null, 1700000000L);
    }

    @Test
    public void testApplyD11Update_DifferentSource() {
        doNothing().when(articleProvenanceRepository)
                .applyD11Update("uid1", "article1", "newSrc", "oldSrc", 1700000000L);

        articleProvenanceRepository.applyD11Update("uid1", "article1", "newSrc", "oldSrc", 1700000000L);

        verify(articleProvenanceRepository)
                .applyD11Update("uid1", "article1", "newSrc", "oldSrc", 1700000000L);
    }

    @Test
    public void testApplyD11Update_SameSource() {
        doNothing().when(articleProvenanceRepository)
                .applyD11Update("uid1", "article1", "sameSrc", "sameSrc", 1700000000L);

        articleProvenanceRepository.applyD11Update("uid1", "article1", "sameSrc", "sameSrc", 1700000000L);

        verify(articleProvenanceRepository)
                .applyD11Update("uid1", "article1", "sameSrc", "sameSrc", 1700000000L);
    }
}