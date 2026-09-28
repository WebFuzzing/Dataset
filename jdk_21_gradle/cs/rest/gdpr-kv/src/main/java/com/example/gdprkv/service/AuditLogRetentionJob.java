package com.example.gdprkv.service;

import com.example.gdprkv.access.AuditEventAccess;
import com.example.gdprkv.config.AuditRetentionProperties;
import com.example.gdprkv.models.AuditEvent;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled job that enforces retention policy on audit events.
 * Runs periodically to delete audit events older than the configured retention period.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(value = "audit.retention.enabled", havingValue = "true")
public class AuditLogRetentionJob {

    private static final long MILLIS_PER_DAY = 86400000L;

    private final Clock clock;
    private final AuditRetentionProperties properties;
    private final AuditEventAccess auditEventAccess;

    @Scheduled(cron = "${audit.retention.schedule:0 0 2 * * *}")
    public void enforceRetentionPolicy() {
        long startTime = clock.millis();
        log.info("Starting audit log retention job at {} (retention period: {} days)",
                startTime, properties.getRetentionDays());

        long cutoffTimestamp = startTime - (properties.getRetentionDays() * MILLIS_PER_DAY);
        log.info("Deleting audit events older than {} (cutoff timestamp: {})",
                new java.util.Date(cutoffTimestamp), cutoffTimestamp);

        List<AuditEvent> oldEvents = auditEventAccess.findEventsOlderThan(cutoffTimestamp);
        log.info("Found {} audit events to delete", oldEvents.size());

        int deletedCount = 0;
        int failedCount = 0;

        for (AuditEvent event : oldEvents) {
            try {
                auditEventAccess.delete(event);
                deletedCount++;
            } catch (Exception ex) {
                failedCount++;
                log.warn("Failed to delete audit event for subject {} at {}: {}",
                        event.getSubjectId(), event.getTsUlid(), ex.getMessage());
            }
        }

        long duration = clock.millis() - startTime;
        log.info("Completed audit log retention job in {}ms: deleted={}, failed={}",
                duration, deletedCount, failedCount);
    }
}
