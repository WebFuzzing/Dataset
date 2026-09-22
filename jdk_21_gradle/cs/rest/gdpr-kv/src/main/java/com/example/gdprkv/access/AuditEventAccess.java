package com.example.gdprkv.access;

import com.example.gdprkv.models.AuditEvent;
import java.util.List;
import java.util.Optional;

/**
 * Storage abstraction for the append-only {@code audit_events} table. Implementations
 * are responsible for persisting events and retrieving the latest hash for a subject so
 * the service layer can maintain the hash chain.
 */
public interface AuditEventAccess {
    void put(AuditEvent event);
    Optional<AuditEvent> findLatest(String subjectId);

    /**
     * Finds all audit events for a specific subject, ordered by timestamp ascending.
     * Used for subject access requests and audit trail verification.
     *
     * @param subjectId the subject ID to query
     * @return list of all audit events for the subject, ordered chronologically
     */
    List<AuditEvent> findAllBySubjectId(String subjectId);

    /**
     * Finds all audit events with timestamp older than the specified cutoff.
     * Used for retention policy enforcement.
     *
     * @param cutoffTimestamp events with timestamp less than this will be returned
     * @return list of audit events older than cutoff
     */
    List<AuditEvent> findEventsOlderThan(long cutoffTimestamp);

    /**
     * Deletes the specified audit event.
     *
     * @param event the event to delete
     */
    void delete(AuditEvent event);
}
