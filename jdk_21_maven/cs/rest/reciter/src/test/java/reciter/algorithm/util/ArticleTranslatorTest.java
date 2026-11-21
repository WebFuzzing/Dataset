package reciter.algorithm.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.engine.StrategyParameters;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleKeywords;
import reciter.model.pubmed.MedlineCitationArticleAuthor;
import reciter.model.pubmed.MedlineCitationCommentsCorrections;
import reciter.model.pubmed.MedlineCitationKeyword;
import reciter.model.pubmed.MedlineCitationKeywordList;
import reciter.model.pubmed.PubMedArticle;

@ExtendWith(MockitoExtension.class)
public class ArticleTranslatorTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private PubMedArticle pubmedArticle;

    @BeforeEach
    public void setUp() {
        // lenient: not every test drives translate() through every branch, and some
        // tests override individual stubs with real data.
        lenient().when(pubmedArticle.getPubmeddata()).thenReturn(null);
        lenient().when(pubmedArticle.getMedlinecitation().getArticle().getJournal()).thenReturn(null);
        lenient().when(pubmedArticle.getMedlinecitation().getArticle().getPublicationtypelist()).thenReturn(null);
        lenient().when(pubmedArticle.getMedlinecitation().getArticle().getPublicationAbstract()).thenReturn(null);
        lenient().when(pubmedArticle.getMedlinecitation().getArticle().getAuthorlist()).thenReturn(null);
        lenient().when(pubmedArticle.getMedlinecitation().getKeywordlist()).thenReturn(null);
        lenient().when(pubmedArticle.getMedlinecitation().getMeshheadinglist()).thenReturn(null);
        lenient().when(pubmedArticle.getMedlinecitation().getArticle().getArticledate()).thenReturn(null);
        lenient().when(pubmedArticle.getMedlinecitation().getArticle().getGrantlist()).thenReturn(null);
        lenient().when(pubmedArticle.getMedlinecitation().getArticle().getPagination()).thenReturn(null);
        lenient().when(pubmedArticle.getMedlinecitation().getArticle().getElocationid()).thenReturn(null);
    }

    @Test
    public void translateSkipsNonNumericCommentsCorrectionsAndKeepsNumeric() {
        MedlineCitationCommentsCorrections pmcEntry = new MedlineCitationCommentsCorrections();
        pmcEntry.setPmid("PMC7149824");
        pmcEntry.setReftype("ErratumIn");

        MedlineCitationCommentsCorrections textEntry = new MedlineCitationCommentsCorrections();
        textEntry.setPmid("Zhu X., Han J. (2020). A novel circular RNA. BMC Cancer 20, 1190.");

        MedlineCitationCommentsCorrections numericEntry = new MedlineCitationCommentsCorrections();
        numericEntry.setPmid("32648546");
        numericEntry.setReftype("ErratumFor");

        List<MedlineCitationCommentsCorrections> ccList = Arrays.asList(pmcEntry, textEntry, numericEntry);
        when(pubmedArticle.getMedlinecitation().getCommentscorrectionslist()).thenReturn(ccList);

        ReCiterArticle result = ArticleTranslator.translate(pubmedArticle, null, "", null);

        assertEquals(1, result.getCommentsCorrectionsPmids().size());
        assertTrue(result.getCommentsCorrectionsPmids().contains(32648546L));
        assertEquals("ErratumFor", result.getCommentsCorrectionsRefTypes().get(32648546L));
    }

    @Test
    public void translateToleratesBlankAuthorNamePartsAndKeepsAuthors() {
        // AuthorName's constructor substrings any non-null firstName/middleName for its
        // initial, so a blank one used to crash translate with
        // StringIndexOutOfBoundsException (observed for pstuebge, kcm9013, xim9010).
        MedlineCitationArticleAuthor blankForeName = new MedlineCitationArticleAuthor();
        blankForeName.setLastname("Stuebgen");
        blankForeName.setForename("");

        MedlineCitationArticleAuthor whitespaceInitials = new MedlineCitationArticleAuthor();
        whitespaceInitials.setLastname("Meyer");
        whitespaceInitials.setInitials(" "); // no forename, so initials become the firstName

        MedlineCitationArticleAuthor normal = new MedlineCitationArticleAuthor();
        normal.setLastname("Bracken");
        normal.setForename("Clay");

        when(pubmedArticle.getMedlinecitation().getArticle().getAuthorlist())
                .thenReturn(Arrays.asList(blankForeName, whitespaceInitials, normal));

        StrategyParameters strategyParameters = new StrategyParameters();
        strategyParameters.setNameExcludedSuffixes("Jr,MD PhD,MD-PhD,PhD,MD,III,II,Sr");

        ReCiterArticle result = ArticleTranslator.translate(pubmedArticle, null, "Wang Y, Wang J", strategyParameters);

        assertEquals(3, result.getArticleCoAuthors().getAuthors().size());
        assertTrue(result.getArticleCoAuthors().getAuthors().stream()
                .anyMatch(author -> "Clay".equals(author.getAuthorName().getFirstName())));
    }

    @Test
    public void translateSkipsNullKeywordEntriesAndKeepsRealOnes() {
        MedlineCitationKeyword realKeyword = new MedlineCitationKeyword();
        realKeyword.setKeyword("neurology");

        MedlineCitationKeyword nullTextKeyword = new MedlineCitationKeyword(); // getKeyword() == null

        MedlineCitationKeywordList keywordList = new MedlineCitationKeywordList();
        keywordList.setKeywordlist(Arrays.asList(null, realKeyword, nullTextKeyword));

        when(pubmedArticle.getMedlinecitation().getKeywordlist()).thenReturn(keywordList);

        ReCiterArticle result = ArticleTranslator.translate(pubmedArticle, null, "", null);

        List<String> keywords = result.getArticleKeywords().getKeywords().stream()
                .map(ReCiterArticleKeywords.Keyword::getKeyword)
                .collect(Collectors.toList());
        assertEquals(Arrays.asList("neurology"), keywords);
    }

    @Test
    public void blankToNullNormalizesBlankAndWhitespaceNameParts() {
        // Covers the middleName path of the AuthorName construction guard: translate
        // itself cannot carry a non-null middleName today (the forename split is
        // commented out), so the normalization is exercised directly.
        assertNull(ArticleTranslator.blankToNull(null));
        assertNull(ArticleTranslator.blankToNull(""));
        assertNull(ArticleTranslator.blankToNull("   "));
        assertEquals("Clay", ArticleTranslator.blankToNull("Clay"));
    }
}
