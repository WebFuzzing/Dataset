package reciter.service.dynamo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.api.parameters.GoldStandardUpdateFlag;
import reciter.database.dynamodb.model.ESearchPmid;
import reciter.database.dynamodb.model.ESearchResult;
import reciter.database.dynamodb.model.FeedbackLog;
import reciter.database.dynamodb.model.GoldStandard;
import reciter.database.dynamodb.repository.DynamoDbGoldStandardRepository;
import reciter.feedback.EntryPath;
import reciter.service.ArticleProvenanceService;
import reciter.service.ESearchResultService;
import reciter.service.FeedbackLogService;
import reciter.service.PmidProvenanceService;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@ExtendWith(MockitoExtension.class)
public class DynamoDbGoldStandardServiceTest {

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

    @Captor
    private ArgumentCaptor<GoldStandard> goldStandardCaptor;
    @Captor
    private ArgumentCaptor<FeedbackLog> feedbackLogCaptor;

    private static final String UID = "test-uid-1";

    @BeforeEach
    public void setUp() {
    }

    @Test
    public void testSave_RefreshUpdateFlag_BypassesMergeLogic() {
        GoldStandard request = createGoldStandard(UID, List.of(100L), List.of(200L));
        
        service.save(request, GoldStandardUpdateFlag.REFRESH, "Source", EntryPath.CANDIDATE_LIST);

        // REFRESH flag should directly save to DDB and bypass all merge/side-effect logic
        verify(dynamoDbGoldStandardRepository, times(1)).save(request);
        verify(dynamoDbGoldStandardRepository, never()).findById(any());
        verify(feedbackLogService, never()).recordAction(any());
    }

    @Test
    public void testSave_Update_NoExistingRecord_CreatesNewAndFiresSideEffects() {
        GoldStandard request = createGoldStandard(UID, List.of(100L), List.of(200L));
        
        when(dynamoDbGoldStandardRepository.findById(UID)).thenReturn(Optional.empty());

        service.save(request, GoldStandardUpdateFlag.UPDATE, "Source", EntryPath.CANDIDATE_LIST);

        verify(dynamoDbGoldStandardRepository, times(1)).save(request);
        
        // As per processAndSave, new records skip recordFeedbackLogAndArticleProvenance
        verify(feedbackLogService, never()).recordAction(any());
        verify(articleProvenanceService, never()).upsertCuratorAction(anyString(), anyLong(), any(), anyLong());
        
        // But writeProvenanceForAcceptedPmids is still called at the end of saveInternal
        verify(pmidProvenanceService, times(1)).saveAllIfNotExists(anyList());
    }

    @Test
    public void testSave_Update_ExistingRecord_MergesCorrectly() {
        GoldStandard existing = createGoldStandard(UID, List.of(100L), List.of(200L));
        existing.setVersion(1L);
        existing.setAuditLog(new ArrayList<>());
        
        // Incoming request adds 300 to known, and adds 100 to rejected (meaning it was moved)
        GoldStandard request = createGoldStandard(UID, List.of(300L), List.of(100L));

        when(dynamoDbGoldStandardRepository.findById(UID)).thenReturn(Optional.of(existing));

        service.save(request, GoldStandardUpdateFlag.UPDATE, "Source", EntryPath.CANDIDATE_LIST);

        verify(dynamoDbGoldStandardRepository).save(goldStandardCaptor.capture());
        GoldStandard savedGS = goldStandardCaptor.getValue();

        // 100 was moved from known to rejected. 300 was added to known. 200 stays in rejected.
        assertEquals(1L, savedGS.getVersion(), "Version must carry over for optimistic locking");
        assertTrue(savedGS.getKnownPmids().contains(300L));
        assertFalse(savedGS.getKnownPmids().contains(100L), "100 should be removed from known because it is in incoming rejected");
        
        assertTrue(savedGS.getRejectedPmids().contains(100L));
        assertTrue(savedGS.getRejectedPmids().contains(200L));
        
        // Ensure audit log is updated
        assertFalse(savedGS.getAuditLog().isEmpty(), "Audit log should have new entries appended");
    }

    @Test
    public void testSave_Delete_RemovesPmidsAndUpdatesESearchResult() {
        GoldStandard existing = createGoldStandard(UID, List.of(100L, 200L), List.of(300L));
        GoldStandard request = createGoldStandard(UID, List.of(100L), List.of(300L)); // PMIDs we want to DELETE
        
        when(dynamoDbGoldStandardRepository.findById(UID)).thenReturn(Optional.of(existing));

        // Mock ESearchResult setup
        ESearchResult eSearchResult = new ESearchResult();
        ESearchPmid esPmid = new ESearchPmid();
        esPmid.setRetrievalStrategyName("GoldStandardRetrievalStrategy");
        esPmid.setPmids(new ArrayList<>(Arrays.asList(100L, 200L, 300L)));
        eSearchResult.setESearchPmids(new ArrayList<>(Collections.singletonList(esPmid)));
        when(eSearchResultService.findByUid(UID)).thenReturn(eSearchResult);

        service.save(request, GoldStandardUpdateFlag.DELETE, "Source", EntryPath.CANDIDATE_LIST);

        verify(dynamoDbGoldStandardRepository).save(goldStandardCaptor.capture());
        GoldStandard savedGS = goldStandardCaptor.getValue();

        // 100 removed from known, 200 remains. 300 removed from rejected.
        assertEquals(List.of(200L), savedGS.getKnownPmids());
        assertTrue(savedGS.getRejectedPmids().isEmpty());

        // Ensure ESearchResult is updated and saved
        verify(eSearchResultService, times(1)).save(any(ESearchResult.class));
        assertFalse(esPmid.getPmids().contains(100L));
        assertFalse(esPmid.getPmids().contains(300L));
        assertTrue(esPmid.getPmids().contains(200L));

        // Check if FeedbackLog created PENDING entries for removed PMIDs
        verify(feedbackLogService, times(2)).recordAction(feedbackLogCaptor.capture());
        assertTrue(feedbackLogCaptor.getAllValues().stream().allMatch(f -> "PENDING".equals(f.getFeedback())));
    }

    @Test
    public void testSave_Update_RetriesOnConditionalCheckFailedException() {
        GoldStandard request = createGoldStandard(UID, List.of(100L), new ArrayList<>());

        // Simulate returning an existing record on the first pass
        GoldStandard baseline1 = createGoldStandard(UID, new ArrayList<>(), new ArrayList<>());
        
        // Simulate returning an UPDATED record on the second pass (someone else wrote 200 in the meantime)
        GoldStandard baseline2 = createGoldStandard(UID, List.of(200L), new ArrayList<>());

        // Mock findById to return baseline1 on first call, baseline2 on second call
        when(dynamoDbGoldStandardRepository.findById(UID))
            .thenReturn(Optional.of(baseline1))
            .thenReturn(Optional.of(baseline2));

        // Throw conflict exception on FIRST save, succeed on SECOND save
        doThrow(ConditionalCheckFailedException.builder().message("Conflict").build())
            .doNothing()
            .when(dynamoDbGoldStandardRepository).save(any(GoldStandard.class));

        service.save(request, GoldStandardUpdateFlag.UPDATE, "Source", EntryPath.CANDIDATE_LIST);

        // Verification
        verify(dynamoDbGoldStandardRepository, times(2)).findById(UID); // Read twice due to collision
        verify(dynamoDbGoldStandardRepository, times(2)).save(goldStandardCaptor.capture()); // Tried to save twice
        
        GoldStandard finalSavedState = goldStandardCaptor.getAllValues().get(1); // Get the 2nd save attempt
        
        // Should contain BOTH the concurrent writer's 200, AND the current request's 100
        assertTrue(finalSavedState.getKnownPmids().contains(100L));
        assertTrue(finalSavedState.getKnownPmids().contains(200L));
        
        // Important: Side effects must only fire ONCE despite the retry!
        verify(feedbackLogService, times(1)).recordAction(any());
        verify(pmidProvenanceService, times(1)).saveAllIfNotExists(anyList());
    }

    @Test
    public void testSaveListInternal_Update_MergesCorrectlyForMultipleRecords() {
        GoldStandard existing = createGoldStandard(UID, List.of(100L), List.of());
        existing.setVersion(1L);

        GoldStandard request1 = createGoldStandard(UID, List.of(200L), List.of());
        GoldStandard request2 = createGoldStandard("test-uid-2", List.of(300L), List.of());
        List<GoldStandard> requestList = Arrays.asList(request1, request2);

        when(dynamoDbGoldStandardRepository.findAllById(anyList()))
            .thenReturn(Collections.singletonList(existing)); // only UID 1 exists in DB, UID 2 is new

        service.save(requestList, GoldStandardUpdateFlag.UPDATE, "Source");

        // Verify saveAll was called
        verify(dynamoDbGoldStandardRepository, times(1)).saveAll(anyList());

        // Verify known list was merged for UID 1 (100 + 200)
        assertTrue(request1.getKnownPmids().contains(100L));
        assertTrue(request1.getKnownPmids().contains(200L));
        assertEquals(1L, request1.getVersion()); // Carried over version

        // UID 2 is brand new, so it just has its original known list
        assertTrue(request2.getKnownPmids().contains(300L));
    }

    @Test
    public void testFindByUid() {
        GoldStandard gs = new GoldStandard();
        when(dynamoDbGoldStandardRepository.findById(UID)).thenReturn(Optional.of(gs));

        GoldStandard result = service.findByUid(UID);
        
        assertNotNull(result);
        verify(dynamoDbGoldStandardRepository, times(1)).findById(UID);
    }

    @Test
    public void testDelete() {
        service.delete(UID);
        verify(dynamoDbGoldStandardRepository, times(1)).deleteById(UID);
    }

    /**
     * Helper to create a GoldStandard object with mutable lists, as the service
     * actively modifies these lists in-memory during merging.
     */
    private GoldStandard createGoldStandard(String uid, List<Long> known, List<Long> rejected) {
        GoldStandard gs = new GoldStandard();
        gs.setUid(uid);
        gs.setKnownPmids(known != null ? new ArrayList<>(known) : new ArrayList<>());
        gs.setRejectedPmids(rejected != null ? new ArrayList<>(rejected) : new ArrayList<>());
        gs.setAuditLog(new ArrayList<>());
        return gs;
    }
}