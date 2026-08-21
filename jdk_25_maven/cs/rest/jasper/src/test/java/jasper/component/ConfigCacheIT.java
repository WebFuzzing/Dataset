package jasper.component;

import jasper.IntegrationTest;
import jasper.domain.External;
import jasper.domain.User;
import jasper.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static jasper.security.AuthoritiesConstants.*;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class ConfigCacheIT {

	@Autowired
	ConfigCache configCache;

	@Autowired
	UserRepository userRepository;

	@BeforeEach
	void init() {
		userRepository.deleteAll();
		configCache.clearUserCache();
	}

	@Test
	void testGetUser_MergesProtectedAndPrivateUsers() {
		// Create protected user
		var protectedUser = new User();
		protectedUser.setTag("+user/test");
		protectedUser.setOrigin("");
		protectedUser.setName("Protected User");
		protectedUser.setRole(USER);
		protectedUser.setReadAccess(List.of("tag1", "tag2"));
		userRepository.save(protectedUser);

		// Create private user with same base tag
		var privateUser = new User();
		privateUser.setTag("_user/test");
		privateUser.setOrigin("");
		privateUser.setRole(ADMIN);
		privateUser.setReadAccess(List.of("tag2", "tag3"));
		privateUser.setWriteAccess(List.of("tag4"));
		userRepository.save(privateUser);

		// Query with protected prefix
		var result = configCache.getUser("+user/test");

		assertThat(result).isNotNull();
		// Should preserve first user's basic properties
		assertThat(result.getTag()).isEqualTo("+user/test");
		assertThat(result.getName()).isEqualTo("Protected User");
		// Should take highest role
		assertThat(result.getRole()).isEqualTo(ADMIN);
		// Should merge and deduplicate access lists
		assertThat(result.getReadAccess()).containsExactlyInAnyOrder("tag1", "tag2", "tag3");
		assertThat(result.getWriteAccess()).containsExactlyInAnyOrder("_user/test", "tag4");
	}

	@Test
	void testGetUser_MergesPrivateAndProtectedUsers() {
		// Create users in different order
		var privateUser = new User();
		privateUser.setTag("_user/test");
		privateUser.setOrigin("");
		privateUser.setName("Private User");
		privateUser.setRole(MOD);
		userRepository.save(privateUser);

		var protectedUser = new User();
		protectedUser.setTag("+user/test");
		protectedUser.setOrigin("");
		protectedUser.setRole(EDITOR);
		userRepository.save(protectedUser);

		// Query with private prefix
		var result = configCache.getUser("_user/test");

		assertThat(result).isNotNull();
		// Should take highest role (MOD > EDITOR)
		assertThat(result.getRole()).isEqualTo(MOD);
	}

	@Test
	void testGetUser_SingleProtectedUser() {
		var user = new User();
		user.setTag("+user/test");
		user.setOrigin("");
		user.setName("Test User");
		user.setRole(USER);
		userRepository.save(user);

		var result = configCache.getUser("+user/test");

		assertThat(result).isNotNull();
		assertThat(result.getTag()).isEqualTo("+user/test");
		assertThat(result.getName()).isEqualTo("Test User");
		assertThat(result.getRole()).isEqualTo(USER);
	}

	@Test
	void testGetUser_SinglePrivateUser() {
		var user = new User();
		user.setTag("_user/test");
		user.setOrigin("");
		user.setName("Private User");
		user.setRole(VIEWER);
		userRepository.save(user);

		var result = configCache.getUser("_user/test");

		assertThat(result).isNotNull();
		assertThat(result.getTag()).isEqualTo("_user/test");
		assertThat(result.getName()).isEqualTo("Private User");
		assertThat(result.getRole()).isEqualTo(VIEWER);
	}

	@Test
	void testGetUser_NoUserFound() {
		var result = configCache.getUser("+user/nonexistent");

		assertThat(result).isNull();
	}

	@Test
	void testGetUser_EmptyQualifiedTag() {
		var result = configCache.getUser("");

		assertThat(result).isNull();
	}

	@Test
	void testGetUser_NullQualifiedTag() {
		var result = configCache.getUser(null);

		assertThat(result).isNull();
	}

	@Test
	void testGetUserByExternalId_MergesMultipleUsers() {
		// Create first user with external ID
		var user1 = new User();
		user1.setTag("+user/alice");
		user1.setOrigin("");
		user1.setName("Alice");
		user1.setRole(USER);
		user1.setExternal(External.builder().ids(List.of("alice@example.com")).build());
		user1.setReadAccess(List.of("tag1"));
		userRepository.save(user1);

		// Create second user with same external ID
		var user2 = new User();
		user2.setTag("_user/aliceprivate");
		user2.setOrigin("");
		user2.setRole(ADMIN);
		user2.setExternal(External.builder().ids(List.of("alice@example.com")).build());
		user2.setReadAccess(List.of("tag2"));
		userRepository.save(user2);

		var result = configCache.getUserByExternalId("", "alice@example.com");

		assertThat(result).isPresent();
		// Should preserve first user's basic properties
		assertThat(result.get().getTag()).isEqualTo("+user/alice");
		assertThat(result.get().getName()).isEqualTo("Alice");
		// Should take highest role
		assertThat(result.get().getRole()).isEqualTo(ADMIN);
		// Should merge access lists
		assertThat(result.get().getReadAccess()).containsExactlyInAnyOrder("tag1", "tag2");
	}

	@Test
	void testGetUserByExternalId_SingleUser() {
		var user = new User();
		user.setTag("+user/test");
		user.setOrigin("");
		user.setName("Test User");
		user.setExternal(External.builder().ids(List.of("test@example.com")).build());
		userRepository.save(user);

		var result = configCache.getUserByExternalId("", "test@example.com");

		assertThat(result).isPresent();
		assertThat(result.get().getTag()).isEqualTo("+user/test");
		assertThat(result.get().getName()).isEqualTo("Test User");
	}

	@Test
	void testGetUserByExternalId_NoUserFound() {
		var result = configCache.getUserByExternalId("", "nonexistent@example.com");

		assertThat(result).isEmpty();
	}

	@Test
	void testGetUserByExternalId_FiltersByOrigin() {
		var user1 = new User();
		user1.setTag("+user/test");
		user1.setOrigin("");
		user1.setExternal(External.builder().ids(List.of("test@example.com")).build());
		userRepository.save(user1);

		var user2 = new User();
		user2.setTag("+user/other");
		user2.setOrigin("@other");
		user2.setExternal(External.builder().ids(List.of("test@example.com")).build());
		userRepository.save(user2);

		var result1 = configCache.getUserByExternalId("", "test@example.com");
		var result2 = configCache.getUserByExternalId("@other", "test@example.com");

		assertThat(result1).isPresent();
		assertThat(result1.get().getTag()).isEqualTo("+user/test");
		assertThat(result2).isPresent();
		assertThat(result2.get().getTag()).isEqualTo("+user/other");
	}

	@Test
	void testGetUser_MergesUsersWithAllAccessTypes() {
		var user1 = new User();
		user1.setTag("+user/test");
		user1.setOrigin("");
		user1.setReadAccess(List.of("read1"));
		user1.setWriteAccess(List.of("write1"));
		user1.setTagReadAccess(List.of("tagread1"));
		user1.setTagWriteAccess(List.of("tagwrite1"));
		userRepository.save(user1);

		var user2 = new User();
		user2.setTag("_user/test");
		user2.setOrigin("");
		user2.setReadAccess(List.of("read2"));
		user2.setWriteAccess(List.of("write2"));
		user2.setTagReadAccess(List.of("tagread2"));
		user2.setTagWriteAccess(List.of("tagwrite2"));
		userRepository.save(user2);

		var result = configCache.getUser("+user/test");

		assertThat(result).isNotNull();
		assertThat(result.getReadAccess()).containsExactlyInAnyOrder("read1", "read2");
		assertThat(result.getWriteAccess()).containsExactlyInAnyOrder("_user/test", "write1", "write2");
		assertThat(result.getTagReadAccess()).containsExactlyInAnyOrder("tagread1", "tagread2");
		assertThat(result.getTagWriteAccess()).containsExactlyInAnyOrder("tagwrite1", "tagwrite2");
	}

	@Test
	void testGetUser_DifferentOriginsSeparateMerge() {
		// Users with same tag but different origins should not merge
		var user1 = new User();
		user1.setTag("+user/test");
		user1.setOrigin("");
		user1.setName("Origin 1");
		user1.setRole(USER);
		userRepository.save(user1);

		var user2 = new User();
		user2.setTag("+user/test");
		user2.setOrigin("@other");
		user2.setName("Origin 2");
		user2.setRole(ADMIN);
		userRepository.save(user2);

		// Users with different origins should be retrievable independently
		var result1 = configCache.getUser("+user/test");
		var result2 = configCache.getUser("+user/test@other");

		// Verifies that users with the same tag but different origins are not merged together
		assertThat(result1).isNotNull();
		assertThat(result1.getTag()).isEqualTo("+user/test");
		assertThat(result1.getName()).isEqualTo("Origin 1");
		assertThat(result1.getRole()).isEqualTo(USER);

		assertThat(result2).isNotNull();
		assertThat(result2.getTag()).isEqualTo("+user/test");
		assertThat(result2.getName()).isEqualTo("Origin 2");
		assertThat(result2.getRole()).isEqualTo(ADMIN);
	}

	@Test
	void testGetUserByExternalId_MergesThreeUsers() {
		// Test merging more than 2 users
		var user1 = new User();
		user1.setTag("+user/alice");
		user1.setOrigin("");
		user1.setRole(VIEWER);
		user1.setExternal(External.builder().ids(List.of("shared@example.com")).build());
		userRepository.save(user1);

		var user2 = new User();
		user2.setTag("_user/aliceprivate");
		user2.setOrigin("");
		user2.setRole(USER);
		user2.setExternal(External.builder().ids(List.of("shared@example.com")).build());
		userRepository.save(user2);

		var user3 = new User();
		user3.setTag("+user/alicealt");
		user3.setOrigin("");
		user3.setRole(MOD);
		user3.setExternal(External.builder().ids(List.of("shared@example.com")).build());
		userRepository.save(user3);

		var result = configCache.getUserByExternalId("", "shared@example.com");

		assertThat(result).isPresent();
		// Should take highest role (MOD > USER > VIEWER)
		assertThat(result.get().getRole()).isEqualTo(MOD);
	}
}
