package reciter.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.ObjectMapper;

import reciter.database.dynamodb.model.ESearchResult;
import reciter.service.ESearchResultService;

/**
 * Covers GET /reciter/esearchresult/{uid} and POST
 * /reciter/esearchresult/retrieved-pmids via standalone MockMvc so Bean
 * Validation and the controller advice are exercised end to end -- Mockito unit
 * tests calling the controller method directly cannot exercise declarative
 * {@code @NotBlank}/{@code @NotEmpty}/{@code @Size} validation.
 */
@ExtendWith(MockitoExtension.class)
public class ReCiterControllerESearchResultMvcTest {

	private static final String BASE_PATH = "/reciter/esearchresult";

	private final ObjectMapper objectMapper = new ObjectMapper();

	@Mock
	private ESearchResultService eSearchResultService;

	@InjectMocks
	private ReCiterController reCiterController;

	private MockMvc mockMvc() {
		return MockMvcBuilders.standaloneSetup(reCiterController).setControllerAdvice(new ApiExceptionHandler())
				.build();
	}

	@Test
	public void getWithRecordReturnsOk() throws Exception {
		ESearchResult record = new ESearchResult();
		record.setUid("test123");
		when(eSearchResultService.findByUid("test123")).thenReturn(record);

		mockMvc().perform(get(BASE_PATH + "/{uid}", "test123")).andExpect(status().isOk())
				.andExpect(jsonPath("$.uid").value("test123"));
	}

	@Test
	public void getWithNoRecordReturnsNotFound() throws Exception {
		when(eSearchResultService.findByUid("nonexistent")).thenReturn(null);

		mockMvc().perform(get(BASE_PATH + "/{uid}", "nonexistent")).andExpect(status().isNotFound());
	}

	@Test
	public void getWithWhitespaceUidIsBadRequest() throws Exception {
		// URI-template form so MockMvc encodes the blank to %20 and the path variable is genuinely " ".
		mockMvc().perform(get(BASE_PATH + "/{uid}", " ")).andExpect(status().isBadRequest());

		verify(eSearchResultService, never()).findByUid(any());
	}

	@Test
	public void postReturnsPmidMap() throws Exception {
		Map<String, List<Long>> pmidsByUid = Map.of("a", List.of(1L, 2L), "b", Collections.emptyList());
		when(eSearchResultService.findRetrievedPmidsByUids(List.of("a", "b"))).thenReturn(pmidsByUid);

		mockMvc().perform(post(BASE_PATH + "/retrieved-pmids").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(List.of("a", "b")))).andExpect(status().isOk())
				.andExpect(jsonPath("$.a[0]").value(1)).andExpect(jsonPath("$.a[1]").value(2))
				.andExpect(jsonPath("$.b", hasSize(0)));

		verify(eSearchResultService).findRetrievedPmidsByUids(List.of("a", "b"));
	}

	@Test
	public void postEmptyListIsBadRequest() throws Exception {
		mockMvc().perform(post(BASE_PATH + "/retrieved-pmids").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Collections.emptyList())))
				.andExpect(status().isBadRequest());

		verify(eSearchResultService, never()).findRetrievedPmidsByUids(any());
	}

	@Test
	public void postExceedsMaxIsBadRequest() throws Exception {
		List<String> tooManyUids = new ArrayList<>();
		for (int i = 0; i < 1001; i++) {
			tooManyUids.add("uid" + i);
		}

		mockMvc().perform(post(BASE_PATH + "/retrieved-pmids").contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(tooManyUids))).andExpect(status().isBadRequest());

		verify(eSearchResultService, never()).findRetrievedPmidsByUids(any());
	}

	@Test
	public void postWithNoBodyIsBadRequest() throws Exception {
		mockMvc().perform(post(BASE_PATH + "/retrieved-pmids")).andExpect(status().isBadRequest());
	}
}
