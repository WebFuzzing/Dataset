package reciter.service.dynamo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.api.parameters.GoldStandardUpdateFlag;
import reciter.database.dynamodb.model.GoldStandard;
import reciter.database.dynamodb.repository.DynamoDbGoldStandardRepository;
import reciter.feedback.EntryPath;
import reciter.service.ArticleProvenanceService;
import reciter.service.ESearchResultService;
import reciter.service.FeedbackLogService;
import reciter.service.PmidProvenanceService;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@ExtendWith(MockitoExtension.class)
public class DynamoDbGoldStandardServiceConcurrencyTest {

    @Mock
    private DynamoDbGoldStandardRepository repository;
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

    private GoldStandard baseRecord;

    @BeforeEach
    void setUp() {
        // Setup a base record that exists in the database
        baseRecord = new GoldStandard();
        baseRecord.setUid("rol3004");
        baseRecord.setVersion(1L);
        baseRecord.setKnownPmids(new ArrayList<>(List.of(1000L))); // Initially has PMID 1000
    }

    @Test
    @DisplayName("Explicitly test ConditionalCheckFailedException recovery and side-effect isolation")
    void testExplicitConditionalCheckFailedExceptionHandling() {
        // Arrange
        GoldStandard incomingRequest = new GoldStandard();
        incomingRequest.setUid("rol3004");
        incomingRequest.setKnownPmids(Collections.singletonList(2000L)); // User is trying to accept PMID 2000

        // 1st read: returns version 1
        // 2nd read (inside catch): returns version 2 (simulating someone else updated it to include PMID 1500)
        GoldStandard updatedRecord = new GoldStandard();
        updatedRecord.setUid("rol3004");
        updatedRecord.setVersion(2L);
        updatedRecord.setKnownPmids(new ArrayList<>(List.of(1000L, 1500L)));

        when(repository.findById("rol3004"))
                .thenReturn(Optional.of(baseRecord))     // First try
                .thenReturn(Optional.of(updatedRecord)); // Second try inside the catch block

        // Mock repository.save() to THROW an exception on the first call, and SUCCEED on the second
        doThrow(ConditionalCheckFailedException.builder().message("Simulated Collision").build())
                .doNothing()
                .when(repository).save(any(GoldStandard.class));

        // Act
        service.save(incomingRequest, GoldStandardUpdateFlag.UPDATE, "TestStrategy", EntryPath.CANDIDATE_LIST);

        // Assert
        // 1. Verify findById was called EXACTLY twice (once in try, once in catch)
        verify(repository, times(2)).findById("rol3004");

        // 2. Verify save was called EXACTLY twice (once failed, once succeeded)
        ArgumentCaptor<GoldStandard> saveCaptor = ArgumentCaptor.forClass(GoldStandard.class);
        verify(repository, times(2)).save(saveCaptor.capture());

        // 3. Inspect the final saved object to ensure ALL PMIDs were successfully merged
        GoldStandard finalSavedObject = saveCaptor.getAllValues().get(1);
        assertTrue(finalSavedObject.getKnownPmids().contains(1000L), "Should contain original PMID");
        assertTrue(finalSavedObject.getKnownPmids().contains(1500L), "Should contain the PMID added by the competing thread");
        assertTrue(finalSavedObject.getKnownPmids().contains(2000L), "Should contain the new PMID requested by this thread");

        // 4. CRITICAL: Verify side effects only fired ONCE (proving they are safely outside the retry logic)
        verify(feedbackLogService, times(1)).recordAction(any());
        verify(articleProvenanceService, times(1)).upsertCuratorAction(anyString(), anyLong(), any(), anyLong());
    }

    @Test
    @DisplayName("Simulate 2 concurrent pods racing to update the exact same record")
    void testSimulatedMultiPodConcurrency() throws InterruptedException {
        // Arrange
        // We will simulate the DB version incrementing behavior dynamically using doAnswer
        AtomicInteger databaseVersion = new AtomicInteger(1);
        List<Long> databasePmids = Collections.synchronizedList(new ArrayList<>(List.of(1000L)));

        when(repository.findById("rol3004")).thenAnswer(invocation -> {
            GoldStandard snapshot = new GoldStandard();
            snapshot.setUid("rol3004");
            snapshot.setVersion((long) databaseVersion.get());
            snapshot.setKnownPmids(new ArrayList<>(databasePmids));
            return Optional.of(snapshot);
        });

        doAnswer(invocation -> {
            GoldStandard itemToSave = invocation.getArgument(0);
            synchronized (databaseVersion) {
                if (itemToSave.getVersion() != databaseVersion.get()) {
                    // This mimics DynamoDB perfectly: if versions don't match, throw the exception!
                    throw ConditionalCheckFailedException.builder().message("Version mismatch").build();
                }
                // Success! Increment version and save data
                databaseVersion.incrementAndGet();
                databasePmids.clear();
                databasePmids.addAll(itemToSave.getKnownPmids());
            }
            return null;
        }).when(repository).save(any(GoldStandard.class));

        // Setup the threading environment to simulate Pod A and Pod B
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finishLine = new CountDownLatch(2);

        // Pod A wants to accept PMID 2000
        Runnable podA = () -> {
            try {
                startGun.await(); // Wait for the start signal
                GoldStandard req = new GoldStandard();
                req.setUid("rol3004");
                req.setKnownPmids(Collections.singletonList(2000L));
                service.save(req, GoldStandardUpdateFlag.UPDATE, "StrategyA", EntryPath.CANDIDATE_LIST);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLine.countDown();
            }
        };

        // Pod B wants to accept PMID 3000
        Runnable podB = () -> {
            try {
                startGun.await(); // Wait for the start signal
                GoldStandard req = new GoldStandard();
                req.setUid("rol3004");
                req.setKnownPmids(Collections.singletonList(3000L));
                service.save(req, GoldStandardUpdateFlag.UPDATE, "StrategyB", EntryPath.CANDIDATE_LIST);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                finishLine.countDown();
            }
        };

        // Act
        executor.submit(podA);
        executor.submit(podB);
        
        startGun.countDown(); // Fire the start gun! Both threads hit the service at the exact same millisecond
        
        boolean finished = finishLine.await(5, TimeUnit.SECONDS);

        // Assert
        assertTrue(finished, "Threads timed out, indicating a deadlock");
        
        // Verify both requests succeeded and the database has all 3 PMIDs
        assertEquals(3, databasePmids.size(), "Database should contain the original PMID + Pod A's PMID + Pod B's PMID");
        assertTrue(databasePmids.contains(1000L), "Original PMID lost!");
        assertTrue(databasePmids.contains(2000L), "Pod A's PMID lost!");
        assertTrue(databasePmids.contains(3000L), "Pod B's PMID lost!");

        // The database version should have incremented twice (Version 1 -> 2 -> 3)
        assertEquals(3, databaseVersion.get(), "Database version should be 3 after two successful concurrent updates");
    }
}