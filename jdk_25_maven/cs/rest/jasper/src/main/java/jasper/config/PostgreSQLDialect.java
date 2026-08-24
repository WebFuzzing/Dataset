package jasper.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.dialect.function.StandardSQLFunction;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;

public class PostgreSQLDialect extends org.hibernate.dialect.PostgreSQLDialect {
	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		var functionRegistry = functionContributions.getFunctionRegistry();
		var string = functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.STRING);
		var bool = functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN);
		var integer = functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.INTEGER);
		var doubleType = functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.DOUBLE);
		var jsonb = functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(Object.class, SqlTypes.JSON);
		functionRegistry.register("age", new StandardSQLFunction("age", StandardBasicTypes.DURATION));
		functionRegistry.registerPattern("jsonb_exists", "jsonb_exists(?1, ?2)", bool);
		functionRegistry.registerPattern("jsonb_extract_path", "jsonb_extract_path(?1, ?2)", jsonb);
		functionRegistry.registerPattern("jsonb_object_field", "(?1)->(?2)", jsonb);
		functionRegistry.registerPattern("jsonb_object_field_text", "(?1)->>(?2)", string);
		functionRegistry.registerPattern("jsonb_set", "jsonb_set(?1, ?2, ?3, ?4)", jsonb);
		functionRegistry.registerPattern("jsonb_strip_nulls", "jsonb_strip_nulls(?1)", jsonb);
		functionRegistry.registerPattern("cast_to_jsonb", "?1::jsonb", jsonb);
		functionRegistry.registerPattern("jsonb_concat", "jsonb_concat(?1, ?2)", jsonb);
		functionRegistry.registerPattern("cast_to_int", "(?1)::integer", integer);
		functionRegistry.registerPattern("cast_to_numeric", "(?1)::numeric", doubleType);
		functionRegistry.registerPattern("jsonb_array_length", "jsonb_array_length(?1)", integer);
		functionRegistry.registerPattern("jsonb_array_element_text", "jsonb_array_element_text(?1, ?2)", string);
		// origin_nesting: returns 0 for blank or '@', otherwise count of '.' + 1
		functionRegistry.registerPattern("origin_nesting", "CASE WHEN ?1 = '' OR ?1 = '@' THEN 0 ELSE (LENGTH(?1) - LENGTH(REPLACE(?1, '.', '')) + 1) END", integer);
		// tag_levels: returns 0 for blank tag, otherwise count of '/' + 1
		functionRegistry.registerPattern("tag_levels", "CASE WHEN ?1 = '' THEN 0 ELSE (LENGTH(?1) - LENGTH(REPLACE(?1, '/', '')) + 1) END", integer);
		// Vote sorting functions - kept together for consistency
		functionRegistry.registerPattern("vote_top", "COALESCE((?1->'plugins'->>'plugin/user/vote/up')::int, 0) + COALESCE((?1->'plugins'->>'plugin/user/vote/down')::int, 0)", integer);
		functionRegistry.registerPattern("vote_score", "COALESCE((?1->'plugins'->>'plugin/user/vote/up')::int, 0) - COALESCE((?1->'plugins'->>'plugin/user/vote/down')::int, 0)", integer);
		functionRegistry.registerPattern("vote_decay", "floor((3 + COALESCE((?1->'plugins'->>'plugin/user/vote/up')::int, 0) - COALESCE((?1->'plugins'->>'plugin/user/vote/down')::int, 0)) * pow(CASE WHEN 3 + COALESCE((?1->'plugins'->>'plugin/user/vote/up')::int, 0) > COALESCE((?1->'plugins'->>'plugin/user/vote/down')::int, 0) THEN 0.5 ELSE 2 END, EXTRACT('EPOCH' FROM age(?2)) / (4 * 60 * 60)))", doubleType);
		// Collation function for binary ASCII sorting (+ comes before _)
		functionRegistry.registerPattern("collate_c", "(?1) COLLATE \"C\"", string);
		// jsonb_array_append: append a text value to a JSON array
		functionRegistry.registerPattern("jsonb_array_append", "(?1 || to_jsonb(CAST(?2 AS text)))", jsonb);
	}

}
