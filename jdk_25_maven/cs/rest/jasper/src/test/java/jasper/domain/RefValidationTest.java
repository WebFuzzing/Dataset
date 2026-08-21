package jasper.domain;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link Ref} entity validation.
 * Tests that fully qualified tags (tags containing @) are properly rejected by validation.
 */
class RefValidationTest {

	private Validator validator;

	@BeforeEach
	void setup() {
		validator = Validation.buildDefaultValidatorFactory().getValidator();
	}

	@Test
	void testRefWithFullyQualifiedTagShouldFailValidation() {
		// Test that a Ref with a fully qualified tag (containing @) fails validation
		var ref = new Ref();
		ref.setUrl("https://www.example.com/");
		ref.setTags(List.of("test@origin")); // Fully qualified tag with @

		Set<ConstraintViolation<Ref>> violations = validator.validate(ref);

		assertThat(violations).isNotEmpty();
		assertThat(violations).anyMatch(v -> 
			v.getPropertyPath().toString().contains("tags") &&
			v.getInvalidValue().equals("test@origin")
		);
	}

	@Test
	void testRefWithTagEndingInAtSignShouldFailValidation() {
		// Test that a Ref with a tag ending in @ fails validation
		var ref = new Ref();
		ref.setUrl("https://www.example.com/");
		ref.setTags(List.of("test@")); // Tag with @ at end

		Set<ConstraintViolation<Ref>> violations = validator.validate(ref);

		assertThat(violations).isNotEmpty();
		assertThat(violations).anyMatch(v -> 
			v.getPropertyPath().toString().contains("tags") &&
			v.getInvalidValue().equals("test@")
		);
	}

	@Test
	void testRefWithFullyQualifiedTagWithFullOriginShouldFailValidation() {
		// Test that a Ref with a fully qualified tag with full origin fails validation
		var ref = new Ref();
		ref.setUrl("https://www.example.com/");
		ref.setTags(List.of("test@remote.example.com")); // Fully qualified tag with full origin

		Set<ConstraintViolation<Ref>> violations = validator.validate(ref);

		assertThat(violations).isNotEmpty();
		assertThat(violations).anyMatch(v -> 
			v.getPropertyPath().toString().contains("tags") &&
			v.getInvalidValue().equals("test@remote.example.com")
		);
	}

	@Test
	void testRefWithMultipleTagsIncludingFullyQualifiedShouldFailValidation() {
		// Test that a Ref with multiple tags where one is fully qualified fails validation
		var ref = new Ref();
		ref.setUrl("https://www.example.com/");
		ref.setTags(List.of("valid", "test@origin", "another")); // One fully qualified tag

		Set<ConstraintViolation<Ref>> violations = validator.validate(ref);

		assertThat(violations).isNotEmpty();
		assertThat(violations).anyMatch(v -> 
			v.getPropertyPath().toString().contains("tags") &&
			v.getInvalidValue().equals("test@origin")
		);
	}

	@Test
	void testRefWithValidLocalTagShouldPassValidation() {
		// Test that a Ref with valid local tags (no @) passes validation
		var ref = new Ref();
		ref.setUrl("https://www.example.com/");
		ref.setTags(List.of("test", "valid", "another")); // Valid local tags

		Set<ConstraintViolation<Ref>> violations = validator.validate(ref);

		assertThat(violations).isEmpty();
	}

	@Test
	void testRefWithPrivateTagShouldPassValidation() {
		// Test that a Ref with a private tag (starting with _) passes validation
		var ref = new Ref();
		ref.setUrl("https://www.example.com/");
		ref.setTags(List.of("_private")); // Valid private tag

		Set<ConstraintViolation<Ref>> violations = validator.validate(ref);

		assertThat(violations).isEmpty();
	}

	@Test
	void testRefWithProtectedTagShouldPassValidation() {
		// Test that a Ref with a protected tag (starting with +) passes validation
		var ref = new Ref();
		ref.setUrl("https://www.example.com/");
		ref.setTags(List.of("+user/tester")); // Valid protected tag

		Set<ConstraintViolation<Ref>> violations = validator.validate(ref);

		assertThat(violations).isEmpty();
	}

	@Test
	void testRefWithHierarchicalTagShouldPassValidation() {
		// Test that a Ref with a hierarchical tag (with /) passes validation
		var ref = new Ref();
		ref.setUrl("https://www.example.com/");
		ref.setTags(List.of("category/subcategory")); // Valid hierarchical tag

		Set<ConstraintViolation<Ref>> violations = validator.validate(ref);

		assertThat(violations).isEmpty();
	}

	@Test
	void testRefWithDotSeparatedTagShouldPassValidation() {
		// Test that a Ref with a dot-separated tag (with .) passes validation
		var ref = new Ref();
		ref.setUrl("https://www.example.com/");
		ref.setTags(List.of("domain.name")); // Valid dot-separated tag

		Set<ConstraintViolation<Ref>> violations = validator.validate(ref);

		assertThat(violations).isEmpty();
	}
}
