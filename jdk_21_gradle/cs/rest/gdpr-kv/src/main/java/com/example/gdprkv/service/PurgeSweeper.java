package com.example.gdprkv.service;

import com.example.gdprkv.access.RecordAccess;
import com.example.gdprkv.config.PurgeSweeperProperties;
import com.example.gdprkv.models.Record;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled job that permanently deletes tombstoned records after their retention period expires.
 * Uses the records_by_purge_due GSI to efficiently find expired records without scanning.
 *
 * The sweeper processes records in hourly purge buckets (e.g., "h#20250827T21") to avoid
 * hot partitions and enable efficient querying. It looks back N hours (configurable) to
 * catch any records that might have been missed in previous runs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "purge.sweeper.enabled", havingValue = "true")
public class PurgeSweeper {

    private static final DateTimeFormatter PURGE_BUCKET_FORMATTER =
            DateTimeFormatter.ofPattern("'h#'yyyyMMdd'T'HH").withZone(ZoneOffset.UTC);

    private final Clock clock;
    private final PurgeSweeperProperties properties;
    private final RecordAccess recordAccess;
    private final AuditLogService auditLogService;

    @Scheduled(cron = "${purge.sweeper.schedule:0 */15 * * * *}")
    public void purgeExpiredRecords() {
        long startTime = clock.millis();
        String jobRequestId = "purge-job-" + UUID.randomUUID();

        log.info("[{}] Starting purge sweeper job at {} (lookback: {} hours)",
                jobRequestId, startTime, properties.getLookbackHours());

        long now = clock.millis();
        List<String> bucketsToCheck = generatePurgeBuckets(now, properties.getLookbackHours());

        log.info("[{}] Will check {} purge buckets: {}",
                jobRequestId, bucketsToCheck.size(), bucketsToCheck);

        int totalCandidates = 0;
        int totalPurged = 0;
        int totalFailed = 0;

        for (String bucket : bucketsToCheck) {
            try {
                PurgeBucketResult result = purgeBucket(bucket, now, jobRequestId);
                totalCandidates += result.candidates;
                totalPurged += result.purged;
                totalFailed += result.failed;
            } catch (Exception ex) {
                log.error("[{}] Failed to process purge bucket {}: {}",
                        jobRequestId, bucket, ex.getMessage(), ex);
            }
        }

        long duration = clock.millis() - startTime;
        log.info("[{}] Completed purge sweeper job in {}ms: candidates={}, purged={}, failed={}",
                jobRequestId, duration, totalCandidates, totalPurged, totalFailed);
    }

    private PurgeBucketResult purgeBucket(String purgeBucket, long cutoffTimestamp, String jobRequestId) {
        log.debug("[{}] Processing purge bucket: {}, cutoff: {}",
                jobRequestId, purgeBucket, cutoffTimestamp);

        List<Record> candidates = recordAccess.findRecordsDueForPurge(purgeBucket, cutoffTimestamp);

        if (candidates.isEmpty()) {
            log.debug("[{}] No records due for purge in bucket {}", jobRequestId, purgeBucket);
            return new PurgeBucketResult(0, 0, 0);
        }

        log.info("[{}] Found {} records due for purge in bucket {}",
                jobRequestId, candidates.size(), purgeBucket);

        int purged = 0;
        int failed = 0;

        for (Record record : candidates) {
            try {
                // Verify the record is actually tombstoned and past its purge_due_at
                if (!isSafeToDelete(record, cutoffTimestamp)) {
                    log.warn("[{}] Skipping record {}/{} - not safe to delete (tombstoned={}, purge_due_at={})",
                            jobRequestId, record.getSubjectId(), record.getRecordKey(),
                            record.getTombstoned(), record.getPurgeDueAt());
                    continue;
                }

                auditLogService.recordPurgeCandidateIdentified(
                        record.getSubjectId(), record.getRecordKey(), jobRequestId);

                recordAccess.delete(record);

                auditLogService.recordPurgeCandidateSuccessful(
                        record.getSubjectId(), record.getRecordKey(), jobRequestId);

                purged++;

                log.debug("[{}] Purged record {}/{}",
                        jobRequestId, record.getSubjectId(), record.getRecordKey());

            } catch (Exception ex) {
                failed++;
                log.error("[{}] Failed to purge record {}/{}: {}",
                        jobRequestId, record.getSubjectId(), record.getRecordKey(), ex.getMessage());

                auditLogService.recordPurgeCandidateFailed(
                        record.getSubjectId(), record.getRecordKey(), jobRequestId, ex.getMessage());
            }
        }

        return new PurgeBucketResult(candidates.size(), purged, failed);
    }

    /**
     * Safety check to ensure we only delete records that are:
     * 1. Actually tombstoned
     * 2. Past their purge_due_at timestamp
     */
    private boolean isSafeToDelete(Record record, long cutoffTimestamp) {
        if (record.getTombstoned() == null || !record.getTombstoned()) {
            return false;
        }
        if (record.getPurgeDueAt() == null) {
            return false;
        }
        return record.getPurgeDueAt() <= cutoffTimestamp;
    }

    /**
     * Generates purge bucket strings for the current time minus lookback hours.
     * Example: "h#20250827T21", "h#20250827T20", etc.
     */
    private List<String> generatePurgeBuckets(long nowMillis, int lookbackHours) {
        List<String> buckets = new ArrayList<>();
        long currentHour = nowMillis;

        // Generate buckets for each hour in the lookback window
        for (int i = 0; i <= lookbackHours; i++) {
            long hourMillis = currentHour - (i * 3600000L);
            String bucket = PURGE_BUCKET_FORMATTER.format(Instant.ofEpochMilli(hourMillis));
            buckets.add(bucket);
        }

        return buckets;
    }

    private record PurgeBucketResult(int candidates, int purged, int failed) { }
}
