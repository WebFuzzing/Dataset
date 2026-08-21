package jasper.repository;

import jasper.IntegrationTest;
import jasper.component.ConfigCache;
import jasper.domain.External;
import jasper.domain.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static jasper.security.AuthoritiesConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class UserRepositoryMergeIT {

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
	void testFindAllByQualifiedSuffix_ReturnsMultipleUsers() {
		// Create protected user
		var protectedUser = new User();
		protectedUser.setTag("+user/test");
		protectedUser.setOrigin("");
		protectedUser.setName("Protected User");
		protectedUser.setRole(USER);
		userRepository.save(protectedUser);

		// Create private user with same suffix
		var privateUser = new User();
		privateUser.setTag("_user/test");
		privateUser.setOrigin("");
		privateUser.setName("Private User");
		privateUser.setRole(ADMIN);
		userRepository.save(privateUser);

		var results = userRepository.findAllByQualifiedSuffix("user/test");

		assertThat(results).hasSize(2);
		assertThat(results).extracting(User::getTag)
			.containsExactlyInAnyOrder("+user/test", "_user/test");
	}

	@Test
	void testFindAllByQualifiedSuffix_ProtectedOnly() {
		var protectedUser = new User();
		protectedUser.setTag("+user/test");
		protectedUser.setOrigin("");
		userRepository.save(protectedUser);

		var results = userRepository.findAllByQualifiedSuffix("user/test");

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getTag()).isEqualTo("+user/test");
	}

	@Test
	void testFindAllByQualifiedSuffix_PrivateOnly() {
		var privateUser = new User();
		privateUser.setTag("_user/test");
		privateUser.setOrigin("");
		userRepository.save(privateUser);

		var results = userRepository.findAllByQualifiedSuffix("user/test");

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getTag()).isEqualTo("_user/test");
	}

	@Test
	void testFindAllByQualifiedSuffix_NoResults() {
		var results = userRepository.findAllByQualifiedSuffix("user/nonexistent");

		assertThat(results).isEmpty();
	}

	@Test
	void testFindAllByQualifiedSuffix_DifferentOrigins() {
		// Create users with same tag but different origins
		var user1 = new User();
		user1.setTag("+user/test");
		user1.setOrigin("");
		userRepository.save(user1);

		var user2 = new User();
		user2.setTag("_user/test");
		user2.setOrigin("");
		userRepository.save(user2);

		var user3 = new User();
		user3.setTag("+user/test");
		user3.setOrigin("@other");
		userRepository.save(user3);

		// Query only matches exact qualifiedTag, so different origins won't be merged
		var results = userRepository.findAllByQualifiedSuffix("user/test");

		// Should return user1 and user2 (both with empty origin), but not user3
		assertThat(results).hasSize(2);
		assertThat(results).extracting(User::getTag)
			.containsExactlyInAnyOrder("+user/test", "_user/test");
		assertThat(results).allMatch(u -> u.getOrigin().equals(""));
	}

	@Test
	void testFindAllByOriginAndExternalId_MultipleUsers() {
		// Create first user with external ID
		var user1 = new User();
		user1.setTag("+user/test1");
		user1.setOrigin("");
		user1.setExternal(External.builder().ids(List.of("ext123", "other")).build());
		userRepository.save(user1);

		// Create second user with same external ID
		var user2 = new User();
		user2.setTag("_user/test2");
		user2.setOrigin("");
		user2.setExternal(External.builder().ids(List.of("ext123", "another")).build());
		userRepository.save(user2);

		var results = userRepository.findAllByOriginAndExternalId("", "ext123");

		assertThat(results).hasSize(2);
		assertThat(results).extracting(User::getTag)
			.containsExactlyInAnyOrder("+user/test1", "_user/test2");
	}

	@Test
	void testFindAllByOriginAndExternalId_SingleUser() {
		var user = new User();
		user.setTag("+user/test");
		user.setOrigin("");
		user.setExternal(External.builder().ids(List.of("ext123")).build());
		userRepository.save(user);

		var results = userRepository.findAllByOriginAndExternalId("", "ext123");

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getTag()).isEqualTo("+user/test");
	}

	@Test
	void testFindAllByOriginAndExternalId_NoResults() {
		var results = userRepository.findAllByOriginAndExternalId("", "nonexistent");

		assertThat(results).isEmpty();
	}

	@Test
	void testFindAllByOriginAndExternalId_FiltersByOrigin() {
		var user1 = new User();
		user1.setTag("+user/test1");
		user1.setOrigin("");
		user1.setExternal(External.builder().ids(List.of("ext123")).build());
		userRepository.save(user1);

		var user2 = new User();
		user2.setTag("+user/test2");
		user2.setOrigin("@other");
		user2.setExternal(External.builder().ids(List.of("ext123")).build());
		userRepository.save(user2);

		var results = userRepository.findAllByOriginAndExternalId("", "ext123");

		assertThat(results).hasSize(1);
		assertThat(results.get(0).getTag()).isEqualTo("+user/test1");
	}

	@Test
	void testFindAllByOriginAndExternalId_MultipleExternalIds() {
		var user = new User();
		user.setTag("+user/test");
		user.setOrigin("");
		user.setExternal(External.builder().ids(List.of("ext1", "ext2", "ext3")).build());
		userRepository.save(user);

		// Should find user with any of its external IDs
		var results1 = userRepository.findAllByOriginAndExternalId("", "ext1");
		var results2 = userRepository.findAllByOriginAndExternalId("", "ext2");
		var results3 = userRepository.findAllByOriginAndExternalId("", "ext3");

		assertThat(results1).hasSize(1);
		assertThat(results2).hasSize(1);
		assertThat(results3).hasSize(1);
		assertThat(results1.get(0).getTag()).isEqualTo("+user/test");
		assertThat(results2.get(0).getTag()).isEqualTo("+user/test");
		assertThat(results3.get(0).getTag()).isEqualTo("+user/test");
	}

	@Test
	void testFindAllByOriginAndExternalId_OrderedByTag() {
		var user1 = new User();
		user1.setTag("+user/zebra");
		user1.setOrigin("");
		user1.setExternal(External.builder().ids(List.of("ext123")).build());
		userRepository.save(user1);

		var user2 = new User();
		user2.setTag("+user/alpha");
		user2.setOrigin("");
		user2.setExternal(External.builder().ids(List.of("ext123")).build());
		userRepository.save(user2);

		var user3 = new User();
		user3.setTag("+user/beta");
		user3.setOrigin("");
		user3.setExternal(External.builder().ids(List.of("ext123")).build());
		userRepository.save(user3);

		var results = userRepository.findAllByOriginAndExternalId("", "ext123");

		assertThat(results).hasSize(3);
		// Database sorts by tag with String.compareTo() semantics
		assertThat(results).extracting(User::getTag)
			.containsExactly("+user/alpha", "+user/beta", "+user/zebra");
	}

	@Test
	void testFindAllByQualifiedSuffix_OrderedByTag() {
		// Create public user
		var publicUser = new User();
		publicUser.setTag("+user/test");
		publicUser.setOrigin("");
		userRepository.save(publicUser);

		// Create private user with same suffix
		var privateUser = new User();
		privateUser.setTag("_user/test");
		privateUser.setOrigin("");
		userRepository.save(privateUser);

		var results = userRepository.findAllByQualifiedSuffix("user/test");

		assertThat(results).hasSize(2);
		// Database sorts by tag: + comes before _ in ASCII
		assertThat(results).extracting(User::getTag)
			.containsExactly("+user/test", "_user/test");
	}

	@Test
	void testFindAllByOriginAndExternalId_JpqlWithJsonbExtractPath() {
		// This test verifies that the JPQL query using jsonb_extract_path works correctly
		// with the custom PostgreSQLDialect function registration. The query uses:
		// jsonb_exists(jsonb_extract_path(u.external, 'ids'), :externalId)
		// which requires both functions to be registered in PostgreSQLDialect.

		var user1 = new User();
		user1.setTag("+user/test1");
		user1.setOrigin("");
		user1.setExternal(External.builder().ids(List.of("github123", "google456")).build());
		userRepository.save(user1);

		var user2 = new User();
		user2.setTag("_user/test2");
		user2.setOrigin("");
		user2.setExternal(External.builder().ids(List.of("github123", "facebook789")).build());
		userRepository.save(user2);

		var user3 = new User();
		user3.setTag("+user/test3");
		user3.setOrigin("");
		user3.setExternal(External.builder().ids(List.of("twitter101")).build());
		userRepository.save(user3);

		// Query should find users with external ID "github123"
		var results = userRepository.findAllByOriginAndExternalId("", "github123");

		assertThat(results).hasSize(2);
		assertThat(results).extracting(User::getTag)
			.containsExactlyInAnyOrder("+user/test1", "_user/test2");
		assertThat(results).allMatch(u -> u.getExternal().getIds().contains("github123"));
	}

	@Test
	void testFindAllByOriginAndExternalId_MultipleAccountsOrderedByTag() {
		// Create multiple users with different tag prefixes sharing the same external ID
		var user1 = new User();
		user1.setTag("_user/charlie");
		user1.setOrigin("");
		user1.setExternal(External.builder().ids(List.of("shared123")).build());
		userRepository.save(user1);

		var user2 = new User();
		user2.setTag("+user/alice");
		user2.setOrigin("");
		user2.setExternal(External.builder().ids(List.of("shared123")).build());
		userRepository.save(user2);

		var user3 = new User();
		user3.setTag("_user/bob");
		user3.setOrigin("");
		user3.setExternal(External.builder().ids(List.of("shared123")).build());
		userRepository.save(user3);

		var user4 = new User();
		user4.setTag("+user/dave");
		user4.setOrigin("");
		user4.setExternal(External.builder().ids(List.of("shared123")).build());
		userRepository.save(user4);

		var results = userRepository.findAllByOriginAndExternalId("", "shared123");

		assertThat(results).hasSize(4);
		// Database sorts by tag: + comes before _ in ASCII, then alphabetically
		assertThat(results).extracting(User::getTag)
			.containsExactly("+user/alice", "+user/dave", "_user/bob", "_user/charlie");
		assertThat(results).allMatch(u -> u.getExternal().getIds().contains("shared123"));
	}
}
