package jasper.domain.validator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jsontypedef.jtd.InvalidSchemaException;
import com.jsontypedef.jtd.Schema;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SchemaValidator implements ConstraintValidator<SchemaValid, JsonNode> {
	public static final ObjectMapper mapper = new ObjectMapper();

	@Override
	public boolean isValid(JsonNode value, ConstraintValidatorContext context) {
		var schema = mapper.convertValue(value, Schema.class);
		if (value == null) return true;
		try {
			schema.verify();
			return true;
		} catch (InvalidSchemaException e) {
			return false;
		}
	}
}
