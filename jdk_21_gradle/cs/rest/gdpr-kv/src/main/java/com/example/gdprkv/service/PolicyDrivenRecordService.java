package com.example.gdprkv.service;

import com.example.gdprkv.access.PolicyAccess;
import com.example.gdprkv.access.RecordAccess;
import com.example.gdprkv.access.SubjectAccess;
import com.example.gdprkv.models.Policy;
import com.example.gdprkv.models.Record;
import com.example.gdprkv.requests.DeleteRecordServiceRequest;
import com.example.gdprkv.requests.PutRecordServiceRequest;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Applies policy constraints when writing records, handling versioning, tombstone metadata,
 * retention calculations, subject existence validation, and persistence while delegating storage
 * to the configured access ports.
 */
@Service
public class PolicyDrivenRecordService {

    private final PolicyAccess policyAccess;
    private final RecordAccess recordAccess;
    private final SubjectAccess subjectAccess;
    private final Clock clock;

    public PolicyDrivenRecordService(PolicyAccess policyAccess,
                                     RecordAccess recordAccess,
                                     SubjectAccess subjectAccess,
                                     Clock clock) {
        this.policyAccess = policyAccess;
        this.recordAccess = recordAccess;
        this.subjectAccess = subjectAccess;
        this.clock = clock;
    }

    public Record putRecord(PutRecordServiceRequest request) {
        Objects.requireNonNull(request, "request");

        subjectAccess.findBySubjectId(request.subjectId())
                .orElseThrow(() -> GdprKvException.subjectNotFound(request.subjectId()));

        Policy policy = policyAccess.findByPurpose(request.purpose())
                .orElseThrow(() -> GdprKvException.invalidPurpose(request.purpose()));

        long now = clock.millis();
        Optional<Record> existingRecord = recordAccess
                .findBySubjectIdAndRecordKey(request.subjectId(), request.recordKey());

        long createdAt = existingRecord.map(Record::getCreatedAt).orElse(now);
        long version = existingRecord.map(Record::getVersion).orElse(0L) + 1;

        Record.RecordBuilder builder = existingRecord
                .map(Record::toBuilder)
                .orElseGet(() -> Record.builder()
                        .subjectId(request.subjectId())
                        .recordKey(request.recordKey()));

        builder.purpose(request.purpose())
                .value(request.value())
                .createdAt(createdAt)
                .updatedAt(now)
                .version(version)
                .requestId(request.requestId())
                .retentionDays(policy.getRetentionDays());

        if (request.tombstoned()) {
            long tombstonedAt = request.tombstonedAt() != null ? request.tombstonedAt() : now;
            long purgeDueAt = Record.calculatePurgeDueAt(tombstonedAt, policy.getRetentionDays());
            builder.tombstoned(true)
                    .tombstonedAt(tombstonedAt)
                    .purgeDueAt(purgeDueAt)
                    .purgeBucket(Record.formatPurgeBucket(purgeDueAt));
        } else {
            builder.tombstoned(false)
                    .tombstonedAt(null)
                    .purgeDueAt(null)
                    .purgeBucket(null);
        }

        Record toSave = builder.build();
        recordAccess.save(toSave);
        return toSave;
    }

    public List<Record> findAllBySubjectId(String subjectId) {
        Objects.requireNonNull(subjectId, "subjectId");
        return recordAccess.findAllBySubjectId(subjectId);
    }

    /**
     * Deletes a record by tombstoning it and scheduling it for purge based on retention policy.
     * If the record is already tombstoned, returns it without changes.
     * If the record doesn't exist, throws a GdprKvException.
     *
     * @param request the delete request containing subject ID, record key, and request ID
     * @return the tombstoned record
     */
    public Record deleteRecord(DeleteRecordServiceRequest request) {
        Objects.requireNonNull(request, "request");

        // Verify subject exists
        subjectAccess.findBySubjectId(request.subjectId())
                .orElseThrow(() -> GdprKvException.subjectNotFound(request.subjectId()));

        // Find the existing record
        Record existingRecord = recordAccess
                .findBySubjectIdAndRecordKey(request.subjectId(), request.recordKey())
                .orElseThrow(() -> GdprKvException.recordNotFound(request.subjectId(), request.recordKey()));

        // If already tombstoned, return it as-is
        if (existingRecord.getTombstoned() != null && existingRecord.getTombstoned()) {
            return existingRecord;
        }

        // Get the retention policy for this record
        Policy policy = policyAccess.findByPurpose(existingRecord.getPurpose())
                .orElseThrow(() -> GdprKvException.invalidPurpose(existingRecord.getPurpose()));

        long now = clock.millis();
        long purgeDueAt = Record.calculatePurgeDueAt(now, policy.getRetentionDays());

        // Create a tombstoned version of the record
        Record tombstonedRecord = existingRecord.toBuilder()
                .tombstoned(true)
                .tombstonedAt(now)
                .purgeDueAt(purgeDueAt)
                .purgeBucket(Record.formatPurgeBucket(purgeDueAt))
                .requestId(request.requestId())
                .version(existingRecord.getVersion() + 1)
                .updatedAt(now)
                .build();

        recordAccess.save(tombstonedRecord);
        return tombstonedRecord;
    }
}
