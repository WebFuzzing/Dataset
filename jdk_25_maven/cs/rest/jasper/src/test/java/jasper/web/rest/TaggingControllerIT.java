package jasper.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import jasper.IntegrationTest;
import jasper.component.ConfigCache;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.repository.PluginRepository;
import jasper.repository.RefRepository;
import jasper.service.TaggingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link TaggingController}.
 * Tests the response endpoint functionality with JsonMergePatch (RFC 7396).
 */
@WithMockUser("+user/tester")
@IntegrationTest
public class TaggingControllerIT {

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

	private static final String URL = "https://www.example.com/";

	Ref refWithTags(String url, String... tags) {
		var ref = new Ref();
		ref.setUrl(url);
		ref.setOrigin("");
		ref.setTags(new ArrayList<>(List.of(tags)));
		refRepository.save(ref);
		return ref;
	}

	@BeforeEach
	void init() {
		refRepository.deleteAll();
		pluginRepository.deleteAll();
		configCache.clearUserCache();
		configCache.clearPluginCache();
		configCache.clearTemplateCache();
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testMergeResponseBasicReplace() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create a plugin with defaults
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

		// Create a JSON Merge Patch to replace the color
		// RFC 7396: {"plugin/test": {"color": "red"}} replaces color field
		var mergePatchJson = "{\"plugin/test\": {\"color\": \"red\"}}";
		var patch = objectMapper.readValue(mergePatchJson, JsonMergePatch.class);

		taggingService.respond(List.of("plugin/test"), URL, patch);

		var responseUrl = "tag:/user/tester?url=" + URL;
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		assertThat(fetched.getPlugins()).isNotNull();
		assertThat(fetched.getPlugins().has("plugin/test")).isTrue();
		var pluginData = fetched.getPlugins().get("plugin/test");
		assertThat(pluginData.get("color").asText()).isEqualTo("red");
		assertThat(pluginData.get("size").asInt()).isEqualTo(10);
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testMergeResponseAddField() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create a plugin with defaults
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

		// Create a JSON Merge Patch to add a new field
		// RFC 7396: New fields are added to the object
		var mergePatchJson = "{\"plugin/test\": {\"newField\": \"newValue\"}}";
		var patch = objectMapper.readValue(mergePatchJson, JsonMergePatch.class);

		taggingService.respond(List.of("plugin/test"), URL, patch);

		var responseUrl = "tag:/user/tester?url=" + URL;
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		var pluginData = fetched.getPlugins().get("plugin/test");
		assertThat(pluginData.get("color").asText()).isEqualTo("blue");
		assertThat(pluginData.get("newField").asText()).isEqualTo("newValue");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testMergeResponseDeleteField() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create a plugin with defaults
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		plugin.setOrigin("");
		var defaults = (ObjectNode) objectMapper.readTree("{\"color\": \"blue\", \"size\": 10}");
		plugin.setDefaults(defaults);
		var schema = (ObjectNode) objectMapper.readTree("""
		{
			"optionalProperties": {
				"color": { "type": "string" },
				"size": { "type": "uint32" }
			}
		}""");
		plugin.setSchema(schema);
		pluginRepository.save(plugin);

		// Create a JSON Merge Patch to delete the size field
		// RFC 7396: null values delete fields
		var mergePatchJson = "{\"plugin/test\": {\"size\": null}}";
		var patch = objectMapper.readValue(mergePatchJson, JsonMergePatch.class);

		taggingService.respond(List.of("plugin/test"), URL, patch);

		var responseUrl = "tag:/user/tester?url=" + URL;
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		var pluginData = fetched.getPlugins().get("plugin/test");
		assertThat(pluginData.get("color").asText()).isEqualTo("blue");
		assertThat(pluginData.has("size")).isFalse();
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testMergeResponseNestedObjects() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create a plugin with nested defaults
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		plugin.setOrigin("");
		var defaults = (ObjectNode) objectMapper.readTree("""
		{
			"config": {
				"theme": "dark",
				"language": "en"
			}
		}""");
		plugin.setDefaults(defaults);
		var schema = (ObjectNode) objectMapper.readTree("""
		{
			"properties": {
				"config": {
					"properties": {
						"theme": { "type": "string" },
						"language": { "type": "string" }
					}
				}
			}
		}""");
		plugin.setSchema(schema);
		pluginRepository.save(plugin);

		// Create a JSON Merge Patch to update nested field
		// RFC 7396: Nested objects are merged recursively
		var mergePatchJson = "{\"plugin/test\": {\"config\": {\"theme\": \"light\"}}}";
		var patch = objectMapper.readValue(mergePatchJson, JsonMergePatch.class);

		taggingService.respond(List.of("plugin/test"), URL, patch);

		var responseUrl = "tag:/user/tester?url=" + URL;
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		var pluginData = fetched.getPlugins().get("plugin/test");
		var config = pluginData.get("config");
		assertThat(config.get("theme").asText()).isEqualTo("light");
		assertThat(config.get("language").asText()).isEqualTo("en");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testMergeResponseMultiplePlugins() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create two plugins with defaults
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

		// Create a JSON Merge Patch to update both plugins
		var mergePatchJson = """
		{
			"plugin/test1": {"value1": "modified_a"},
			"plugin/test2": {"value2": "modified_b"}
		}""";
		var patch = objectMapper.readValue(mergePatchJson, JsonMergePatch.class);

		taggingService.respond(List.of("plugin/test1", "plugin/test2"), URL, patch);

		var responseUrl = "tag:/user/tester?url=" + URL;
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		assertThat(fetched.getPlugins().get("plugin/test1").get("value1").asText())
			.isEqualTo("modified_a");
		assertThat(fetched.getPlugins().get("plugin/test2").get("value2").asText())
			.isEqualTo("modified_b");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testMergeResponseReplaceEntireObject() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create a plugin with defaults
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		plugin.setOrigin("");
		var defaults = (ObjectNode) objectMapper.readTree("""
		{
			"config": {
				"theme": "dark",
				"language": "en"
			}
		}""");
		plugin.setDefaults(defaults);
		var schema = (ObjectNode) objectMapper.readTree("""
		{
			"optionalProperties": {
				"config": {
					"optionalProperties": {
						"theme": { "type": "string" },
						"size": { "type": "uint32" },
						"language": { "type": "string" }
					}
				}
			}
		}""");
		plugin.setSchema(schema);
		pluginRepository.save(plugin);

		// Create a JSON Merge Patch to update nested object and remove language field
		// RFC 7396: Objects are merged recursively; use null to delete fields
		var mergePatchJson = """
		{
			"plugin/test": {
				"config": {
					"theme": "light",
					"size": 12,
					"language": null
				}
			}
		}""";
		var patch = objectMapper.readValue(mergePatchJson, JsonMergePatch.class);

		taggingService.respond(List.of("plugin/test"), URL, patch);

		var responseUrl = "tag:/user/tester?url=" + URL;
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		var pluginData = fetched.getPlugins().get("plugin/test");
		var config = pluginData.get("config");
		assertThat(config.get("theme").asText()).isEqualTo("light");
		assertThat(config.get("size").asInt()).isEqualTo(12);
		// language field should be removed because we explicitly set it to null
		assertThat(config.has("language")).isFalse();
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testMergeResponseEmptyPatch() throws IOException {
		refWithTags(URL, "+user/tester");

		// Create a plugin with defaults
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

		// Empty merge patch should leave defaults unchanged
		var mergePatchJson = "{}";
		var patch = objectMapper.readValue(mergePatchJson, JsonMergePatch.class);

		taggingService.respond(List.of("plugin/test"), URL, patch);

		var responseUrl = "tag:/user/tester?url=" + URL;
		var fetched = refRepository.findOneByUrlAndOrigin(responseUrl, "").get();
		var pluginData = fetched.getPlugins().get("plugin/test");
		assertThat(pluginData.get("color").asText()).isEqualTo("blue");
		assertThat(pluginData.get("size").asInt()).isEqualTo(10);
	}
}
