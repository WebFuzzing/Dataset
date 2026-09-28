package com.example.gdprkv.http;

import com.example.gdprkv.access.AuditEventAccess;
import com.example.gdprkv.models.AuditEvent;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST entry point for retrieving audit events. Provides read-only access to the audit trail
 * for compliance, debugging, and subject access requests.
 */
@RestController
public class AuditEventController {

    private final AuditEventAccess auditEventAccess;

    public AuditEventController(AuditEventAccess auditEventAccess) {
        this.auditEventAccess = auditEventAccess;
    }

    @GetMapping("/subjects/{subjectId}/audit-events")
    public ResponseEntity<List<AuditEventResponse>> getAllAuditEvents(@PathVariable String subjectId) {
        List<AuditEvent> events = auditEventAccess.findAllBySubjectId(subjectId);
        List<AuditEventResponse> response = events.stream()
                .map(this::map)
                .toList();
        return ResponseEntity.ok(response);
    }

    private AuditEventResponse map(AuditEvent event) {
        return new AuditEventResponse(
                event.getSubjectId(),
                event.getTsUlid(),
                event.getEventType(),
                event.getRequestId(),
                event.getTimestamp(),
                event.getPrevHash(),
                event.getHash(),
                event.getItemKey(),
                event.getPurpose(),
                event.getDetails()
        );
    }
}
