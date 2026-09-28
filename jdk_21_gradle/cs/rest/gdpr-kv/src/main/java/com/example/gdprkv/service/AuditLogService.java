package com.example.gdprkv.service;

import com.example.gdprkv.access.AuditEventAccess;
import com.example.gdprkv.models.AuditEvent;
import com.example.gdprkv.models.Record;
import com.example.gdprkv.models.Subject;
import java.time.Clock;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private static final String ZERO_HASH = "0".repeat(64);

    private final AuditEventAccess auditEventAccess;
    private final Clock clock;

    public void recordPutRequested(String subjectId,
                                   String recordKey,
                                   String purpose,
                                   String requestId) {
        appendRecordEvent(subjectId, recordKey, purpose, requestId, AuditEvent.EventType.PUT_REQUESTED, null);
    }

    public void recordPutSuccess(Record record) {
        AuditEvent.EventType type = (record.getVersion() != null && record.getVersion() > 1)
                ? AuditEvent.EventType.PUT_UPDATE_ITEM_SUCCESS
                : AuditEvent.EventType.PUT_NEW_ITEM_SUCCESS;
        appendRecordEvent(
                record.getSubjectId(),
                record.getRecordKey(),
                record.getPurpose(),
                record.getRequestId(),
                type,
                Map.of("version", record.getVersion())
        );
    }

    public void recordPutFailure(String subjectId,
                                 String recordKey,
                                 String purpose,
                                 String requestId,
                                 String errorMessage) {
        appendRecordEvent(subjectId, recordKey, purpose, requestId,
                AuditEvent.EventType.PUT_FAILED,
                errorMessage == null ? null : Map.of("error", errorMessage));
    }

    public void recordCreateSubjectRequested(String subjectId, String requestId) {
        appendSubjectEvent(subjectId, requestId, AuditEvent.EventType.CREATE_SUBJECT_REQUESTED, null);
    }

    public void recordCreateSubjectSuccess(Subject subject) {
        appendSubjectEvent(
                subject.getSubjectId(),
                subject.getRequestId(),
                AuditEvent.EventType.CREATE_SUBJECT_COMPLETED,
                null
        );
    }

    public void recordCreateSubjectFailure(String subjectId, String requestId, String errorMessage) {
        appendSubjectEvent(subjectId, requestId,
                AuditEvent.EventType.CREATE_SUBJECT_FAILED,
                errorMessage == null ? null : Map.of("error", errorMessage));
    }

    public void recordDeleteRequested(String subjectId, String recordKey, String requestId) {
        appendRecordEvent(subjectId, recordKey, null, requestId,
                AuditEvent.EventType.DELETE_ITEM_REQUESTED, null);
    }

    public void recordDeleteSuccess(Record record) {
        appendRecordEvent(
                record.getSubjectId(),
                record.getRecordKey(),
                record.getPurpose(),
                record.getRequestId(),
                AuditEvent.EventType.DELETE_ITEM_SUCCESSFUL,
                Map.of("version", record.getVersion(), "purge_due_at", record.getPurgeDueAt())
        );
    }

    public void recordDeleteAlreadyTombstoned(String subjectId, String recordKey, String requestId) {
        appendRecordEvent(subjectId, recordKey, null, requestId,
                AuditEvent.EventType.DELETE_ITEM_ALREADY_TOMBSTONED, null);
    }

    public void recordDeleteFailure(String subjectId, String recordKey, String requestId, String errorMessage) {
        appendRecordEvent(subjectId, recordKey, null, requestId,
                AuditEvent.EventType.DELETE_ITEM_FAILURE,
                errorMessage == null ? null : Map.of("error", errorMessage));
    }

    public void recordSubjectErasureRequested(String subjectId, String requestId) {
        appendSubjectEvent(subjectId, requestId, AuditEvent.EventType.SUBJECT_ERASURE_REQUESTED, null);
    }

    public void recordSubjectErasureStarted(String subjectId, String requestId, int recordCount) {
        appendSubjectEvent(subjectId, requestId,
                AuditEvent.EventType.SUBJECT_ERASURE_STARTED,
                Map.of("record_count", recordCount));
    }

    public void recordSubjectErasureCompleted(String subjectId, String requestId, int recordsDeleted) {
        appendSubjectEvent(subjectId, requestId,
                AuditEvent.EventType.SUBJECT_ERASURE_COMPLETED,
                Map.of("records_deleted", recordsDeleted));
    }

    public void recordSubjectErasureFailure(String subjectId, String requestId, String errorMessage) {
        appendSubjectEvent(subjectId, requestId,
                AuditEvent.EventType.SUBJECT_ERASURE_FAILED,
                errorMessage == null ? null : Map.of("error", errorMessage));
    }

    /**
     * Appends an audit event for record operations, maintaining the per-subject hash chain.
     */
    private void appendRecordEvent(String subjectId,
                                   String recordKey,
                                   String purpose,
                                   String requestId,
                                   AuditEvent.EventType type,
                                   Map<String, Object> details) {
        long now = clock.millis();
        String prevHash = auditEventAccess.findLatest(subjectId)
                .map(AuditEvent::getHash)
                .orElse(ZERO_HASH);

        AuditEvent event = AuditEvent.builder()
                .subjectId(subjectId)
                .tsUlid(generateTimestampUlid(now))
                .eventType(type)
                .requestId(requestId)
                .timestamp(now)
                .prevHash(prevHash)
                .itemKey(recordKey)
                .purpose(purpose)
                .details(details)
                .build();

        auditEventAccess.put(event);
    }

    /**
     * Appends an audit event for subject operations, maintaining the per-subject hash chain.
     */
    private void appendSubjectEvent(String subjectId,
                                     String requestId,
                                     AuditEvent.EventType type,
                                     Map<String, Object> details) {
        long now = clock.millis();
        String prevHash = auditEventAccess.findLatest(subjectId)
                .map(AuditEvent::getHash)
                .orElse(ZERO_HASH);

        AuditEvent event = AuditEvent.builder()
                .subjectId(subjectId)
                .tsUlid(generateTimestampUlid(now))
                .eventType(type)
                .requestId(requestId)
                .timestamp(now)
                .prevHash(prevHash)
                .details(details)
                .build();

        auditEventAccess.put(event);
    }

    /**
     * Records that a purge candidate was identified by the sweeper.
     */
    public void recordPurgeCandidateIdentified(String subjectId, String recordKey, String requestId) {
        appendRecordEvent(subjectId, recordKey, null, requestId, AuditEvent.EventType.PURGE_CANDIDATE_IDENTIFIED, null);
    }

    /**
     * Records successful purge of a record.
     */
    public void recordPurgeCandidateSuccessful(String subjectId, String recordKey, String requestId) {
        appendRecordEvent(subjectId, recordKey, null, requestId, AuditEvent.EventType.PURGE_CANDIDATE_SUCCESSFUL, null);
    }

    /**
     * Records failed purge attempt.
     */
    public void recordPurgeCandidateFailed(String subjectId, String recordKey, String requestId, String errorMessage) {
        Map<String, Object> details = new HashMap<>();
        details.put("error", errorMessage);
        appendRecordEvent(subjectId, recordKey, null, requestId, AuditEvent.EventType.PURGE_CANDIDATE_FAILED, details);
    }

    /**
     * Generates a ULID-like string combining the millisecond timestamp and
     * a random component so events retain a natural sort order per subject.
     */
    private String generateTimestampUlid(long timestamp) {
        String random = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return timestamp + "_" + random;
    }
}
