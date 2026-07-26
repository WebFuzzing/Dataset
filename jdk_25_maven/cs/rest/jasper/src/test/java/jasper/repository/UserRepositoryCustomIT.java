package jasper.repository;

import jasper.IntegrationTest;
import jasper.component.ConfigCache;
import jasper.domain.TagId;
import jasper.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static jasper.security.AuthoritiesConstants.USER;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class UserRepositoryCustomIT {

	@Autowired
	UserRepository userRepository;

	@Autowired
	ConfigCache configCache;

	@BeforeEach
	void init() {
		userRepository.deleteAll();
		configCache.clearUserCache();
		configCache.clearPluginCache();
		configCache.clearTemplateCache();
	}

	@Test
	void testSetExternalId_AddsIdToEmptyExternal() {
		var user = new User();
		user.setTag("+user/test");
		user.setOrigin("");
		user.setRole(USER);
		userRepository.save(user);

		int updated = userRepository.setExternalId("+user/test", "", "ext123");

		assertThat(updated).isEqualTo(1);

		var result = userRepository.findById(new TagId("+user/test", "")).orElseThrow();
		assertThat(result.getExternal()).isNotNull();
		assertThat(result.getExternal().getIds()).contains("ext123");
	}

	@Test
	void testSetExternalId_DoesNotDuplicateExistingId() {
		var user = new User();
		user.setTag("+user/test");
		user.setOrigin("");
		user.setRole(USER);
		userRepository.save(user);

		// Set ID first time
		userRepository.setExternalId("+user/test", "", "ext123");

		// Try to set same ID again
		int updated = userRepository.setExternalId("+user/test", "", "ext123");

		assertThat(updated).isEqualTo(0);

		var result = userRepository.findById(new TagId("+user/test", "")).orElseThrow();
		assertThat(result.getExternal().getIds()).containsExactly("ext123");
	}

	@Test
	void testSetExternalId_AddsMultipleIds() {
		var user = new User();
		user.setTag("+user/test");
		user.setOrigin("");
		user.setRole(USER);
		userRepository.save(user);

		userRepository.setExternalId("+user/test", "", "ext1");
		userRepository.setExternalId("+user/test", "", "ext2");

		var result = userRepository.findById(new TagId("+user/test", "")).orElseThrow();
		assertThat(result.getExternal().getIds()).containsExactlyInAnyOrder("ext1", "ext2");
	}

	@Test
	void testSetExternalId_NoMatchReturnsZero() {
		int updated = userRepository.setExternalId("+user/nonexistent", "", "ext123");

		assertThat(updated).isEqualTo(0);
	}
}
