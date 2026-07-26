package jasper.repository;

import jasper.IntegrationTest;
import jasper.component.ConfigCache;
import jasper.domain.Metadata;
import jasper.domain.Ref;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

import static jasper.config.JacksonConfiguration.om;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class RefRepositoryIT {

	@Autowired
	RefRepository refRepository;

	@Autowired
	RefRepositoryCustom refRepositoryCustom;

	@Autowired
	PluginRepository pluginRepository;

	@Autowired
	ConfigCache configCache;

	@BeforeEach
	void init() {
		refRepository.deleteAllInBatch();
		pluginRepository.deleteAllInBatch();
		configCache.clearPluginCache();
	}

	// --- findAllPluginTagsInResponses ---

	@Test
	void testFindAllPluginTagsInResponses_ReturnsPluginTags() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		parent.setTags(List.of("public"));
		refRepository.save(parent);

		var response = new Ref();
		response.setUrl("http://example.com/response");
		response.setOrigin("");
		response.setSources(List.of("http://example.com/parent"));
		response.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/comment", "+plugin/vote/up", "public"))
			.build());
		refRepository.save(response);

		var result = refRepositoryCustom.findAllPluginTagsInResponses("http://example.com/parent", "");

		assertThat(result).containsExactlyInAnyOrder("plugin/comment", "+plugin/vote/up");
		assertThat(result).doesNotContain("public");
	}

	@Test
	void testFindAllPluginTagsInResponses_ExcludesSelf() {
		var self = new Ref();
		self.setUrl("http://example.com/self");
		self.setOrigin("");
		self.setSources(List.of("http://example.com/self"));
		self.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/comment"))
			.build());
		refRepository.save(self);

		var result = refRepositoryCustom.findAllPluginTagsInResponses("http://example.com/self", "");

		assertThat(result).isEmpty();
	}

	@Test
	void testFindAllPluginTagsInResponses_NoResponses() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var result = refRepositoryCustom.findAllPluginTagsInResponses("http://example.com/parent", "");

		assertThat(result).isEmpty();
	}

	@Test
	void testFindAllPluginTagsInResponses_FiltersByOrigin() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var resp1 = new Ref();
		resp1.setUrl("http://example.com/resp1");
		resp1.setOrigin("@test");
		resp1.setSources(List.of("http://example.com/parent"));
		resp1.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/comment"))
			.build());
		refRepository.save(resp1);

		var resp2 = new Ref();
		resp2.setUrl("http://example.com/resp2");
		resp2.setOrigin("@other");
		resp2.setSources(List.of("http://example.com/parent"));
		resp2.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/vote"))
			.build());
		refRepository.save(resp2);

		var result = refRepositoryCustom.findAllPluginTagsInResponses("http://example.com/parent", "@test");

		assertThat(result).containsExactly("plugin/comment");
	}

	@Test
	void testFindAllPluginTagsInResponses_EmptyPluginTable() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var response = new Ref();
		response.setUrl("http://example.com/response");
		response.setOrigin("");
		response.setSources(List.of("http://example.com/parent"));
		response.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/comment", "public"))
			.build());
		refRepository.save(response);

		// Even with no plugins in the database, plugin tags in responses should still be found
		var result = refRepositoryCustom.findAllPluginTagsInResponses("http://example.com/parent", "");

		assertThat(result).containsExactly("plugin/comment");
		assertThat(result).doesNotContain("public");
	}

	// --- countPluginTagsInResponses ---

	@Test
	void testCountPluginTagsInResponses_ReturnsPluginTagsWithCounts() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var resp1 = new Ref();
		resp1.setUrl("http://example.com/resp1");
		resp1.setOrigin("");
		resp1.setSources(List.of("http://example.com/parent"));
		resp1.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/comment", "+plugin/vote/up", "public"))
			.build());
		refRepository.save(resp1);

		var resp2 = new Ref();
		resp2.setUrl("http://example.com/resp2");
		resp2.setOrigin("");
		resp2.setSources(List.of("http://example.com/parent"));
		resp2.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/comment", "public"))
			.build());
		refRepository.save(resp2);

		var result = refRepositoryCustom.countPluginTagsInResponses("http://example.com/parent", "");

		var map = result.stream().collect(java.util.stream.Collectors.toMap(r -> (String) r[0], r -> ((Number) r[1]).longValue()));
		assertThat(map).containsEntry("plugin/comment", 2L);
		assertThat(map).containsEntry("+plugin/vote/up", 1L);
		assertThat(map).doesNotContainKey("public");
	}

	@Test
	void testCountPluginTagsInResponses_EmptyPluginTable() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var response = new Ref();
		response.setUrl("http://example.com/response");
		response.setOrigin("");
		response.setSources(List.of("http://example.com/parent"));
		response.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/comment", "public"))
			.build());
		refRepository.save(response);

		// Even with no plugins in the database, plugin tags in responses should still be counted
		var result = refRepositoryCustom.countPluginTagsInResponses("http://example.com/parent", "");

		var map = result.stream().collect(java.util.stream.Collectors.toMap(r -> (String) r[0], r -> ((Number) r[1]).longValue()));
		assertThat(map).containsEntry("plugin/comment", 1L);
		assertThat(map).doesNotContainKey("public");
	}

	// --- findAllUserPluginTagsInResponses ---

	@Test
	void testFindAllUserPluginTagsInResponses_ReturnsUserPluginTags() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var response = new Ref();
		response.setUrl("http://example.com/response");
		response.setOrigin("");
		response.setSources(List.of("http://example.com/parent"));
		response.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/user/tester", "+plugin/user/admin", "plugin/comment"))
			.build());
		refRepository.save(response);

		var result = refRepositoryCustom.findAllUserPluginTagsInResponses("http://example.com/parent", "");

		assertThat(result).containsExactlyInAnyOrder("plugin/user/tester", "+plugin/user/admin");
	}

	@Test
	void testFindAllUserPluginTagsInResponses_FiltersExactOrigin() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var resp1 = new Ref();
		resp1.setUrl("http://example.com/resp1");
		resp1.setOrigin("@test");
		resp1.setSources(List.of("http://example.com/parent"));
		resp1.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/user/local"))
			.build());
		refRepository.save(resp1);

		var resp2 = new Ref();
		resp2.setUrl("http://example.com/resp2");
		resp2.setOrigin("@other");
		resp2.setSources(List.of("http://example.com/parent"));
		resp2.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/user/remote"))
			.build());
		refRepository.save(resp2);

		var result = refRepositoryCustom.findAllUserPluginTagsInResponses("http://example.com/parent", "@test");

		assertThat(result).containsExactly("plugin/user/local");
	}

	@Test
	void testFindAllUserPluginTagsInResponses_UnderscorePrefixMatchesLiteralOnly() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var response = new Ref();
		response.setUrl("http://example.com/response");
		response.setOrigin("");
		response.setSources(List.of("http://example.com/parent"));
		response.setMetadata(Metadata.builder()
			.expandedTags(List.of("_plugin/user/test", "+plugin/comment"))
			.build());
		refRepository.save(response);

		var result = refRepositoryCustom.findAllUserPluginTagsInResponses("http://example.com/parent", "");

		assertThat(result).containsExactly("_plugin/user/test");
		assertThat(result).doesNotContain("+plugin/comment");
	}

	@Test
	void testFindAllUserPluginTagsInResponses_WorksWithEmptyPluginTable() {
		// No plugins in the database at all
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var response = new Ref();
		response.setUrl("http://example.com/response");
		response.setOrigin("");
		response.setSources(List.of("http://example.com/parent"));
		response.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/user/tester", "+plugin/user/admin", "plugin/comment", "public"))
			.build());
		refRepository.save(response);

		var result = refRepositoryCustom.findAllUserPluginTagsInResponses("http://example.com/parent", "");

		assertThat(result).containsExactlyInAnyOrder("plugin/user/tester", "+plugin/user/admin");
	}

	@Test
	void testFindAllUserPluginTagsInResponses_NoMatchingTags() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var response = new Ref();
		response.setUrl("http://example.com/response");
		response.setOrigin("");
		response.setSources(List.of("http://example.com/parent"));
		response.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/comment", "public"))
			.build());
		refRepository.save(response);

		var result = refRepositoryCustom.findAllUserPluginTagsInResponses("http://example.com/parent", "");

		assertThat(result).isEmpty();
	}

	@Test
	void testFindAllUserPluginTagsInResponses_DeduplicatesAcrossResponses() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var resp1 = new Ref();
		resp1.setUrl("http://example.com/resp1");
		resp1.setOrigin("");
		resp1.setSources(List.of("http://example.com/parent"));
		resp1.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/user/tester", "public"))
			.build());
		refRepository.save(resp1);

		var resp2 = new Ref();
		resp2.setUrl("http://example.com/resp2");
		resp2.setOrigin("");
		resp2.setSources(List.of("http://example.com/parent"));
		resp2.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/user/tester", "+plugin/user/admin"))
			.build());
		refRepository.save(resp2);

		var result = refRepositoryCustom.findAllUserPluginTagsInResponses("http://example.com/parent", "");

		assertThat(result).containsExactlyInAnyOrder("plugin/user/tester", "+plugin/user/admin");
	}

	@Test
	void testFindAllUserPluginTagsInResponses_ExcludesSelf() {
		var self = new Ref();
		self.setUrl("http://example.com/self");
		self.setOrigin("");
		self.setSources(List.of("http://example.com/self"));
		self.setMetadata(Metadata.builder()
			.expandedTags(List.of("plugin/user/tester"))
			.build());
		refRepository.save(self);

		var result = refRepositoryCustom.findAllUserPluginTagsInResponses("http://example.com/self", "");

		assertThat(result).isEmpty();
	}

	@Test
	void testFindAllUserPluginTagsInResponses_FallsBackToTagsWhenNoExpandedTags() {
		var parent = new Ref();
		parent.setUrl("http://example.com/parent");
		parent.setOrigin("");
		refRepository.save(parent);

		var response = new Ref();
		response.setUrl("http://example.com/response");
		response.setOrigin("");
		response.setSources(List.of("http://example.com/parent"));
		response.setTags(List.of("plugin/user/tester", "public"));
		// No metadata / expandedTags set
		refRepository.save(response);

		var result = refRepositoryCustom.findAllUserPluginTagsInResponses("http://example.com/parent", "");

		assertThat(result).containsExactly("plugin/user/tester");
	}

	// --- originUrl ---

	@Test
	void testOriginUrl_FindsOriginRef() {
		var originRef = new Ref();
		originRef.setUrl("http://example.com/origin");
		originRef.setOrigin("@test");
		originRef.setTags(List.of("+plugin/origin"));
		originRef.setMetadata(Metadata.builder()
			.expandedTags(List.of("+plugin/origin"))
			.build());
		originRef.setPlugins(om().createObjectNode()
			.set("+plugin/origin", om().createObjectNode()));
		refRepository.save(originRef);

		var result = refRepository.originUrl("@test", "");

		assertThat(result).isPresent();
		assertThat(result.get().getUrl()).isEqualTo("http://example.com/origin");
	}

	@Test
	void testOriginUrl_WithProxy() {
		var originRef = new Ref();
		originRef.setUrl("http://example.com/origin");
		originRef.setOrigin("@remote");
		originRef.setTags(List.of("+plugin/origin"));
		originRef.setMetadata(Metadata.builder()
			.expandedTags(List.of("+plugin/origin"))
			.build());
		originRef.setPlugins(om().createObjectNode()
			.set("+plugin/origin", om().createObjectNode()
				.put("proxy", "http://proxy.example.com")));
		refRepository.save(originRef);

		var result = refRepository.originUrl("@remote", "");

		assertThat(result).isPresent();
		assertThat(result.get().getProxy()).isEqualTo("http://proxy.example.com");
		assertThat(result.get().get()).isEqualTo("http://proxy.example.com");
	}

	@Test
	void testOriginUrl_NotFound() {
		var result = refRepository.originUrl("@nonexistent", "");

		assertThat(result).isEmpty();
	}

	@Test
	void testOriginUrl_FiltersByRemote() {
		var originRef = new Ref();
		originRef.setUrl("http://example.com/origin");
		originRef.setOrigin("@test");
		originRef.setTags(List.of("+plugin/origin"));
		originRef.setMetadata(Metadata.builder()
			.expandedTags(List.of("+plugin/origin"))
			.build());
		originRef.setPlugins(om().createObjectNode()
			.set("+plugin/origin", om().createObjectNode()
				.put("local", "@remote")));
		refRepository.save(originRef);

		var result1 = refRepository.originUrl("@test", "");
		assertThat(result1).isEmpty();

		var result2 = refRepository.originUrl("@test", "@remote");
		assertThat(result2).isPresent();
		assertThat(result2.get().getUrl()).isEqualTo("http://example.com/origin");
	}
}
