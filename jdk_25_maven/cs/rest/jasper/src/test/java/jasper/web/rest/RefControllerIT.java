package jasper.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import jasper.IntegrationTest;
import jasper.component.ConfigCache;
import jasper.domain.Ref;
import jasper.repository.RefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link RefController}.
 * Tests validation of fully qualified tags in Ref operations.
 */
@WithMockUser(value = "+user/tester@a", roles = {"ADMIN"})
@AutoConfigureMockMvc
@IntegrationTest
class RefControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private RefRepository refRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private ConfigCache configCache;

	private static final String URL = "https://www.example.com/";

	@BeforeEach
	void setup() {
		refRepository.deleteAll();
		configCache.clearUserCache();
		configCache.clearPluginCache();
		configCache.clearTemplateCache();
	}

	@Test
	void testCreateRefWithFullyQualifiedTagShouldFail() throws Exception {
		// Test that creating a Ref with a fully qualified tag (containing @) in tags list fails validation
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTags(List.of("test@origin")); // Fully qualified tag with @

		mockMvc
			.perform(post("/api/v1/ref")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ref))
				.with(csrf().asHeader()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void testCreateRefWithFullyQualifiedTagAtSignOnlyShouldFail() throws Exception {
		// Test that creating a Ref with a tag ending in @ fails validation
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTags(List.of("test@")); // Tag with @ at end

		mockMvc
			.perform(post("/api/v1/ref")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ref))
				.with(csrf().asHeader()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void testCreateRefWithFullyQualifiedTagWithFullOriginShouldFail() throws Exception {
		// Test that creating a Ref with a fully qualified tag with full origin fails validation
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTags(List.of("test@remote.example.com")); // Fully qualified tag with full origin

		mockMvc
			.perform(post("/api/v1/ref")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ref))
				.with(csrf().asHeader()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void testCreateRefWithValidLocalTagShouldSucceed() throws Exception {
		// Test that creating a Ref with a valid local tag (no @) succeeds
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTags(List.of("test")); // Valid local tag

		mockMvc
			.perform(post("/api/v1/ref")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ref))
				.with(csrf().asHeader()))
			.andExpect(status().isCreated());
	}

	@Test
	void testCreateRefWithMultipleTagsIncludingFullyQualifiedShouldFail() throws Exception {
		// Test that creating a Ref with multiple tags where one is fully qualified fails
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTags(List.of("valid", "test@origin", "another")); // One fully qualified tag

		mockMvc
			.perform(post("/api/v1/ref")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ref))
				.with(csrf().asHeader()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void testUpdateRefWithFullyQualifiedTagShouldFail() throws Exception {
		// First create a valid ref
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTags(new ArrayList<>(List.of("public")));
		refRepository.save(ref);

		// Try to update with a fully qualified tag
		ref.setTags(List.of("test@origin"));

		mockMvc
			.perform(put("/api/v1/ref")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ref))
				.with(csrf().asHeader()))
			.andExpect(status().isBadRequest());
	}

	@Test
	void testCreateRefWithDifferentOriginThanLocalOriginHeaderShouldFail() throws Exception {
		// Test that creating a Ref with origin @b when Local-Origin is @a is rejected
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setOrigin("@b");
		ref.setTags(List.of("public"));

		mockMvc
			.perform(post("/api/v1/ref")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ref))
				.header("Local-Origin", "@a")
				.with(csrf().asHeader()))
			.andExpect(status().isForbidden());

		assertThat(refRepository.count()).isZero();
	}

	@Test
	void testCreateRefWithParentOriginFromSubOriginShouldFail() throws Exception {
		// Test that creating a Ref with origin @a when Local-Origin is @a.b is rejected
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setOrigin("@a");
		ref.setTags(List.of("public"));

		mockMvc
			.perform(post("/api/v1/ref")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ref))
				.header("Local-Origin", "@a.b")
				.with(csrf().asHeader()))
			.andExpect(status().isForbidden());

		assertThat(refRepository.count()).isZero();
	}

	@Test
	void testUpdateRefWithDifferentOriginThanLocalOriginHeaderShouldFail() throws Exception {
		// First create a valid ref at origin @b
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setOrigin("@b");
		ref.setTags(new ArrayList<>(List.of("public")));
		refRepository.save(ref);

		// Try to update with Local-Origin @a but ref origin @b
		ref.setTags(new ArrayList<>(List.of("public", "test")));

		mockMvc
			.perform(put("/api/v1/ref")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(ref))
				.header("Local-Origin", "@a")
				.with(csrf().asHeader()))
			.andExpect(status().isForbidden());

		var existing = refRepository.findOneByUrlAndOrigin(URL, "@b");
		assertThat(existing).isPresent();
		assertThat(existing.get().getTags()).containsExactly("public");
	}

	@Test
	void testPatchRefWithDifferentOriginThanLocalOriginHeaderShouldFail() throws Exception {
		// First create a valid ref at origin @b
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setOrigin("@b");
		ref.setTitle("original");
		ref.setTags(new ArrayList<>(List.of("public")));
		refRepository.save(ref);

		// Try to patch with Local-Origin @a but ref origin @b
		var patch = "[{\"op\":\"replace\",\"path\":\"/title\",\"value\":\"hacked\"}]";

		mockMvc
			.perform(patch("/api/v1/ref")
				.param("url", URL)
				.param("origin", "@b")
				.param("cursor", ref.getModified().toString())
				.contentType("application/json-patch+json")
				.content(patch)
				.header("Local-Origin", "@a")
				.with(csrf().asHeader()))
			.andExpect(status().isForbidden());

		var existing = refRepository.findOneByUrlAndOrigin(URL, "@b");
		assertThat(existing).isPresent();
		assertThat(existing.get().getTitle()).isEqualTo("original");
	}

	@Test
	void testDeleteRefWithDifferentOriginThanLocalOriginHeaderShouldFail() throws Exception {
		// First create a valid ref at origin @b
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setOrigin("@b");
		ref.setTags(new ArrayList<>(List.of("public")));
		refRepository.save(ref);

		// Try to delete with Local-Origin @a but ref origin @b
		mockMvc
			.perform(delete("/api/v1/ref")
				.param("url", URL)
				.param("origin", "@b")
				.header("Local-Origin", "@a")
				.with(csrf().asHeader()))
			.andExpect(status().isForbidden());

		assertThat(refRepository.findOneByUrlAndOrigin(URL, "@b")).isPresent();
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"MOD"})
	void testSearchWithUrlAndRankSortShouldSucceed() throws Exception {
		var ref = new Ref();
		ref.setUrl("http://repl-web");
		ref.setTitle("Repl Web App");
		ref.setTags(new ArrayList<>(List.of("public")));
		refRepository.save(ref);

		mockMvc
			.perform(get("/api/v1/ref/page")
				.param("sort", "rank,DESC")
				.param("search", "http://repl-web")
				.param("size", "24")
				.accept(MediaType.APPLICATION_JSON))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.content[0].url").value("http://repl-web"));
	}
}
