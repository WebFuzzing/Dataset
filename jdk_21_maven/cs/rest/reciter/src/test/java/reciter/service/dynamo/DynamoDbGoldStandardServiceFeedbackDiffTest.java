package reciter.service.dynamo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.api.parameters.GoldStandardUpdateFlag;
import reciter.database.dynamodb.model.FeedbackLog;
import reciter.database.dynamodb.model.GoldStandard;
import reciter.database.dynamodb.repository.DynamoDbGoldStandardRepository;
import reciter.feedback.EntryPath;
import reciter.service.ArticleProvenanceService;
import reciter.service.ESearchResultService;
import reciter.service.FeedbackLogService;
import reciter.service.PmidProvenanceService;

/**
 * A single-pmid UPDATE (merge semantics — the AAR queue's shape) must log exactly
 * the newly accepted pmid, never the rest of the existing set as PENDING. Diffing
 * against the incoming request instead of the merged final state flooded FeedbackLog
 * with a bogus PENDING row per already-known pmid on every accept.
 */
@ExtendWith(MockitoExtension.class)
public class DynamoDbGoldStandardServiceFeedbackDiffTest {

    @Mock
    private DynamoDbGoldStandardRepository dynamoDbGoldStandardRepository;
    @Mock
    private ESearchResultService eSearchResultService;
    @Mock
    private PmidProvenanceService pmidProvenanceService;
    @Mock
    private FeedbackLogService feedbackLogService;
    @Mock
    private ArticleProvenanceService articleProvenanceService;

    @InjectMocks
    private DynamoDbGoldStandardService service;

    private GoldStandard existing(String uid, List<Long> known) {
        GoldStandard gs = new GoldStandard();
        gs.setUid(uid);
        gs.setKnownPmids(new ArrayList<>(known));
        return gs;
    }

    @Test
    public void singlePmidMergeAcceptLogsOnlyTheNewPmid() {
        when(dynamoDbGoldStandardRepository.findById("brk2001"))
                .thenReturn(Optional.of(existing("brk2001", Arrays.asList(111L, 222L, 333L))));

        GoldStandard incoming = new GoldStandard();
        incoming.setUid("brk2001");
        incoming.setKnownPmids(new ArrayList<>(Arrays.asList(444L)));

        service.save(incoming, GoldStandardUpdateFlag.UPDATE, "adversarial-attribution-review",
                EntryPath.PM_AUTHOR, 40421);

        ArgumentCaptor<FeedbackLog> logged = ArgumentCaptor.forClass(FeedbackLog.class);
        verify(feedbackLogService).recordAction(logged.capture());
        assertEquals(1, logged.getAllValues().size());
        assertEquals("444", logged.getValue().getArticleId());
        assertEquals(FeedbackLogService.Feedback.ACCEPTED.name(), logged.getValue().getFeedback());
        assertEquals(40421, logged.getValue().getCuratedBy());
        // the merge kept the full set
        assertTrue(incoming.getKnownPmids().containsAll(Arrays.asList(111L, 222L, 333L, 444L)));
    }

    @Test
    public void reAcceptOfKnownPmidLogsNothing() {
        when(dynamoDbGoldStandardRepository.findById("brk2001"))
                .thenReturn(Optional.of(existing("brk2001", Arrays.asList(111L, 222L))));

        GoldStandard incoming = new GoldStandard();
        incoming.setUid("brk2001");
        incoming.setKnownPmids(new ArrayList<>(Arrays.asList(222L)));

        service.save(incoming, GoldStandardUpdateFlag.UPDATE, "adversarial-attribution-review",
                EntryPath.PM_AUTHOR, 40421);

        verify(feedbackLogService, never()).recordAction(any());
    }
}