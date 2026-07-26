package jasper.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jasper.IntegrationTest;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.errors.InvalidPluginException;
import jasper.repository.PluginRepository;
import jasper.repository.RefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@IntegrationTest
@Transactional
public class ValidateRefIT {

	@Autowired
	Validate validate;

	@Autowired
	RefRepository refRepository;

	@Autowired
	PluginRepository pluginRepository;

	static final String URL = "https://www.example.com/";

	@BeforeEach
	void init() {
		refRepository.deleteAll();
		pluginRepository.deleteAll();
	}

	@Test
	void testValidateRef() {
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(Arrays.asList("+user/tester"));
		refRepository.save(ref);

		validate.ref("", ref, false);
	}

	@Test
	void testValidateRefWithInvalidPlugin() throws IOException {
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		var mapper = new ObjectMapper();
		plugin.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		pluginRepository.save(plugin);
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(Arrays.asList("+user/tester", "plugin/test"));

		assertThatThrownBy(() -> validate.ref("", ref, false))
			.isInstanceOf(InvalidPluginException.class);
	}

	@Test
	void testValidateRefWithInvalidPluginDefaults() throws IOException {
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		var mapper = new ObjectMapper();
		plugin.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		plugin.setDefaults((ObjectNode) mapper.readTree("""
		{
			"invalid": "defaults"
		}"""));
		pluginRepository.save(plugin);
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(Arrays.asList("+user/tester", "plugin/test"));

		assertThatThrownBy(() -> validate.ref("", ref, false))
			.isInstanceOf(InvalidPluginException.class);
	}

	@Test
	void testValidateRefWithPlugin() throws IOException {
		var mapper = new ObjectMapper();
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		plugin.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		pluginRepository.save(plugin);
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(Arrays.asList("+user/tester", "plugin/test")));
		ref.setPlugins((ObjectNode) mapper.readTree("""
		{
			"plugin/test": {
				"name": "Alice",
				"age": 100
			}
		}"""));

		validate.ref("", ref, false);
	}

	@Test
	void testValidateRefWithOptionalPlugin() throws IOException {
		var mapper = new ObjectMapper();
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		plugin.setSchema((ObjectNode) mapper.readTree("""
		{
			"optionalProperties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		pluginRepository.save(plugin);
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(new ArrayList<>(Arrays.asList("+user/tester", "plugin/test")));

		validate.ref("", ref, false);
	}

	@Test
	void testValidateRefWithStringPlugin() throws IOException {
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		var mapper = new ObjectMapper();
		plugin.setSchema((ObjectNode) mapper.readTree("""
			{ "type": "string" },
		"""));
		pluginRepository.save(plugin);
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(Arrays.asList("+user/tester", "plugin/test"));
		ref.setPlugins((ObjectNode) mapper.readTree("""
		{
			"plugin/test": "test"
		}"""));

		validate.ref("", ref, false);
	}

	@Test
	void testValidateRefWithBooleanPlugin() throws IOException {
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		var mapper = new ObjectMapper();
		plugin.setSchema((ObjectNode) mapper.readTree("""
			{ "type": "boolean" },
		"""));
		pluginRepository.save(plugin);
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(Arrays.asList("+user/tester", "plugin/test"));
		ref.setPlugins((ObjectNode) mapper.readTree("""
		{
			"plugin/test": true
		}"""));

		validate.ref("", ref, false);
	}

	@Test
	void testValidateRefWithNumberPlugin() throws IOException {
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		var mapper = new ObjectMapper();
		plugin.setSchema((ObjectNode) mapper.readTree("""
			{ "type": "uint32" },
		"""));
		pluginRepository.save(plugin);
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(Arrays.asList("+user/tester", "plugin/test"));
		ref.setPlugins((ObjectNode) mapper.readTree("""
		{
			"plugin/test": 100
		}"""));

		validate.ref("", ref, false);
	}

	@Test
	void testValidateRefWithArrayPlugin() throws IOException {
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		var mapper = new ObjectMapper();
		plugin.setSchema((ObjectNode) mapper.readTree("""
			{ "elements": { "type": "string" } },
		"""));
		pluginRepository.save(plugin);
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(Arrays.asList("+user/tester", "plugin/test"));
		ref.setPlugins((ObjectNode) mapper.readTree("""
		{
			"plugin/test": ["test", "works"]
		}"""));

		validate.ref("", ref, false);
	}

	@Test
	void testValidateRefWithPluginExtraFailed() throws IOException {
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		var mapper = new ObjectMapper();
		plugin.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		pluginRepository.save(plugin);
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(Arrays.asList("+user/tester", "plugin/test"));
		ref.setPlugins((ObjectNode) mapper.readTree("""
		{
			"plugin/test": {
				"name": "Alice",
				"age": 100
			},
			"extraStuff": {
				"is": "not allowed"
			}
		}"""));

		assertThatThrownBy(() -> validate.ref("", ref, false))
			.isInstanceOf(InvalidPluginException.class);
	}

	@Test
	void testValidateRefWithSchemalessPluginFailed() throws IOException {
		var mapper = new ObjectMapper();
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		pluginRepository.save(plugin);
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(Arrays.asList("+user/tester", "plugin/test"));
		ref.setPlugins((ObjectNode) mapper.readTree("""
		{
			"plugin/test": {
				"name": "Alice",
				"age": 100
			}
		}"""));

		assertThatThrownBy(() -> validate.ref("", ref, false))
			.isInstanceOf(InvalidPluginException.class);
	}

	@Test
	void testValidateRefWithPluginDefaults() throws IOException {
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		var mapper = new ObjectMapper();
		plugin.setDefaults((ObjectNode) mapper.readTree("""
		{
			"name": "Alice",
			"age": 100
		}"""));
		plugin.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		pluginRepository.save(plugin);
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("First");
		ref.setTags(Arrays.asList("+user/tester", "plugin/test"));

		validate.ref("", ref, false);
	}

	@Test
	void testResponseValidationWithNoRoles() {
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("Response Ref");
		ref.setTags(new ArrayList<>(Arrays.asList("+user/tester", "seal", "+seal", "_seal", "_moderated", "plugin/qc")));

		validate.response("", ref);

		// Without any roles, all mod and editor seals should be removed
		assertThat(ref.getTags())
			.contains("+user/tester")
			.doesNotContain("seal", "+seal", "_seal", "_moderated", "plugin/qc");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testResponseValidationWithUserRole() {
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("Response Ref");
		ref.setTags(new ArrayList<>(Arrays.asList("+user/tester", "seal", "+seal", "_seal", "_moderated", "plugin/qc")));

		validate.response("", ref);

		// With USER role (not MOD or EDITOR), all mod and editor seals should be removed
		assertThat(ref.getTags())
			.contains("+user/tester")
			.doesNotContain("seal", "+seal", "_seal", "_moderated", "plugin/qc");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"MOD"})
	void testResponseValidationWithModRole() {
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("Response Ref");
		ref.setTags(new ArrayList<>(Arrays.asList("+user/tester", "seal", "+seal", "_seal", "_moderated", "plugin/qc")));

		validate.response("", ref);

		// With MOD role, due to role hierarchy MOD > EDITOR, both mod and editor seals are kept
		assertThat(ref.getTags())
			.contains("+user/tester", "seal", "+seal", "_seal", "_moderated", "plugin/qc");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"EDITOR"})
	void testResponseValidationWithEditorRole() {
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("Response Ref");
		ref.setTags(new ArrayList<>(Arrays.asList("+user/tester", "seal", "+seal", "_seal", "_moderated", "plugin/qc")));

		validate.response("", ref);

		// With EDITOR role (but not MOD), editor seals are kept but mod seals are removed
		assertThat(ref.getTags())
			.contains("+user/tester", "plugin/qc")
			.doesNotContain("seal", "+seal", "_seal", "_moderated");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"MOD", "EDITOR"})
	void testResponseValidationWithBothModAndEditorRoles() {
		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("Response Ref");
		ref.setTags(new ArrayList<>(Arrays.asList("+user/tester", "seal", "+seal", "_seal", "_moderated", "plugin/qc")));

		validate.response("", ref);

		// With both MOD and EDITOR roles, all seals should be kept
		assertThat(ref.getTags())
			.contains("+user/tester", "seal", "+seal", "_seal", "_moderated", "plugin/qc");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testResponseValidationCallsPluginValidation() throws IOException {
		// Create a plugin with schema
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		var mapper = new ObjectMapper();
		plugin.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" }
			}
		}"""));
		pluginRepository.save(plugin);

		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("Response Ref");
		ref.setTags(new ArrayList<>(Arrays.asList("+user/tester", "plugin/test")));
		ref.setPlugins((ObjectNode) mapper.readTree("""
		{
			"plugin/test": {
				"name": "Alice"
			}
		}"""));

		// Should pass validation
		validate.response("", ref);

		assertThat(ref.getTags())
			.contains("+user/tester", "plugin/test");
	}

	@Test
	@WithMockUser(value = "+user/tester", roles = {"USER"})
	void testResponseValidationFailsWithInvalidPlugin() throws IOException {
		// Create a plugin with schema
		var plugin = new Plugin();
		plugin.setTag("plugin/test");
		var mapper = new ObjectMapper();
		plugin.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" }
			}
		}"""));
		pluginRepository.save(plugin);

		var ref = new Ref();
		ref.setUrl(URL);
		ref.setTitle("Response Ref");
		ref.setTags(new ArrayList<>(Arrays.asList("+user/tester", "plugin/test")));
		ref.setPlugins((ObjectNode) mapper.readTree("""
		{
			"plugin/test": {
				"name": 123
			}
		}"""));

		// Should fail validation because name should be string, not number
		assertThatThrownBy(() -> validate.response("", ref))
			.isInstanceOf(InvalidPluginException.class);
	}
}
