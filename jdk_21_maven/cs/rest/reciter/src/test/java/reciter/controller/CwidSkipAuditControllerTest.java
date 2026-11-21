package reciter.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import reciter.database.dynamodb.model.CwidSkipAudit;
import reciter.service.CwidSkipAuditService;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

/**
 * Covers POST /reciter/cwid-skip-audits and GET /reciter/cwid-skip-audits/{cwid}
 * via standalone MockMvc so Bean Validation, the controller advice, the 201
 * status, and the Location header are all actually exercised end to end.
 */
@ExtendWith(MockitoExtension.class)
public class CwidSkipAuditControllerTest {

	private static final String BASE_PATH = "/reciter/cwid-skip-audits";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private CwidSkipAuditService cwidSkipAuditService;

	@InjectMocks
	private CwidSkipAuditController cwidSkipAuditController;

	private MockMvc mockMvc() {
		return MockMvcBuilders.standaloneSetup(cwidSkipAuditController).setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	private CwidSkipAudit request(String cwid, String eventTimestamp, String skipReason, String source) {
		CwidSkipAudit body = new CwidSkipAudit();
		body.setCwid(cwid);
		body.setEventTimestamp(eventTimestamp);
		body.setSkipReason(skipReason);
		body.setSource(source);
		body.setProcessingStatus("SKIPPED");
		return body;
	}

	@Test
	public void validPostReturnsCreatedWithLocationAndPersistsRecord() throws Exception {
		CwidSkipAudit body = request("abc1001", "2026-08-27T12:00:00Z", "inactive in CTSC feed", "ctsc");

		mockMvc().perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)))
				.andExpect(status().isCreated())
				.andExpect(header().string("Location", endsWith(BASE_PATH + "/abc1001")));

		ArgumentCaptor<CwidSkipAudit> saved = ArgumentCaptor.forClass(CwidSkipAudit.class);
		verify(cwidSkipAuditService).save(saved.capture());
		assertNotNull(saved.getValue().getCreatedTimestamp());
		assertFalse(saved.getValue().getCreatedTimestamp().isBlank());
		assertEquals("2026-08-27T12:00:00Z", saved.getValue().getEventTimestamp());
	}

	@Test
	public void postMissingEventTimestampIsBadRequest() throws Exception {
		CwidSkipAudit body = request("abc1001", null, "inactive in CTSC feed", "ctsc");

		mockMvc().perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body))).andExpect(status().isBadRequest());

		verify(cwidSkipAuditService, never()).save(any());
	}

	@Test
	public void postMissingSourceIsBadRequest() throws Exception {
		CwidSkipAudit body = request("abc1001", "2026-08-27T12:00:00Z", "inactive in CTSC feed", null);

		mockMvc().perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body))).andExpect(status().isBadRequest());

		verify(cwidSkipAuditService, never()).save(any());
	}

	@Test
	public void postBlankCwidIsBadRequest() throws Exception {
		CwidSkipAudit body = request("  ", "2026-08-27T12:00:00Z", "inactive in CTSC feed", "ctsc");

		mockMvc().perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body))).andExpect(status().isBadRequest());

		verify(cwidSkipAuditService, never()).save(any());
	}

	@Test
	public void postDuplicateKeyIsConflict() throws Exception {
		CwidSkipAudit body = request("abc1001", "2026-08-27T12:00:00Z", "inactive in CTSC feed", "ctsc");
		doThrow(ConditionalCheckFailedException.builder().message("x").build()).when(cwidSkipAuditService)
				.save(any());

		mockMvc().perform(post(BASE_PATH).contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body))).andExpect(status().isConflict());
	}

	@Test
	public void getWithRecordsReturnsList() throws Exception {
		CwidSkipAudit record = request("abc1001", "2026-08-27T12:00:00Z", "inactive in CTSC feed", "ctsc");
		when(cwidSkipAuditService.findByCwid(eq("abc1001"))).thenReturn(List.of(record));

		mockMvc().perform(get(BASE_PATH + "/abc1001")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].cwid").value("abc1001"));
	}

	@Test
	public void getWithNoRecordsReturnsEmptyList() throws Exception {
		when(cwidSkipAuditService.findByCwid(eq("nobody"))).thenReturn(Collections.emptyList());

		mockMvc().perform(get(BASE_PATH + "/nobody")).andExpect(status().isOk())
				.andExpect(jsonPath("$", hasSize(0)));
	}

	@Test
	public void getWithWhitespaceCwidIsBadRequest() throws Exception {
		// URI-template form so MockMvc encodes the blank to %20 and the path variable is genuinely " ".
		mockMvc().perform(get(BASE_PATH + "/{cwid}", " ")).andExpect(status().isBadRequest());

		verify(cwidSkipAuditService, never()).findByCwid(any());
	}
}
