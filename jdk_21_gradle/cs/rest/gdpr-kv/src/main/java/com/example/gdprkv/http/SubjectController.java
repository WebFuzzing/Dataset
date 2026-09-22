package com.example.gdprkv.http;

import com.example.gdprkv.models.Subject;
import com.example.gdprkv.requests.DeleteSubjectServiceRequest;
import com.example.gdprkv.requests.PutSubjectHttpRequest;
import com.example.gdprkv.requests.PutSubjectServiceRequest;
import com.example.gdprkv.service.AuditLogService;
import com.example.gdprkv.service.SubjectService;
import com.example.gdprkv.service.SubjectService.SubjectDeletionResult;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for managing subject metadata. Subjects are immutable after creation;
 * attempting to recreate an existing subject yields a conflict response.
 */
@RestController
public class SubjectController {

    private final SubjectService subjectService;
    private final AuditLogService auditLogService;

    public SubjectController(SubjectService subjectService, AuditLogService auditLogService) {
        this.subjectService = subjectService;
        this.auditLogService = auditLogService;
    }

    @PutMapping("/subjects/{subjectId}")
    public ResponseEntity<SubjectResponse> putSubject(
            @PathVariable String subjectId,
            @RequestBody(required = false) PutSubjectHttpRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        String residency = request != null ? request.residency() : null;
        String effectiveRequestId = (requestId == null || requestId.isBlank())
                ? UUID.randomUUID().toString()
                : requestId;

        auditLogService.recordCreateSubjectRequested(subjectId, effectiveRequestId);

        Subject subject;
        try {
            subject = subjectService.putSubject(
                    new PutSubjectServiceRequest(subjectId, residency, effectiveRequestId));
        } catch (RuntimeException ex) {
            auditLogService.recordCreateSubjectFailure(subjectId, effectiveRequestId, ex.getMessage());
            throw ex;
        }

        auditLogService.recordCreateSubjectSuccess(subject);

        return ResponseEntity.ok()
                .header("X-Request-Id", subject.getRequestId())
                .body(map(subject));
    }

    @DeleteMapping("/subjects/{subjectId}")
    public ResponseEntity<SubjectDeletionResponse> deleteSubject(
            @PathVariable String subjectId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        String effectiveRequestId = (requestId == null || requestId.isBlank())
                ? UUID.randomUUID().toString()
                : requestId;

        DeleteSubjectServiceRequest deleteRequest = new DeleteSubjectServiceRequest(
                subjectId, effectiveRequestId);

        // Capture the client intent before we validate or attempt deletion.
        auditLogService.recordSubjectErasureRequested(subjectId, effectiveRequestId);

        SubjectDeletionResult result;
        try {
            result = subjectService.deleteSubject(deleteRequest);
            auditLogService.recordSubjectErasureStarted(subjectId, effectiveRequestId, result.totalRecords());
        } catch (RuntimeException ex) {
            // Persist a failure event so the audit log reflects the rejected deletion.
            auditLogService.recordSubjectErasureFailure(subjectId, effectiveRequestId, ex.getMessage());
            throw ex;
        }

        // Log completion with the count of records deleted
        auditLogService.recordSubjectErasureCompleted(subjectId, effectiveRequestId, result.recordsDeleted());

        return ResponseEntity.ok()
                .header("X-Request-Id", effectiveRequestId)
                .body(new SubjectDeletionResponse(
                        map(result.subject()),
                        result.recordsDeleted(),
                        result.totalRecords()
                ));
    }

    private SubjectResponse map(Subject subject) {
        return new SubjectResponse(
                subject.getSubjectId(),
                subject.getCreatedAt(),
                subject.getResidency(),
                subject.getErasureInProgress(),
                subject.getErasureRequestedAt()
        );
    }
}
