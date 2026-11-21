package reciter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import reciter.database.dynamodb.model.ExternalArticle;
import reciter.model.identity.Identity;
import reciter.service.AnalysisService;
import reciter.service.ExternalArticleService;
import reciter.service.FeedbackLogService;
import reciter.service.IdentityService;
import reciter.service.dynamo.IDynamoDbGoldStandardService;

/** CROSSREF:<doi> ids on POST /reciter/external-article/by/uid. */
@ExtendWith(MockitoExtension.class)
public class ExternalArticleControllerCrossrefTest {

    private static final String DOI = "10.1016/j.surg.2026.110815";

    @Mock
    private ExternalArticleService externalArticleService;
    @Mock
    private IDynamoDbGoldStandardService dynamoDbGoldStandardService;
    @Mock
    private AnalysisService analysisService;
    @Mock
    private IdentityService identityService;
    @Mock
    private FeedbackLogService feedbackLogService;

    @InjectMocks
    private ExternalArticleController externalArticleController;

    private ExternalArticle crossref(String articleId) {
        ExternalArticle article = new ExternalArticle();
        article.setArticleId(articleId);
        article.setTitle("A Crossref-only paper");
        article.setPubDate("2026");
        return article;
    }

    @Test
    public void articleIdShapes() {
        assertNull(ExternalArticleController.articleIdError("CROSSREF:" + DOI));
        assertNull(ExternalArticleController.articleIdError("CROSSREF:10.1002/(sici)1097-0258(19980815)17:15<1661::aid-sim968>3.0.co;2-2"));
        assertNull(ExternalArticleController.articleIdError("OPENALEX:W2741809807"));
        assertNotNull(ExternalArticleController.articleIdError("CROSSREF:" + DOI.toUpperCase()));
        assertNotNull(ExternalArticleController.articleIdError("CROSSREF:https://doi.org/" + DOI));
        assertNotNull(ExternalArticleController.articleIdError("CROSSREF:11.1016/x"));
        assertNotNull(ExternalArticleController.articleIdError("CROSSREF:10.12/x"));
        assertNotNull(ExternalArticleController.articleIdError("CROSSREF:10.1016/"));

        String atLimit = "CROSSREF:10.1234/" + "a".repeat(96 - "CROSSREF:10.1234/".length());
        assertEquals(96, atLimit.length());
        assertNull(ExternalArticleController.articleIdError(atLimit));
        String tooLong = atLimit + "b";
        assertTrue(ExternalArticleController.articleIdError(tooLong).contains("longer than 96"));
    }

    @Test
    public void addPinsDoiAndSourceTypeFromId() {
        when(identityService.findByUid("dnr4021")).thenReturn(new Identity());
        when(externalArticleService.findByUid("dnr4021")).thenReturn(Collections.emptyList());

        ExternalArticle article = crossref("CROSSREF:" + DOI);
        article.setDoi("https://doi.org/10.9999/something-else");
        ResponseEntity<Object> response = externalArticleController.addExternalArticle("dnr4021", false, article);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals("CROSSREF", article.getSourceType());
        assertEquals(DOI, article.getDoi());
        verify(externalArticleService).save(article);
    }

    @Test
    public void addOfDoiAlreadyAddedFromOpenAlexIsBlocked() {
        ExternalArticle openalex = new ExternalArticle();
        openalex.setUid("dnr4021");
        openalex.setArticleId("OPENALEX:W999");
        openalex.setSourceType("OPENALEX");
        openalex.setDoi("https://doi.org/" + DOI.toUpperCase());
        openalex.setTitle("Same work, different title casing");
        when(identityService.findByUid("dnr4021")).thenReturn(new Identity());
        when(externalArticleService.findByUid("dnr4021")).thenReturn(Collections.singletonList(openalex));

        // No doi in the body: the id alone must be enough to catch the duplicate.
        ResponseEntity<Object> response = externalArticleController.addExternalArticle("dnr4021", true,
                crossref("CROSSREF:" + DOI));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("BLOCKED", body.get("status"));
        verify(externalArticleService, never()).save(any());
    }

    @Test
    public void tooLongCrossrefIdIsInvalid() {
        String tooLong = "CROSSREF:10.1234/" + "a".repeat(80);
        ResponseEntity<Object> response = externalArticleController.addExternalArticle("dnr4021", false,
                crossref(tooLong));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertEquals("INVALID", body.get("status"));
        verify(identityService, never()).findByUid(any());
    }
}
