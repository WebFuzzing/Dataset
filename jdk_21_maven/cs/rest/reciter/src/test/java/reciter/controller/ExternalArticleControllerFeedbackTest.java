package reciter.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import reciter.controller.ExternalArticleController.FeedbackRequest;
import reciter.database.dynamodb.model.ExternalArticle;
import reciter.database.dynamodb.model.FeedbackLog;
import reciter.model.identity.Identity;
import reciter.service.AnalysisService;
import reciter.service.ExternalArticleService;
import reciter.service.FeedbackLogService;
import reciter.service.IdentityService;
import reciter.service.dynamo.IDynamoDbGoldStandardService;

/** Covers PATCH /reciter/external-article/feedback (ACCEPTED/REJECTED/PENDING via FeedbackLog). */
@ExtendWith(MockitoExtension.class)
public class ExternalArticleControllerFeedbackTest {

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

    private ExternalArticle existing;

    @BeforeEach
    public void setUp() {
        existing = new ExternalArticle();
        existing.setUid("dnr4021");
        existing.setArticleId("SCOPUS:85142207731");
        existing.setSuppressed(Boolean.FALSE);
    }

    private FeedbackRequest request(String action, String actor) {
        FeedbackRequest body = new FeedbackRequest();
        body.setUid("dnr4021");
        body.setArticleId("SCOPUS:85142207731");
        body.setAction(action);
        body.setActorPersonIdentifier(actor);
        return body;
    }

    private void happyPathStubs() {
        when(identityService.findByUid("dnr4021")).thenReturn(new Identity());
        when(externalArticleService.find("dnr4021", "SCOPUS:85142207731")).thenReturn(existing);
        when(feedbackLogService.recordAction(any(FeedbackLog.class))).thenReturn(true);
    }

    @Test
    public void rejectedSuppressesRowAndLogsFirst() {
        happyPathStubs();

        FeedbackRequest body = request("REJECTED", "dnr4021");
        body.setNote("not mine");
        ResponseEntity<Object> response = externalArticleController.recordExternalArticleFeedback(body);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Boolean.TRUE, existing.getSuppressed());

        // The authoritative history row must land before the suppressed-cache flip.
        InOrder order = inOrder(feedbackLogService, externalArticleService);
        ArgumentCaptor<FeedbackLog> logged = ArgumentCaptor.forClass(FeedbackLog.class);
        order.verify(feedbackLogService).recordAction(logged.capture());
        order.verify(externalArticleService).save(existing);
        assertEquals("REJECTED", logged.getValue().getFeedback());
        assertEquals("dnr4021", logged.getValue().getActorPersonIdentifier());
        assertEquals("not mine", logged.getValue().getNote());
        assertEquals(0, logged.getValue().getCuratedBy());
    }

    @Test
    public void rejectedTakesOwnershipOfSupersedeSuppression() {
        existing.setSuppressed(Boolean.TRUE);
        existing.setSupersededByPmid(12345L);
        happyPathStubs();

        ResponseEntity<Object> response =
                externalArticleController.recordExternalArticleFeedback(request("REJECTED", "dnr4021"));

        // supersededByPmid cleared so the reconciler's auto-un-suppress can't
        // resurrect an explicitly rejected row once PMID 12345 leaves the gold standard.
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Boolean.TRUE, existing.getSuppressed());
        assertNull(existing.getSupersededByPmid());
        verify(externalArticleService).save(existing);
    }

    @Test
    public void acceptedUnsuppressesRow() {
        existing.setSuppressed(Boolean.TRUE);
        happyPathStubs();

        // lower-case on the wire should still work
        ResponseEntity<Object> response =
                externalArticleController.recordExternalArticleFeedback(request("accepted", "curator99"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Boolean.FALSE, existing.getSuppressed());
        verify(externalArticleService).save(existing);
    }

    @Test
    public void acceptedLeavesSupersedeSuppressionAlone() {
        existing.setSuppressed(Boolean.TRUE);
        existing.setSupersededByPmid(12345L);
        happyPathStubs();

        ResponseEntity<Object> response =
                externalArticleController.recordExternalArticleFeedback(request("ACCEPTED", "curator99"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(Boolean.TRUE, existing.getSuppressed());
        verify(externalArticleService, never()).save(any());
        verify(feedbackLogService).recordAction(any(FeedbackLog.class));
    }

    @Test
    public void pendingLogsWithoutTouchingRow() {
        happyPathStubs();

        ResponseEntity<Object> response =
                externalArticleController.recordExternalArticleFeedback(request("PENDING", "curator99"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(externalArticleService, never()).save(any());
        verify(feedbackLogService).recordAction(any(FeedbackLog.class));
    }

    @Test
    public void feedbackOnNeverAddedCandidateStillLogs() {
        when(identityService.findByUid("dnr4021")).thenReturn(new Identity());
        when(externalArticleService.find("dnr4021", "SCOPUS:85142207731")).thenReturn(null);
        when(feedbackLogService.recordAction(any(FeedbackLog.class))).thenReturn(true);

        ResponseEntity<Object> response =
                externalArticleController.recordExternalArticleFeedback(request("REJECTED", "curator99"));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(externalArticleService, never()).save(any());
        verify(feedbackLogService).recordAction(any(FeedbackLog.class));
        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertNotNull(body);
        assertEquals(Boolean.FALSE, body.get("rowExists"));
    }

    @Test
    public void failedLogWriteFailsRequestBeforeAnyFlip() {
        when(identityService.findByUid("dnr4021")).thenReturn(new Identity());
        when(externalArticleService.find("dnr4021", "SCOPUS:85142207731")).thenReturn(existing);
        when(feedbackLogService.recordAction(any(FeedbackLog.class))).thenReturn(false);

        ResponseEntity<Object> response =
                externalArticleController.recordExternalArticleFeedback(request("REJECTED", "dnr4021"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(Boolean.FALSE, existing.getSuppressed());
        verify(externalArticleService, never()).save(any());
    }

    @Test
    public void unknownUidIsNotFoundAndNothingLogged() {
        when(identityService.findByUid("dnr4021")).thenReturn(null);

        ResponseEntity<Object> response =
                externalArticleController.recordExternalArticleFeedback(request("REJECTED", "curator99"));

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(feedbackLogService, never()).recordAction(any());
    }

    @Test
    public void invalidActionIsBadRequest() {
        ResponseEntity<Object> response =
                externalArticleController.recordExternalArticleFeedback(request("DELETE_EVERYTHING", "curator99"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(feedbackLogService, never()).recordAction(any());
    }

    @Test
    public void missingActorIsBadRequest() {
        ResponseEntity<Object> response =
                externalArticleController.recordExternalArticleFeedback(request("REJECTED", "  "));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(feedbackLogService, never()).recordAction(any());
        verify(externalArticleService, never()).find(any(), any());
    }

    @Test
    public void malformedArticleIdIsBadRequest() {
        FeedbackRequest body = request("REJECTED", "curator99");
        body.setArticleId("85142207731"); // no source prefix

        ResponseEntity<Object> response = externalArticleController.recordExternalArticleFeedback(body);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(feedbackLogService, never()).recordAction(any());
    }
}
