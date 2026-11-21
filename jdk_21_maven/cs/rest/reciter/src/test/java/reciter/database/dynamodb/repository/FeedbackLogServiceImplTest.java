package reciter.database.dynamodb.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import reciter.database.dynamodb.model.FeedbackLog;
import reciter.service.FeedbackLogService;
import reciter.service.dynamo.FeedbackLogServiceImpl;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;
import software.amazon.awssdk.services.dynamodb.model.DynamoDbException;

/**
 * Unit tests for {@link FeedbackLogServiceImpl}.
 *
 * <p>Covers schema matching, unique sort-key formatting, and Phase 33-02 
 * idempotency / retry semantics using the repository pattern.
 */
@RunWith(MockitoJUnitRunner.class)
public class FeedbackLogServiceImplTest {

    @Mock
    private FeedbackLogRepository feedbackLogRepository;

    private FeedbackLogServiceImpl service;
    private FeedbackLog logEntry;

    @Before
    public void setUp() {
        service = new FeedbackLogServiceImpl(feedbackLogRepository);
        // Fresh initialization before each test run prevents state pollution
        logEntry = new FeedbackLog();
    }

    private FeedbackLog captureSavedEntry() {
        ArgumentCaptor<FeedbackLog> captor = ArgumentCaptor.forClass(FeedbackLog.class);
        verify(feedbackLogRepository).save(captor.capture());
        return captor.getValue();
    }

    @Test
    public void testRecordAction_ItemHasFullSchema() {
        logEntry.setUid("ajg9004");
        logEntry.setArticleId(String.valueOf(12345L));
        logEntry.setFeedback(FeedbackLogService.Feedback.ACCEPTED.name());
        logEntry.setCuratedBy(3);
        logEntry.setSrc("MAN");
        logEntry.setCreateTimestamp(1700000000L);
        logEntry.setModifyTimestamp(1700000000L);
        
        service.recordAction(logEntry);
         
        FeedbackLog saved = captureSavedEntry();
        assertEquals("ajg9004", saved.getUid());
        assertEquals("12345", saved.getArticleId());
        assertEquals("ACCEPTED", saved.getFeedback());
        assertEquals(3, saved.getCuratedBy());
        assertEquals("MAN", saved.getSrc());
        assertEquals(1700000000L, saved.getCreateTimestamp());
        assertEquals(1700000000L, saved.getModifyTimestamp());
        assertNotNull(saved.getSk());
    }

    @Test
    public void testRecordAction_SkFormat_EpochHashHex() {
        logEntry.setUid("uid1");
        logEntry.setArticleId(String.valueOf(1L));
        logEntry.setFeedback(FeedbackLogService.Feedback.ACCEPTED.name());
        logEntry.setCuratedBy(0);
        logEntry.setSrc("MAN");
        logEntry.setCreateTimestamp(1700000000L);
        logEntry.setModifyTimestamp(1700000000L);
        
        service.recordAction(logEntry);
        
        String sk = captureSavedEntry().getSk();
        // Format assertion: "<epoch>#<8-hex>"
        assertTrue("sk should match epoch#hex format: " + sk, sk.matches("^\\d+#[0-9a-f]{8}$"));
        assertTrue("sk should start with the epoch: " + sk, sk.startsWith("1700000000#"));
    }

    @Test
    public void testRecordAction_FeedbackRejected_WritesRejected() {
        logEntry.setUid("uid1");
        logEntry.setArticleId(String.valueOf(1L));
        logEntry.setFeedback(FeedbackLogService.Feedback.REJECTED.name());
        logEntry.setCreateTimestamp(1700000000L);
        
        service.recordAction(logEntry);
    	
        assertEquals("REJECTED", captureSavedEntry().getFeedback());
    }

    @Test
    public void testRecordAction_FeedbackPending_WritesPending() {
        logEntry.setUid("uid1");
        logEntry.setArticleId(String.valueOf(1L));
        logEntry.setFeedback(FeedbackLogService.Feedback.PENDING.name());
        logEntry.setCreateTimestamp(1700000000L);
        
        service.recordAction(logEntry);
        
        assertEquals("PENDING", captureSavedEntry().getFeedback());
    }

    @Test
    public void testRecordAction_SkCollision_RetriesWithFreshSuffix() {
        // Mocking the new AWS SDK v2 ConditionalCheckFailedException variant via builder hierarchy
        doThrow(ConditionalCheckFailedException.builder().message("collision").build())
                .doNothing() // Succeed on the second attempt invocation
                .when(feedbackLogRepository).save(any(FeedbackLog.class));
        
        logEntry.setUid("uid1");
        logEntry.setArticleId(String.valueOf(1L));
        logEntry.setFeedback(FeedbackLogService.Feedback.ACCEPTED.name());
        logEntry.setCreateTimestamp(1700000000L);
        
        service.recordAction(logEntry);
        
        // Verifies the retry controller looped back around and retried exactly twice
        verify(feedbackLogRepository, times(2)).save(any(FeedbackLog.class));
    }

    @Test
    public void testRecordAction_DynamoDbException_LoggedNotThrown() {
        // Mocking native base client exception mapping for v2 SDK executions
        doThrow(DynamoDbException.builder().message("simulated connection issue").build())
                .when(feedbackLogRepository).save(any(FeedbackLog.class));

        logEntry.setUid("uid1");
        logEntry.setArticleId(String.valueOf(1L));
        logEntry.setFeedback(FeedbackLogService.Feedback.ACCEPTED.name());
        logEntry.setCreateTimestamp(1700000000L);
        
        service.recordAction(logEntry);
        
        // Assert that structural exceptions immediately bail instead of looping or breaking the app execution flow
        verify(feedbackLogRepository, times(1)).save(any(FeedbackLog.class));
    }

    @Test
    public void testRecordAction_NullUid_SkipsCallEntirely() {
        logEntry.setUid(null);
        logEntry.setFeedback(FeedbackLogService.Feedback.ACCEPTED.name());
        
        service.recordAction(logEntry);
        
        verify(feedbackLogRepository, never()).save(any(FeedbackLog.class));
    }

    @Test
    public void testRecordAction_EmptyUid_SkipsCallEntirely() {
        logEntry.setUid("");
        logEntry.setFeedback(FeedbackLogService.Feedback.ACCEPTED.name());
        
        service.recordAction(logEntry);
        
        verify(feedbackLogRepository, never()).save(any(FeedbackLog.class));
    }

    @Test
    public void testRecordAction_NullFeedback_SkipsCallEntirely() {
        logEntry.setUid("uid1");
        logEntry.setFeedback(null);
        
        service.recordAction(logEntry);
        
        verify(feedbackLogRepository, never()).save(any(FeedbackLog.class));
    }
}