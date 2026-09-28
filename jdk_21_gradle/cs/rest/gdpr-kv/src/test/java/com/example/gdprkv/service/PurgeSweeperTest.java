package com.example.gdprkv.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.gdprkv.access.RecordAccess;
import com.example.gdprkv.config.PurgeSweeperProperties;
import com.example.gdprkv.models.Record;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurgeSweeperTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2025-08-27T21:30:00Z"), ZoneOffset.UTC);
    private static final long NOW = CLOCK.millis();

    @Mock
    private RecordAccess recordAccess;

    @Mock
    private AuditLogService auditLogService;

    private PurgeSweeperProperties properties;
    private PurgeSweeper purgeSweeper;

    @BeforeEach
    void setUp() {
        properties = new PurgeSweeperProperties();
        properties.setEnabled(true);
        properties.setSchedule("0 */15 * * * *");
        properties.setLookbackHours(2);

        purgeSweeper = new PurgeSweeper(CLOCK, properties, recordAccess, auditLogService);
    }

    @Test
    @DisplayName("purges tombstoned records that are past their purge_due_at")
    void purgeExpiredRecords() {
        // Create a tombstoned record that's due for purging
        Record expiredRecord = createTombstonedRecord("subject1", "key1", NOW - 1000);

        when(recordAccess.findRecordsDueForPurge(anyString(), anyLong()))
                .thenReturn(List.of(expiredRecord))
                .thenReturn(Collections.emptyList()); // Other buckets are empty

        purgeSweeper.purgeExpiredRecords();

        // Verify the record was deleted
        verify(recordAccess).delete(expiredRecord);

        // Verify audit events were created
        verify(auditLogService).recordPurgeCandidateIdentified(eq("subject1"), eq("key1"), anyString());
        verify(auditLogService).recordPurgeCandidateSuccessful(eq("subject1"), eq("key1"), anyString());
        verify(auditLogService, never()).recordPurgeCandidateFailed(anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("does not purge records that are not yet due")
    void doesNotPurgeRecordsNotYetDue() {
        // Create a tombstoned record that's not yet due for purging
        Record notYetDue = createTombstonedRecord("subject1", "key1", NOW + 100000);

        when(recordAccess.findRecordsDueForPurge(anyString(), anyLong()))
                .thenReturn(List.of(notYetDue));

        purgeSweeper.purgeExpiredRecords();

        // Verify the record was NOT deleted (safety check failed)
        verify(recordAccess, never()).delete(any());
        verify(auditLogService, never()).recordPurgeCandidateIdentified(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("does not purge records that are not tombstoned")
    void doesNotPurgeNonTombstonedRecords() {
        // Create a non-tombstoned record
        Record nonTombstoned = Record.builder()
                .subjectId("subject1")
                .recordKey("key1")
                .purpose("test")
                .version(1L)
                .createdAt(NOW)
                .updatedAt(NOW)
                .retentionDays(30)
                .requestId("req1")
                .tombstoned(false)
                .purgeDueAt(NOW - 1000)
                .purgeBucket("h#20250827T21")
                .build();

        when(recordAccess.findRecordsDueForPurge(anyString(), anyLong()))
                .thenReturn(List.of(nonTombstoned));

        purgeSweeper.purgeExpiredRecords();

        // Verify the record was NOT deleted (safety check failed)
        verify(recordAccess, never()).delete(any());
        verify(auditLogService, never()).recordPurgeCandidateIdentified(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("handles deletion failures gracefully and records audit events")
    void handlesDeleteFailure() {
        Record expiredRecord = createTombstonedRecord("subject1", "key1", NOW - 1000);

        when(recordAccess.findRecordsDueForPurge(anyString(), anyLong()))
                .thenReturn(List.of(expiredRecord))
                .thenReturn(Collections.emptyList());

        // Simulate deletion failure
        doThrow(new RuntimeException("DynamoDB error")).when(recordAccess).delete(any());

        purgeSweeper.purgeExpiredRecords();

        // Verify audit events for failure
        verify(auditLogService).recordPurgeCandidateIdentified(eq("subject1"), eq("key1"), anyString());
        verify(auditLogService).recordPurgeCandidateFailed(eq("subject1"), eq("key1"), anyString(), eq("DynamoDB error"));
        verify(auditLogService, never()).recordPurgeCandidateSuccessful(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("processes multiple records in a single run")
    void processesMultipleRecords() {
        Record record1 = createTombstonedRecord("subject1", "key1", NOW - 1000);
        Record record2 = createTombstonedRecord("subject2", "key2", NOW - 2000);
        Record record3 = createTombstonedRecord("subject3", "key3", NOW - 3000);

        when(recordAccess.findRecordsDueForPurge(anyString(), eq(NOW)))
                .thenReturn(List.of(record1, record2))
                .thenReturn(List.of(record3))
                .thenReturn(Collections.emptyList());

        purgeSweeper.purgeExpiredRecords();

        // Verify all records were deleted
        verify(recordAccess).delete(record1);
        verify(recordAccess).delete(record2);
        verify(recordAccess).delete(record3);

        // Verify audit events for all
        verify(auditLogService, times(3)).recordPurgeCandidateIdentified(anyString(), anyString(), anyString());
        verify(auditLogService, times(3)).recordPurgeCandidateSuccessful(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("handles empty results gracefully")
    void handlesEmptyResults() {
        when(recordAccess.findRecordsDueForPurge(anyString(), anyLong()))
                .thenReturn(Collections.emptyList());

        purgeSweeper.purgeExpiredRecords();

        // Should complete without errors
        verify(recordAccess, never()).delete(any());
        verify(auditLogService, never()).recordPurgeCandidateIdentified(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("checks multiple purge buckets based on lookback hours")
    void checksMultiplePurgeBuckets() {
        properties.setLookbackHours(3);  // Should check 4 buckets (current + 3 previous hours)

        when(recordAccess.findRecordsDueForPurge(anyString(), anyLong()))
                .thenReturn(Collections.emptyList());

        purgeSweeper.purgeExpiredRecords();

        // Verify we queried 4 different buckets (current hour + 3 lookback hours)
        verify(recordAccess, times(4)).findRecordsDueForPurge(anyString(), eq(NOW));
    }

    @Test
    @DisplayName("skips records with null purgeDueAt")
    void skipsRecordsWithNullPurgeDueAt() {
        Record invalidRecord = Record.builder()
                .subjectId("subject1")
                .recordKey("key1")
                .purpose("test")
                .version(1L)
                .createdAt(NOW)
                .updatedAt(NOW)
                .retentionDays(30)
                .requestId("req1")
                .tombstoned(true)
                .tombstonedAt(NOW - 10000)
                .purgeDueAt(null)  // Missing purge_due_at
                .purgeBucket("h#20250827T21")
                .build();

        when(recordAccess.findRecordsDueForPurge(anyString(), anyLong()))
                .thenReturn(List.of(invalidRecord));

        purgeSweeper.purgeExpiredRecords();

        // Verify the record was NOT deleted (safety check failed)
        verify(recordAccess, never()).delete(any());
    }

    private Record createTombstonedRecord(String subjectId, String recordKey, long purgeDueAt) {
        return Record.builder()
                .subjectId(subjectId)
                .recordKey(recordKey)
                .purpose("test_purpose")
                .version(1L)
                .createdAt(NOW - 100000)
                .updatedAt(NOW - 50000)
                .retentionDays(30)
                .requestId("req-" + recordKey)
                .tombstoned(true)
                .tombstonedAt(NOW - 50000)
                .purgeDueAt(purgeDueAt)
                .purgeBucket("h#20250827T21")
                .build();
    }
}
