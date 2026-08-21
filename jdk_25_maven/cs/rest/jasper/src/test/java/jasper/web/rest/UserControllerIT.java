package jasper.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import jasper.IntegrationTest;
import jasper.domain.User;
import jasper.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for {@link UserController}.
 * Tests cross-origin write prevention with Local-Origin header.
 */
@WithMockUser(value = "+user/tester@a", roles = {"ADMIN"})
@AutoConfigureMockMvc
@IntegrationTest
class UserControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ObjectMapper objectMapper;

	@BeforeEach
	void setup() {
		userRepository.deleteAll();
	}

	@Test
	void testCreateUserWithDifferentOriginThanLocalOriginHeaderShouldFail() throws Exception {
		var user = new User();
		user.setTag("+user/test");
		user.setOrigin("@b");
		user.setRole("ROLE_USER");

		mockMvc
			.perform(post("/api/v1/user")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(user))
				.header("Local-Origin", "@a")
				.with(csrf().asHeader()))
			.andExpect(status().isForbidden());

		assertThat(userRepository.count()).isZero();
	}

	@Test
	void testCreateUserWithParentOriginFromSubOriginShouldFail() throws Exception {
		var user = new User();
		user.setTag("+user/test");
		user.setOrigin("@a");
		user.setRole("ROLE_USER");

		mockMvc
			.perform(post("/api/v1/user")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(user))
				.header("Local-Origin", "@a.b")
				.with(csrf().asHeader()))
			.andExpect(status().isForbidden());

		assertThat(userRepository.count()).isZero();
	}

	@Test
	void testUpdateUserWithDifferentOriginThanLocalOriginHeaderShouldFail() throws Exception {
		var user = new User();
		user.setTag("+user/test");
		user.setOrigin("@b");
		user.setRole("ROLE_USER");
		userRepository.save(user);

		user.setRole("ROLE_EDITOR");

		mockMvc
			.perform(put("/api/v1/user")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(user))
				.header("Local-Origin", "@a")
				.with(csrf().asHeader()))
			.andExpect(status().isForbidden());

		var existing = userRepository.findOneByQualifiedTag("+user/test@b");
		assertThat(existing).isPresent();
		assertThat(existing.get().getRole()).isEqualTo("ROLE_USER");
	}
}
