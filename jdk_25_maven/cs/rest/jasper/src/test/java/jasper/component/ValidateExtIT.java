package jasper.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jasper.IntegrationTest;
import jasper.domain.Ext;
import jasper.domain.Template;
import jasper.domain.User;
import jasper.errors.InvalidTemplateException;
import jasper.repository.TemplateRepository;
import jasper.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@WithMockUser("+user/tester")
@IntegrationTest
@Transactional
public class ValidateExtIT {

	@Autowired
    Validate validate;

	@Autowired
	TemplateRepository templateRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ConfigCache configCache;

	@BeforeEach
	void init() {
		templateRepository.deleteAll();
		userRepository.deleteAll();
		configCache.clearUserCache();
		configCache.clearPluginCache();
		configCache.clearTemplateCache();
	}

	@Test
	void testValidateExt() {
		var ext = new Ext();
		ext.setTag("+user/tester");
		ext.setName("First");

		validate.ext("", ext);
	}

	@Test
	void testValidateTagWithInvalidTemplate() throws IOException {
		var template = new Template();
		template.setTag("user");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var ext = new Ext();
		ext.setTag("+user/tester");
		ext.setName("First");

		assertThatThrownBy(() -> validate.ext("", ext))
			.isInstanceOf(InvalidTemplateException.class);
	}

	@Test
	void testValidateTagWithTemplate() throws IOException {
		var template = new Template();
		template.setTag("user");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var ext = new Ext();
		ext.setTag("+user/tester");
		ext.setName("First");
		ext.setConfig((ObjectNode) mapper.readTree("""
		{
			"name": "Alice",
			"age": 100
		}"""));

		validate.ext("", ext);
	}

	@Test
	void testValidateTagWithTemplateAndNullConfig() throws IOException {
		var template = new Template();
		template.setTag("user");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"optionalProperties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var ext = new Ext();
		ext.setTag("+user/tester");
		ext.setName("First");

		validate.ext("", ext);
	}

	@Test
	void testValidatePrivateExt() {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_slug/custom"));
		user.setWriteAccess(List.of("_slug/custom"));
		userRepository.save(user);
		var ext = new Ext();
		ext.setTag("_slug/custom");
		ext.setName("First");

		validate.ext("", ext);
	}

	@Test
	void testValidatePrivateTagWithInvalidTemplate() throws IOException {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_slug/custom"));
		user.setWriteAccess(List.of("_slug/custom"));
		userRepository.save(user);
		var template = new Template();
		template.setTag("slug");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var ext = new Ext();
		ext.setTag("_slug/custom");
		ext.setName("First");

		assertThatThrownBy(() -> validate.ext("", ext))
			.isInstanceOf(InvalidTemplateException.class);
	}

	@Test
	void testValidateTagWithMergedTemplate() throws IOException {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("+slug/more/custom"));
		user.setWriteAccess(List.of("+slug/more/custom"));
		userRepository.save(user);
		var mapper = new ObjectMapper();
		var template1 = new Template();
		template1.setTag("slug");
		template1.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template1);
		var template2 = new Template();
		template2.setTag("slug/more");
		template2.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"location": { "type": "string" },
				"lat": { "type": "uint32" },
				"lng": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template2);
		var ext = new Ext();
		ext.setTag("+slug/more/custom");
		ext.setName("First");
		ext.setConfig((ObjectNode) mapper.readTree("""
		{
			"name": "Alice",
			"age": 100,
			"location": "Paris",
			"lat": 123,
			"lng": 456
		}"""));

		validate.ext("", ext);
	}

	@Test
	void testValidateTagWithMergedDefaultsTemplate() throws IOException {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("+slug/more/custom"));
		user.setWriteAccess(List.of("+slug/more/custom"));
		userRepository.save(user);
		var mapper = new ObjectMapper();
		var template1 = new Template();
		template1.setTag("slug");
		template1.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		template1.setDefaults((ObjectNode) mapper.readTree("""
		{
			"name": "Alice",
			"age": 100
		}"""));
		templateRepository.save(template1);
		var template2 = new Template();
		template2.setTag("slug/more");
		template2.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"location": { "type": "string" },
				"lat": { "type": "uint32" },
				"lng": { "type": "uint32" }
			}
		}"""));
		template2.setDefaults((ObjectNode) mapper.readTree("""
		{
			"location": "Paris",
			"lat": 123,
			"lng": 456
		}"""));
		templateRepository.save(template2);
		var ext = new Ext();
		ext.setTag("+slug/more/custom");
		ext.setName("First");

		validate.ext("", ext);
	}

	@Test
	void testValidatePrivateTagWithTemplate() throws IOException {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_slug/custom"));
		user.setWriteAccess(List.of("_slug/custom"));
		userRepository.save(user);
		var template = new Template();
		template.setTag("slug");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var ext = new Ext();
		ext.setTag("_slug/custom");
		ext.setName("First");
		ext.setConfig((ObjectNode) mapper.readTree("""
		{
			"name": "Alice",
			"age": 100
		}"""));

		validate.ext("", ext);
	}

	@Test
	void testValidatePrivateTagWithInvalidPrivateTemplate() throws IOException {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_slug/custom"));
		user.setWriteAccess(List.of("_slug/custom"));
		userRepository.save(user);
		var template = new Template();
		template.setTag("_slug");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var ext = new Ext();
		ext.setTag("_slug/custom");
		ext.setName("First");

		assertThatThrownBy(() -> validate.ext("", ext))
			.isInstanceOf(InvalidTemplateException.class);
	}

	@Test
	void testValidatePrivateTagWithPrivateTemplate() throws IOException {
		var user = new User();
		user.setTag("+user/tester");
		user.setReadAccess(List.of("_slug/custom"));
		user.setWriteAccess(List.of("_slug/custom"));
		userRepository.save(user);
		var template = new Template();
		template.setTag("_slug");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));
		templateRepository.save(template);
		var ext = new Ext();
		ext.setTag("_slug/custom");
		ext.setName("First");
		ext.setConfig((ObjectNode) mapper.readTree("""
		{
			"name": "Alice",
			"age": 100
		}"""));

		validate.ext("", ext);
	}
}
