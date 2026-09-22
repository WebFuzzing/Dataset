package com.example.gdprkv.service;

import com.example.gdprkv.access.RecordAccess;
import com.example.gdprkv.access.SubjectAccess;
import com.example.gdprkv.models.Record;
import com.example.gdprkv.models.Subject;
import com.example.gdprkv.requests.DeleteRecordServiceRequest;
import com.example.gdprkv.requests.DeleteSubjectServiceRequest;
import com.example.gdprkv.requests.PutSubjectServiceRequest;
import java.time.Clock;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Manages subject metadata, providing a create-once workflow plus lightweight existence checks
 * for downstream services.
 */
@Service
public class SubjectService {

    private final SubjectAccess subjectAccess;
    private final RecordAccess recordAccess;
    private final PolicyDrivenRecordService recordService;
    private final Clock clock;

    @Autowired
    public SubjectService(SubjectAccess subjectAccess,
                         RecordAccess recordAccess,
                         PolicyDrivenRecordService recordService,
                         Clock clock) {
        this.subjectAccess = subjectAccess;
        this.recordAccess = recordAccess;
        this.recordService = recordService;
        this.clock = clock;
    }

    /**
     * Constructor for cases where only subject operations are needed (e.g., tests).
     */
    public SubjectService(SubjectAccess subjectAccess, Clock clock) {
        this(subjectAccess, null, null, clock);
    }

    public Subject putSubject(PutSubjectServiceRequest request) {
        Objects.requireNonNull(request, "request");

        long now = clock.millis();
        Subject subject = Subject.builder()
                .subjectId(request.subjectId())
                .createdAt(now)
                .residency(request.residency())
                .requestId(request.requestId())
                .build();

        try {
            return subjectAccess.save(subject);
        } catch (ConditionalCheckFailedException ex) {
            throw GdprKvException.subjectAlreadyExists(request.subjectId());
        }
    }

    public boolean exists(String subjectId) {
        Objects.requireNonNull(subjectId, "subjectId");
        return subjectAccess.findBySubjectId(subjectId).isPresent();
    }

    /**
     * Deletes a subject by marking it for erasure and tombstoning all associated records.
     * This implements the GDPR right to erasure by:
     * 1. Marking the subject with erasureInProgress=true and erasureRequestedAt timestamp
     * 2. Finding all records for the subject
     * 3. Tombstoning each record for eventual purging
     *
     * @param request the delete request containing subject ID and request ID
     * @return the updated subject with erasure metadata and count of records tombstoned
     */
    public SubjectDeletionResult deleteSubject(DeleteSubjectServiceRequest request) {
        Objects.requireNonNull(request, "request");

        // Find the subject or throw if it doesn't exist
        Subject subject = subjectAccess.findBySubjectId(request.subjectId())
                .orElseThrow(() -> GdprKvException.subjectNotFound(request.subjectId()));

        // Mark subject for erasure
        long now = clock.millis();
        Subject updatedSubject = subject.toBuilder()
                .erasureInProgress(true)
                .erasureRequestedAt(now)
                .requestId(request.requestId())
                .build();
        subjectAccess.update(updatedSubject);

        // Find all records for this subject
        List<Record> records = recordAccess.findAllBySubjectId(request.subjectId());

        // Tombstone each record
        int deletedCount = 0;
        for (Record record : records) {
            // Skip records that are already tombstoned
            if (record.getTombstoned() != null && record.getTombstoned()) {
                continue;
            }

            DeleteRecordServiceRequest deleteRecordRequest = new DeleteRecordServiceRequest(
                    record.getSubjectId(),
                    record.getRecordKey(),
                    request.requestId()
            );
            recordService.deleteRecord(deleteRecordRequest);
            deletedCount++;
        }

        return new SubjectDeletionResult(updatedSubject, deletedCount, records.size());
    }

    /**
     * Result of a subject deletion operation.
     */
    public record SubjectDeletionResult(
            Subject subject,
            int recordsDeleted,
            int totalRecords
    ) { }
}
