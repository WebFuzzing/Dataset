package com.example.gdprkv.http;

import com.example.gdprkv.models.Record;
import com.example.gdprkv.requests.DeleteRecordServiceRequest;
import com.example.gdprkv.requests.PutRecordHttpRequest;
import com.example.gdprkv.requests.PutRecordServiceRequest;
import com.example.gdprkv.service.AuditLogService;
import com.example.gdprkv.service.PolicyDrivenRecordService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for record writes. Accepts HTTP payloads, converts them into service commands,
 * invokes the policy-aware record service (which requires the subject to exist), and records
 * audit events for success/failure paths.
 */
@RestController
public class RecordController {

    private final PolicyDrivenRecordService recordService;
    private final AuditLogService auditLogService;

    public RecordController(PolicyDrivenRecordService recordService,
                           AuditLogService auditLogService) {
        this.recordService = recordService;
        this.auditLogService = auditLogService;
    }

    @PutMapping("/subjects/{subjectId}/records/{recordKey}")
    public ResponseEntity<RecordResponse> putRecord(
            @PathVariable String subjectId,
            @PathVariable String recordKey,
            @Valid @RequestBody PutRecordHttpRequest request
    ) {

        PutRecordServiceRequest writeRequest = new PutRecordServiceRequest(
                subjectId,
                recordKey,
                request.purpose(),
                request.value()
        );

        // Capture the client intent before we validate or attempt writes.
        auditLogService.recordPutRequested(subjectId, recordKey, request.purpose(), writeRequest.requestId());

        Record record;
        try {
            record = recordService.putRecord(writeRequest);
        } catch (RuntimeException ex) {
            // Persist a failure event so the audit log reflects the rejected write.
            auditLogService.recordPutFailure(subjectId, recordKey, request.purpose(), writeRequest.requestId(), ex.getMessage());
            throw ex;
        }

        // Final success audit indicates whether this was a new item or an update.
        auditLogService.recordPutSuccess(record);

        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, record.getVersion().toString())
                .body(map(record));
    }

    @GetMapping("/subjects/{subjectId}/records")
    public ResponseEntity<List<RecordResponse>> getAllRecords(@PathVariable String subjectId) {
        List<Record> records = recordService.findAllBySubjectId(subjectId);
        List<RecordResponse> response = records.stream()
                .map(this::map)
                .toList();
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/subjects/{subjectId}/records/{recordKey}")
    public ResponseEntity<RecordResponse> deleteRecord(
            @PathVariable String subjectId,
            @PathVariable String recordKey
    ) {
        DeleteRecordServiceRequest deleteRequest = new DeleteRecordServiceRequest(subjectId, recordKey);

        // Capture the client intent before we validate or attempt deletion.
        auditLogService.recordDeleteRequested(subjectId, recordKey, deleteRequest.requestId());

        Record record;
        try {
            record = recordService.deleteRecord(deleteRequest);
        } catch (RuntimeException ex) {
            // Persist a failure event so the audit log reflects the rejected deletion.
            auditLogService.recordDeleteFailure(subjectId, recordKey, deleteRequest.requestId(), ex.getMessage());
            throw ex;
        }

        // Check if record was already tombstoned
        if (record.getTombstoned() != null && record.getTombstoned()
                && !record.getRequestId().equals(deleteRequest.requestId())) {
            // Record was already tombstoned in a previous request
            auditLogService.recordDeleteAlreadyTombstoned(subjectId, recordKey, deleteRequest.requestId());
        } else {
            // Successfully tombstoned this request
            auditLogService.recordDeleteSuccess(record);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, record.getVersion().toString())
                .body(map(record));
    }

    private RecordResponse map(Record record) {
        JsonNode value = record.getValue();
        return new RecordResponse(
                record.getSubjectId(),
                record.getRecordKey(),
                record.getPurpose(),
                value,
                record.getVersion(),
                record.getCreatedAt(),
                record.getUpdatedAt(),
                record.getRetentionDays()
        );
    }
}
