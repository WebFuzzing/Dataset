package jasper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.JsonPatch;
import jasper.IntegrationTest;
import jasper.component.ConfigCache;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.errors.InvalidPatchException;
import jasper.repository.PluginRepository;
import jasper.repository.RefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser("+user/tester")
@IntegrationTest
public class TaggingServiceIT {

	@Autowired
	TaggingService taggingService;

	@Autowired
	RefRepository refRepository;

	@Autowired
	PluginRepository pluginRepository;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	ConfigCache configCache;

	static final String URL = "https://www.example.com/";

	Ref refWithTags(String url, String... tags) {
		var ref = new Ref();
		ref.setUrl(url);
		ref.setTags(new ArrayList<>(List.of(tags)));
		refRepository.save(ref);
		return ref;
	}

	@BeforeEach
	void init() {
		configCache.clearUserCache();
		configCache.clearPluginCache();
		configCache.clearTemplateCache();
		refRepository.deleteAll();
		pluginRepository.deleteAll();
	}

	@Test
	void testCreateTagRef() {
		refWithTags(URL, "+user/tester");

		taggingService.create("test", URL, "");

		assertThat(refRepository.existsByUrlAndOrigin(URL, ""))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "").get();
		assertThat(fetched.getTags())
			.contains("test");
	}

	@Test
	void testCreatePrivateTagRef() {
		refWithTags(URL, "+user/tester");

		assertThatThrownBy(() -> taggingService.create("_test", URL, ""))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, ""))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "").get();
		assertThat(fetched.getTags())
			.doesNotContain("_test");
	}

	@Test
	void testDeletePrivateTagRef() {
		refWithTags(URL, "+user/tester", "_test");

		assertThatThrownBy(() -> taggingService.delete("_test", URL, ""))
			.isInstanceOf(AccessDeniedException.class);

		assertThat(refRepository.existsByUrlAndOrigin(URL, ""))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "").get();
		assertThat(fetched.getTags())
			.contains("_test");
	}

	@Test
	void testCreateTagRefUnauthorized() {
		refWithTags(URL, "public");

		assertThatThrownBy(() -> taggingService.create("test", URL, ""))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"EDITOR"})
	void testCreateTagRefEditor() {
		refWithTags(URL, "public");

		taggingService.create("test", URL, "");

		assertThat(refRepository.existsByUrlAndOrigin(URL, ""))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(URL, "").get();
		assertThat(fetched.getTags())
			.contains("test");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testRespondBasic() {
		refWithTags(URL, "+user/tester");

		taggingService.respond(List.of("test"), URL, null);

		var responseUrl = "tag:/user/tester?url=" + URL;
		assertThat(refRepository.existsByUrlAndOrigin(responseUrl, ""))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		assertThat(fetched.getTags())
			.contains("test", "internal", "+user/tester");
		assertThat(fetched.getSources())
			.contains(URL);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testRespondMultipleTags() {
		refWithTags(URL, "+user/tester");

		taggingService.respond(List.of("tag1", "tag2", "tag3"), URL, null);

		var responseUrl = "tag:/user/tester?url=" + URL;
		assertThat(refRepository.existsByUrlAndOrigin(responseUrl, ""))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		assertThat(fetched.getTags())
			.contains("tag1", "tag2", "tag3", "internal", "+user/tester");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testRespondWithPluginDefaults() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create a plugin with defaults and schema
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		plugin.setOrigin("");
		var defaults = (ObjectNode) objectMapper.readTree("{\"color\": \"blue\", \"size\": 10}");
		plugin.setDefaults(defaults);
		var schema = (ObjectNode) objectMapper.readTree("""
		{
			"properties": {
				"color": { "type": "string" },
				"size": { "type": "uint32" }
			}
		}""");
		plugin.setSchema(schema);
		pluginRepository.save(plugin);

		taggingService.respond(List.of("plugin/test"), URL, null);

		var responseUrl = "tag:/user/tester?url=" + URL;
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		assertThat(fetched.getTags())
			.contains("plugin/test");
		assertThat(fetched.getPlugins())
			.isNotNull();
		assertThat(fetched.getPlugins().has("plugin/test"))
			.isTrue();
		var pluginData = fetched.getPlugins().get("plugin/test");
		assertThat(pluginData.get("color").asText())
			.isEqualTo("blue");
		assertThat(pluginData.get("size").asInt())
			.isEqualTo(10);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testRespondWithJsonPatch() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create a plugin with defaults and schema
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		plugin.setOrigin("");
		var defaults = (ObjectNode) objectMapper.readTree("{\"color\": \"blue\", \"size\": 10}");
		plugin.setDefaults(defaults);
		var schema = (ObjectNode) objectMapper.readTree("""
		{
			"properties": {
				"color": { "type": "string" },
				"size": { "type": "uint32" }
			}
		}""");
		plugin.setSchema(schema);
		pluginRepository.save(plugin);

		// Create a JSON patch to modify the plugin data
		var patchJson = "[{\"op\": \"replace\", \"path\": \"/plugin~1test/color\", \"value\": \"red\"}]";
		var patch = objectMapper.readValue(patchJson, JsonPatch.class);

		taggingService.respond(List.of("plugin/test"), URL, patch);

		var responseUrl = "tag:/user/tester?url=" + URL;
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		assertThat(fetched.getPlugins())
			.isNotNull();
		assertThat(fetched.getPlugins().has("plugin/test"))
			.isTrue();
		var pluginData = fetched.getPlugins().get("plugin/test");
		assertThat(pluginData.get("color").asText())
			.isEqualTo("red");
		assertThat(pluginData.get("size").asInt())
			.isEqualTo(10);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testRespondWithJsonPatchAdd() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create a plugin with defaults and schema
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		plugin.setOrigin("");
		var defaults = (ObjectNode) objectMapper.readTree("{\"color\": \"blue\"}");
		plugin.setDefaults(defaults);
		var schema = (ObjectNode) objectMapper.readTree("""
		{
			"properties": {
				"color": { "type": "string" },
				"newField": { "type": "string" }
			}
		}""");
		plugin.setSchema(schema);
		pluginRepository.save(plugin);

		// Create a JSON patch to add a new field
		var patchJson = "[{\"op\": \"add\", \"path\": \"/plugin~1test/newField\", \"value\": \"newValue\"}]";
		var patch = objectMapper.readValue(patchJson, JsonPatch.class);

		taggingService.respond(List.of("plugin/test"), URL, patch);

		var responseUrl = "tag:/user/tester?url=" + URL;
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		var pluginData = fetched.getPlugins().get("plugin/test");
		assertThat(pluginData.get("color").asText())
			.isEqualTo("blue");
		assertThat(pluginData.get("newField").asText())
			.isEqualTo("newValue");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testRespondWithInvalidJsonPatch() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create an invalid JSON patch that references a non-existent path
		var patchJson = "[{\"op\": \"replace\", \"path\": \"/nonexistent/field\", \"value\": \"test\"}]";
		var patch = objectMapper.readValue(patchJson, JsonPatch.class);

		assertThatThrownBy(() -> taggingService.respond(List.of("plugin/test"), URL, patch))
			.isInstanceOf(InvalidPatchException.class);
	}

	@Test
	@WithMockUser(value = "+user/anonymous", roles = {"USER"})
	void testRespondUnauthorized() {
		refWithTags(URL, "+user/tester");

		// User without permission to patch tags should fail
		assertThatThrownBy(() -> taggingService.respond(List.of("_private"), URL, null))
			.isInstanceOf(AccessDeniedException.class);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testRespondWithoutPlugin() {
		refWithTags(URL, "+user/tester");

		// Respond with a tag that has no plugin configured
		taggingService.respond(List.of("plugin/nonexistent"), URL, null);

		var responseUrl = "tag:/user/tester?url=" + URL;
		assertThat(refRepository.existsByUrlAndOrigin(responseUrl, ""))
			.isTrue();
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		assertThat(fetched.getTags())
			.contains("plugin/nonexistent");
		// Plugin data should not be set if plugin has no defaults
		if (fetched.getPlugins() != null) {
			assertThat(fetched.getPlugins().has("plugin/nonexistent"))
				.isFalse();
		}
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testRespondMultiplePluginsWithDefaults() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create multiple plugins with defaults and schemas
		var plugin1 = new Plugin();
		plugin1.setTag("plugin/test1");
		plugin1.setOrigin("");
		plugin1.setDefaults((ObjectNode) objectMapper.readTree("{\"value1\": \"a\"}"));
		plugin1.setSchema((ObjectNode) objectMapper.readTree("""
		{
			"properties": {
				"value1": { "type": "string" }
			}
		}"""));
		pluginRepository.save(plugin1);

		var plugin2 = new Plugin();
		plugin2.setTag("plugin/test2");
		plugin2.setOrigin("");
		plugin2.setDefaults((ObjectNode) objectMapper.readTree("{\"value2\": \"b\"}"));
		plugin2.setSchema((ObjectNode) objectMapper.readTree("""
		{
			"properties": {
				"value2": { "type": "string" }
			}
		}"""));
		pluginRepository.save(plugin2);

		taggingService.respond(List.of("plugin/test1", "plugin/test2"), URL, null);

		var responseUrl = "tag:/user/tester?url=" + URL;
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		assertThat(fetched.getPlugins().has("plugin/test1"))
			.isTrue();
		assertThat(fetched.getPlugins().has("plugin/test2"))
			.isTrue();
		assertThat(fetched.getPlugins().get("plugin/test1").get("value1").asText())
			.isEqualTo("a");
		assertThat(fetched.getPlugins().get("plugin/test2").get("value2").asText())
			.isEqualTo("b");
	}

}
