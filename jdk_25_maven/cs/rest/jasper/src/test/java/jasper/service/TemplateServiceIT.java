package jasper.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jasper.IntegrationTest;
import jasper.domain.Template;
import jasper.repository.TemplateRepository;
import jasper.repository.filter.TagFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithMockUser;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@WithMockUser(value = "admin", roles = "ADMIN")
@IntegrationTest
public class TemplateServiceIT {

	@Autowired
	TemplateService templateService;

	@Autowired
	TemplateRepository templateRepository;

	@BeforeEach
	void init() {
		templateRepository.deleteAll();
	}

	@Test
	void testCreateTemplateWithSchema() throws IOException {
		var template = new Template();
		template.setTag("test");
		var mapper = new ObjectMapper();
		template.setSchema((ObjectNode) mapper.readTree("""
		{
			"properties": {
				"name": { "type": "string" },
				"age": { "type": "uint32" }
			}
		}"""));

		templateService.create(template);

		assertThat(templateRepository.existsByQualifiedTag("test"))
			.isTrue();
		var fetched = templateRepository.findOneByQualifiedTag("test").get();
		assertThat(fetched.getTag())
			.isEqualTo("test");
	}

	Template template(String tag) {
		var t = new Template();
		t.setTag(tag);
		templateRepository.save(t);
		return t;
	}

	@Test
	void testGetPageRefWithQuery() {
		template("public");
		template("custom");
		template("extra");

		var page = templateService.page(
			TagFilter
				.builder()
				.query("custom")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
	}

	@Test
	void testGetEmptyPageRefWithEmptyQuery() {
		template("public");
		template("custom");
		template("extra");

		var page = templateService.page(
			TagFilter
				.builder()
				.query("!@*")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testGetPageRefWithEmptyQueryRoot() {
		template("");
		template("custom");
		template("extra");

		var page = templateService.page(
			TagFilter
				.builder()
				.query("@*")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(3);
	}

	@Test
	void testGetEmptyPageRefWithEmptyQueryRoot() {
		template("");
		template("custom");
		template("extra");

		var page = templateService.page(
			TagFilter
				.builder()
				.query("!@*")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testGetPageWithNotQueryRef() {
		template("test");

		var page = templateService.page(
			TagFilter
				.builder()
				.query("!test")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(0);
	}

	@Test
	void testGetPageWithNotQueryFoundRef() {
		template("public");

		var page = templateService.page(
			TagFilter
				.builder()
				.query("!test")
				.build(),
			PageRequest.of(0, 10));

		assertThat(page.getTotalElements())
			.isEqualTo(1);
	}
}
