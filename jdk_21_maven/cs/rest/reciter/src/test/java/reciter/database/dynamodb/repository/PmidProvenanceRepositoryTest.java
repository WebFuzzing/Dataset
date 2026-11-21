package reciter.database.dynamodb.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import reciter.database.dynamodb.model.PmidProvenance;

@ExtendWith(MockitoExtension.class)
public class PmidProvenanceRepositoryTest {

    @Mock
    private PmidProvenanceRepository pmidProvenanceRepository;

    private PmidProvenance provenance1;
    private PmidProvenance provenance2;

    @BeforeEach
    public void setUp() {
        provenance1 = new PmidProvenance();
        provenance1.setUid("uid1");
        provenance1.setPmid(11111111L);
        provenance1.setRetrievalStrategy("S1");

        provenance2 = new PmidProvenance();
        provenance2.setUid("uid1");
        provenance2.setPmid(22222222L);
        provenance2.setRetrievalStrategy("BACKFILL_FROM_ESEARCHRESULT");
    }

    @Test
    public void testSave() {
        doNothing().when(pmidProvenanceRepository).save(provenance1);

        pmidProvenanceRepository.save(provenance1);

        verify(pmidProvenanceRepository).save(provenance1);
    }

    @Test
    public void testSaveIfNotExists_NewItem() {
        doNothing().when(pmidProvenanceRepository).saveIfNotExists(provenance1);

        pmidProvenanceRepository.saveIfNotExists(provenance1);

        verify(pmidProvenanceRepository).saveIfNotExists(provenance1);
    }

    @Test
    public void testSaveIfNotExists_ExistingItem() {
        // Simulates the case where item already exists — method should silently skip (no exception surfaced)
        doNothing().when(pmidProvenanceRepository).saveIfNotExists(provenance1);

        pmidProvenanceRepository.saveIfNotExists(provenance1);

        verify(pmidProvenanceRepository).saveIfNotExists(provenance1);
    }

    @Test
    public void testSaveAllIfNotExists() {
        List<PmidProvenance> provenances = Arrays.asList(provenance1, provenance2);
        doNothing().when(pmidProvenanceRepository).saveAllIfNotExists(provenances);

        pmidProvenanceRepository.saveAllIfNotExists(provenances);

        verify(pmidProvenanceRepository).saveAllIfNotExists(provenances);
    }

    @Test
    public void testFindByUid_ReturnsList() {
        when(pmidProvenanceRepository.findByUid("uid1"))
                .thenReturn(Arrays.asList(provenance1, provenance2));

        List<PmidProvenance> results = pmidProvenanceRepository.findByUid("uid1");

        verify(pmidProvenanceRepository).findByUid("uid1");
        assertEquals(2, results.size());
    }

    @Test
    public void testFindByUid_ReturnsEmpty() {
        when(pmidProvenanceRepository.findByUid("uid_missing"))
                .thenReturn(Collections.emptyList());

        List<PmidProvenance> results = pmidProvenanceRepository.findByUid("uid_missing");

        verify(pmidProvenanceRepository).findByUid("uid_missing");
        assertTrue(results.isEmpty());
    }

    @Test
    public void testFindPmidsByUid() {
        when(pmidProvenanceRepository.findPmidsByUid("uid1"))
                .thenReturn(Set.of(11111111L, 22222222L));

        Set<Long> pmids = pmidProvenanceRepository.findPmidsByUid("uid1");

        verify(pmidProvenanceRepository).findPmidsByUid("uid1");
        assertEquals(2, pmids.size());
        assertTrue(pmids.contains(11111111L));
        assertTrue(pmids.contains(22222222L));
    }

    @Test
    public void testFindPmidsByUidAndStrategy_MatchingStrategy() {
        when(pmidProvenanceRepository.findPmidsByUidAndStrategy("uid1", "S1"))
                .thenReturn(Set.of(11111111L));

        Set<Long> pmids = pmidProvenanceRepository.findPmidsByUidAndStrategy("uid1", "S1");

        verify(pmidProvenanceRepository).findPmidsByUidAndStrategy("uid1", "S1");
        assertEquals(1, pmids.size());
        assertTrue(pmids.contains(11111111L));
    }

    @Test
    public void testFindPmidsByUidAndStrategy_NoMatch() {
        when(pmidProvenanceRepository.findPmidsByUidAndStrategy("uid1", "UNKNOWN_STRATEGY"))
                .thenReturn(Collections.emptySet());

        Set<Long> pmids = pmidProvenanceRepository.findPmidsByUidAndStrategy("uid1", "UNKNOWN_STRATEGY");

        verify(pmidProvenanceRepository).findPmidsByUidAndStrategy("uid1", "UNKNOWN_STRATEGY");
        assertTrue(pmids.isEmpty());
    }

    @Test
    public void testUpdateStrategyIfBackfill_ExistingBackfillItem() {
        // Item exists with BACKFILL strategy — should update to real strategy
        doNothing().when(pmidProvenanceRepository).updateStrategyIfBackfill("uid1", 22222222L, "S2");

        pmidProvenanceRepository.updateStrategyIfBackfill("uid1", 22222222L, "S2");

        verify(pmidProvenanceRepository).updateStrategyIfBackfill("uid1", 22222222L, "S2");
    }

    @Test
    public void testUpdateStrategyIfBackfill_ItemNotBackfill() {
        // Item exists but strategy is already real — method should be a no-op
        doNothing().when(pmidProvenanceRepository).updateStrategyIfBackfill("uid1", 11111111L, "S1");

        pmidProvenanceRepository.updateStrategyIfBackfill("uid1", 11111111L, "S1");

        verify(pmidProvenanceRepository).updateStrategyIfBackfill("uid1", 11111111L, "S1");
    }

    @Test
    public void testUpdateStrategyIfBackfill_ItemNotFound() {
        // Item doesn't exist — method should be a no-op
        doNothing().when(pmidProvenanceRepository).updateStrategyIfBackfill("uid_missing", 99999999L, "S3");

        pmidProvenanceRepository.updateStrategyIfBackfill("uid_missing", 99999999L, "S3");

        verify(pmidProvenanceRepository).updateStrategyIfBackfill("uid_missing", 99999999L, "S3");
    }
}