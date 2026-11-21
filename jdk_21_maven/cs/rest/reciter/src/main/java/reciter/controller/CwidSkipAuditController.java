package reciter.controller;

import java.net.URI;
import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import reciter.database.dynamodb.model.CwidSkipAudit;
import reciter.service.CwidSkipAuditService;

/**
 * Audit trail for CWIDs that a job run declined to process. Write-once records:
 * a caller (e.g. the institutional client) POSTs a skip event identified by
 * cwid + eventTimestamp (the job run's start time), and a CWID's history can be
 * looked up by GET for troubleshooting. Generic across callers — any job that
 * needs to record a skip supplies its own {@code source}.
 */
@Tag(name = "CwidSkipAuditController", description = "Audit trail for CWIDs skipped by a job run.")
@RestController
@RequestMapping("/reciter/cwid-skip-audits")
public class CwidSkipAuditController {

	private static final Logger log = LoggerFactory.getLogger(CwidSkipAuditController.class);

	@Autowired
	private CwidSkipAuditService cwidSkipAuditService;

	@Operation(summary = "Record a CWID skip-audit event",
			description = "Records that a CWID was skipped during job processing, along with the reason, source "
					+ "system, processing status, and any error details. eventTimestamp is the ISO-8601 UTC start "
					+ "time of the job run that skipped the cwid, supplied by the client, and is required. source "
					+ "identifies the calling system (e.g. institutional-client) and is required. createdTimestamp "
					+ "is stamped by the server and reflects when the row was persisted, not when the skip "
					+ "occurred. The cwid + eventTimestamp pair is write-once.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "201", description = "CwidSkipAudit record created successfully"),
			@ApiResponse(responseCode = "400", description = "The request body failed validation"),
			@ApiResponse(responseCode = "409", description = "A record with the same cwid and eventTimestamp already exists")
	})
	@PostMapping(value = "", produces = "application/json")
	public ResponseEntity<CwidSkipAudit> save(@Valid @RequestBody CwidSkipAudit cwidSkipAudit) {
		cwidSkipAudit.setCreatedTimestamp(Instant.now().toString());
		cwidSkipAuditService.save(cwidSkipAudit);
		log.info("Recorded CwidSkipAudit for cwid={} eventTimestamp={}", cwidSkipAudit.getCwid(),
				cwidSkipAudit.getEventTimestamp());
		URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{cwid}")
				.buildAndExpand(cwidSkipAudit.getCwid()).toUri();
		return ResponseEntity.created(location).body(cwidSkipAudit);
	}

	@Operation(summary = "Find CWID skip-audit records by cwid",
			description = "Returns all skip-audit records for the given cwid, ordered by eventTimestamp. Returns "
					+ "an empty array when the cwid has no recorded skips.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "CwidSkipAudit records for the cwid (possibly empty)"),
			@ApiResponse(responseCode = "400", description = "cwid is blank")
	})
	@GetMapping(value = "/{cwid}", produces = "application/json")
	public ResponseEntity<List<CwidSkipAudit>> findByCwid(@PathVariable("cwid") @NotBlank String cwid) {
		return ResponseEntity.ok(cwidSkipAuditService.findByCwid(cwid));
	}
}
