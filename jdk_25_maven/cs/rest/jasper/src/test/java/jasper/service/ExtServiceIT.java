package jasper.service;

import jasper.IntegrationTest;
import jasper.component.ConfigCache;
import jasper.domain.Ext;
import jasper.domain.User;
import jasper.errors.NotFoundException;
import jasper.repository.ExtRepository;
import jasper.repository.UserRepository;
import jasper.repository.filter.TagFilter;
import jasper.repository.spec.ExtSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser("+user/tester")
@IntegrationTest
public class ExtServiceIT {

	@Autowired
	ExtService extService;

	@Autowired
	ExtRepository extRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ConfigCache configCache;

	@BeforeEach
	void init() {
		extRepository.deleteAll();
		userRepository.deleteAll();
		configCache.clearUserCache();
		configCache.clearPluginCache();
		configCache.clearTemplateCache();
	}

	@Test
	void testCreateExt() {
		var ext = new Ext();
		ext.setTag("custom");
		ext.setName("Custom");

		assertThatThrownBy(() -> extService.create(ext))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testCreateUserExt() {
		var ext = new Ext();
		ext.setTag("+user/tester");
		ext.setName("Custom");

		extService.create(ext);

		assertThat(extRepository.existsByQualifiedTag("+user/tester"))
			.isTrue();
		var fetched = extRepository.findOneByQualifiedTag("+user/tester").get();
		assertThat(fetched.getTag())
			.isEqualTo("+user/tester");
		assertThat(fetched.getName())
			.isEqualTo("Custom");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = "MOD")
	void testModCreateExt() {
		var ext = new Ext();
		ext.setTag("custom");
		ext.setName("Custom");

		extService.create(ext);

		assertThat(extRepository.existsByQualifiedTag("custom"))
			.isTrue();
		var fetched = extRepository.findOneByQualifiedTag("custom").get();
		assertThat(fetched.getTag())
			.isEqualTo("custom");
		assertThat(fetched.getName())
			.isEqualTo("Custom");
	}

	@Test
	void testReadNonExistentExt() {
		assertThatThrownBy(() -> extService.get("custom"))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void testReadNonExistentPrivateExt() {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);

		assertThatThrownBy(() -> extService.get("_secret"))
			.isInstanceOf(NotFoundException.class);
	}

	@Test
	void testReadPrivateExtDenied() {
		assertThatThrownBy(() -> extService.get("_secret"))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testReadPublicExt() {
		var ext = new Ext();
		ext.setTag("public");
		ext.setName("Custom");
		extRepository.save(ext);

		var fetched = extService.get("public");

		assertThat(fetched.getTag())
			.isEqualTo("public");
		assertThat(fetched.getName())
			.isEqualTo("Custom");
	}

	@Test
	void testReadPublicRemoteExt() {
		var ext = new Ext();
		ext.setOrigin("@remote");
		ext.setTag("public");
		ext.setName("Custom");
		extRepository.save(ext);

		var fetched = extService.get("public@remote");

		assertThat(fetched.getTag())
			.isEqualTo("public");
		assertThat(fetched.getOrigin())
			.isEqualTo("@remote");
		assertThat(fetched.getName())
			.isEqualTo("Custom");
	}

	@Test
	void testReadUserExt() {
		var ext = new Ext();
		ext.setTag("+user/tester");
		ext.setName("Custom");
		extRepository.save(ext);

		var fetched = extService.get("+user/tester");

		assertThat(fetched.getTag())
			.isEqualTo("+user/tester");
		assertThat(fetched.getName())
			.isEqualTo("Custom");
	}

	@Test
	void testReadExt() {
		var ext = new Ext();
		ext.setTag("custom");
		ext.setName("Custom");
		extRepository.save(ext);

		var fetched = extService.get("custom");

		assertThat(fetched.getTag())
			.isEqualTo("custom");
		assertThat(fetched.getName())
			.isEqualTo("Custom");
	}

	@Test
	void testReadPrivateExtFailed() {
		var ext = new Ext();
		ext.setTag("_secret");
		ext.setName("Secret");
		extRepository.save(ext);

		assertThatThrownBy(() -> extService.get("_secret"))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testReadPrivateExt() {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);
		var ext = new Ext();
		ext.setTag("_secret");
		ext.setName("Secret");
		extRepository.save(ext);

		var fetched = extService.get("_secret");

		assertThat(fetched.getTag())
			.isEqualTo("_secret");
		assertThat(fetched.getName())
			.isEqualTo("Secret");
	}

	@Test
	@WithMockUser("_user/tester")
	void testReadPrivateUserExt() {
		var ext = new Ext();
		ext.setTag("_user/tester");
		ext.setName("Secret");
		extRepository.save(ext);

		var fetched = extService.get("_user/tester");

		assertThat(fetched.getTag())
			.isEqualTo("_user/tester");
		assertThat(fetched.getName())
			.isEqualTo("Secret");
	}

	@Test
	void testReadPrivateUserExtFailed() {
		var ext = new Ext();
		ext.setTag("_user/other");
		ext.setName("Secret");
		extRepository.save(ext);

		assertThatThrownBy(() -> extService.get("_user/other"))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	void testPagePublicExt() {
		var ext = new Ext();
		ext.setTag("custom");
		ext.setName("Custom");
		extRepository.save(ext);

		var page = extService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
		assertThat(page.getContent().get(0).getTag())
			.isEqualTo("custom");
		assertThat(page.getContent().get(0).getName())
			.isEqualTo("Custom");
	}

	@Test
	void testPagePublicRemoteExt() {
		var ext = new Ext();
		ext.setTag("custom");
		ext.setName("Custom");
		extRepository.save(ext);
		var remote = new Ext();
		remote.setTag("extra");
		remote.setOrigin("@remote");
		remote.setName("Extra");
		extRepository.save(remote);

		var page = extService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(2);
	}


	@Test
	void testPagePrivateExtHidden() {
		var ext = new Ext();
		ext.setTag("_secret");
		ext.setName("Secret");
		extRepository.save(ext);

		var page = extService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testPagePrivateExt() {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);
		var ext = new Ext();
		ext.setTag("_secret");
		ext.setName("Secret");
		extRepository.save(ext);

		var page = extService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
		assertThat(page.getContent().get(0).getTag())
			.isEqualTo("_secret");
		assertThat(page.getContent().get(0).getName())
			.isEqualTo("Secret");
	}

	@Test
	@WithMockUser("_user/tester")
	void testPagePrivateUserExt() {
		var ext = new Ext();
		ext.setTag("_user/tester");
		ext.setName("Secret");
		extRepository.save(ext);

		var page = extService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
		assertThat(page.getContent().get(0).getTag())
			.isEqualTo("_user/tester");
		assertThat(page.getContent().get(0).getName())
			.isEqualTo("Secret");
	}

	@Test
	@WithMockUser("_user/tester")
	void testPagePrivateUserExtFailed() {
		var ext = new Ext();
		ext.setTag("_user/other");
		ext.setName("Secret");
		extRepository.save(ext);

		var page = extService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testPageUserExt() {
		var ext = new Ext();
		ext.setTag("+user/tester");
		ext.setName("Secret");
		extRepository.save(ext);

		var page = extService.page(
			TagFilter.builder().build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
		assertThat(page.getContent().get(0).getTag())
			.isEqualTo("+user/tester");
		assertThat(page.getContent().get(0).getName())
			.isEqualTo("Secret");
	}

	@Test
	void testUpdateExt() {
		var user = new User();
		user.setTag("+user/tester");
		user.setWriteAccess(List.of("+custom"));
		userRepository.save(user);
		var ext = new Ext();
		ext.setTag("+custom");
		ext.setName("First");
		extRepository.save(ext);
		var updated = new Ext();
		updated.setTag("+custom");
		updated.setName("Second");
		updated.setModified(ext.getModified());

		extService.update(updated);

		assertThat(extRepository.existsByQualifiedTag("+custom"))
			.isTrue();
		var fetched = extRepository.findOneByQualifiedTag("+custom").get();
		assertThat(fetched.getTag())
			.isEqualTo("+custom");
		assertThat(fetched.getName())
			.isEqualTo("Second");
	}

	@Test
	void testUpdateExtFailed() {
		var ext = new Ext();
		ext.setTag("custom");
		ext.setName("First");
		extRepository.save(ext);
		var updated = new Ext();
		updated.setTag("custom");
		updated.setName("Second");
		updated.setModified(ext.getModified());

		assertThatThrownBy(() -> extService.update(updated))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(extRepository.existsByQualifiedTag("custom"))
			.isTrue();
		var fetched = extRepository.findOneByQualifiedTag("custom").get();
		assertThat(fetched.getTag())
			.isEqualTo("custom");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testUpdateUserExt() {
		var ext = new Ext();
		ext.setTag("+user/tester");
		ext.setName("First");
		extRepository.save(ext);
		var updated = new Ext();
		updated.setTag("+user/tester");
		updated.setName("Second");
		updated.setModified(ext.getModified());

		extService.update(updated);

		assertThat(extRepository.existsByQualifiedTag("+user/tester"))
			.isTrue();
		var fetched = extRepository.findOneByQualifiedTag("+user/tester").get();
		assertThat(fetched.getTag())
			.isEqualTo("+user/tester");
		assertThat(fetched.getName())
			.isEqualTo("Second");
	}

	@Test
	void testUpdateUserExtFailed() {
		var ext = new Ext();
		ext.setTag("+user/other");
		ext.setName("First");
		extRepository.save(ext);
		var updated = new Ext();
		updated.setTag("+user/other");
		updated.setName("Second");
		updated.setModified(ext.getModified());

		assertThatThrownBy(() -> extService.update(updated))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(extRepository.existsByQualifiedTag("+user/other"))
			.isTrue();
		var fetched = extRepository.findOneByQualifiedTag("+user/other").get();
		assertThat(fetched.getTag())
			.isEqualTo("+user/other");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testUpdatePrivateExt() {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_secret"));
		user.setWriteAccess(List.of("_secret"));
		userRepository.save(user);
		var ext = new Ext();
		ext.setTag("_secret");
		ext.setName("First");
		extRepository.save(ext);
		var updated = new Ext();
		updated.setTag("_secret");
		updated.setName("Second");
		updated.setModified(ext.getModified());

		extService.update(updated);

		assertThat(extRepository.existsByQualifiedTag("_secret"))
			.isTrue();
		var fetched = extRepository.findOneByQualifiedTag("_secret").get();
		assertThat(fetched.getTag())
			.isEqualTo("_secret");
		assertThat(fetched.getName())
			.isEqualTo("Second");
	}

	@Test
	void testUpdatePrivateExtFailed() {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);
		var ext = new Ext();
		ext.setTag("_secret");
		ext.setName("First");
		extRepository.save(ext);
		var updated = new Ext();
		updated.setTag("_secret");
		updated.setName("Second");
		updated.setModified(ext.getModified());

		assertThatThrownBy(() -> extService.update(updated))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(extRepository.existsByQualifiedTag("_secret"))
			.isTrue();
		var fetched = extRepository.findOneByQualifiedTag("_secret").get();
		assertThat(fetched.getTag())
			.isEqualTo("_secret");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testUpdatePublicExtFailed() {
		var ext = new Ext();
		ext.setTag("public");
		ext.setName("First");
		extRepository.save(ext);
		var updated = new Ext();
		updated.setTag("public");
		updated.setName("Second");
		updated.setModified(ext.getModified());

		assertThatThrownBy(() -> extService.update(updated))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(extRepository.existsByQualifiedTag("public"))
			.isTrue();
		var fetched = extRepository.findOneByQualifiedTag("public").get();
		assertThat(fetched.getTag())
			.isEqualTo("public");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testDeleteExt() {
		var user = new User();
		user.setTag("+user/tester");
		user.setWriteAccess(List.of("+custom"));
		userRepository.save(user);
		var ext = new Ext();
		ext.setTag("+custom");
		ext.setName("First");
		extRepository.save(ext);

		extService.delete("+custom");

		assertThat(extRepository.existsByQualifiedTag("+custom"))
			.isFalse();
	}

	@Test
	void testDeleteExtFailed() {
		var ext = new Ext();
		ext.setTag("custom");
		ext.setName("First");
		extRepository.save(ext);

		assertThatThrownBy(() -> extService.delete("custom"))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(extRepository.existsByQualifiedTag("custom"))
			.isTrue();
		var fetched = extRepository.findOneByQualifiedTag("custom").get();
		assertThat(fetched.getTag())
			.isEqualTo("custom");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testDeletePrivateExt() {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_secret"));
		user.setWriteAccess(List.of("_secret"));
		userRepository.save(user);
		var ext = new Ext();
		ext.setTag("_secret");
		ext.setName("First");
		extRepository.save(ext);

		extService.delete("_secret");

		assertThat(extRepository.existsByQualifiedTag("_secret"))
			.isFalse();
	}

	@Test
	void testDeletePrivateExtFailed() {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_secret"));
		userRepository.save(user);
		var ext = new Ext();
		ext.setTag("_secret");
		ext.setName("First");
		extRepository.save(ext);

		assertThatThrownBy(() -> extService.delete("_secret"))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(extRepository.existsByQualifiedTag("_secret"))
			.isTrue();
		var fetched = extRepository.findOneByQualifiedTag("_secret").get();
		assertThat(fetched.getTag())
			.isEqualTo("_secret");
		assertThat(fetched.getName())
			.isEqualTo("First");
	}

	@Test
	void testApplySortingSpec_WithNoSort() {
		// Create test Ext entities
		var ext1 = new Ext();
		ext1.setTag("+user/test1");
		ext1.setName("Test1");
		extRepository.save(ext1);
		var ext2 = new Ext();
		ext2.setTag("+user/test2");
		ext2.setName("Test2");
		extRepository.save(ext2);

		var spec = ExtSpec.sort(
			TagFilter.builder().build().spec(),
			PageRequest.of(0, 10));

		// Execute query to verify no exceptions
		var result = extRepository.findAll(spec, PageRequest.ofSize(10));
		assertThat(result.getContent()).hasSize(2);
	}

	@Test
	void testApplySortingSpec_WithConfigSort() {
		// Create Ext entities with config->value
		var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		try {
			var ext1 = new Ext();
			ext1.setTag("+user/test1");
			ext1.setName("Test1");
			ext1.setConfig((com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree("{\"value\": \"alpha\"}"));
			extRepository.save(ext1);

			var ext2 = new Ext();
			ext2.setTag("+user/test2");
			ext2.setName("Test2");
			ext2.setConfig((com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree("{\"value\": \"beta\"}"));
			extRepository.save(ext2);

			var pageable = PageRequest.of(0, 10, Sort.by(
				Sort.Order.desc("config->value")));
			var spec = ExtSpec.sort(
				TagFilter.builder().build().spec(),
				pageable);

			// Execute query to verify sorting works
			var result = extRepository.findAll(spec, PageRequest.ofSize(10));
			assertThat(result.getContent()).hasSize(2);
			// Verify descending order (beta before alpha)
			assertThat(result.getContent().get(0).getTag()).isEqualTo("+user/test2");
			assertThat(result.getContent().get(1).getTag()).isEqualTo("+user/test1");
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

	@Test
	void testApplySortingSpec_WithNumericSort() {
		// Create Ext entities with numeric config->count
		var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
		try {
			var ext1 = new Ext();
			ext1.setTag("+user/test1");
			ext1.setName("Test1");
			ext1.setConfig((com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree("{\"count\": 2}"));
			extRepository.save(ext1);

			var ext2 = new Ext();
			ext2.setTag("+user/test2");
			ext2.setName("Test2");
			ext2.setConfig((com.fasterxml.jackson.databind.node.ObjectNode) mapper.readTree("{\"count\": 10}"));
			extRepository.save(ext2);

			var pageable = PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(
				org.springframework.data.domain.Sort.Order.asc("config->count:num")));
			var spec = ExtSpec.sort(
				TagFilter.builder().build().spec(),
				pageable);

			// Execute query to verify numeric sorting (2 before 10, not string sort where "10" < "2")
			var result = extRepository.findAll(spec, PageRequest.of(0, 10));
			assertThat(result.getContent()).hasSize(2);
			// Verify ascending numeric order (2 before 10)
			assertThat(result.getContent().get(0).getTag()).isEqualTo("+user/test1");
			assertThat(result.getContent().get(1).getTag()).isEqualTo("+user/test2");
		} catch (com.fasterxml.jackson.core.JsonProcessingException e) {
			throw new RuntimeException(e);
		}
	}

}
