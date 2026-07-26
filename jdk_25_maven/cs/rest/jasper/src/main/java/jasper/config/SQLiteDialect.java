package jasper.config;

import org.hibernate.boot.model.FunctionContributions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.SqlTypes;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.JdbcType;

import java.sql.*;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

public class SQLiteDialect extends org.hibernate.community.dialect.SQLiteDialect {

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);
		// Register for all timestamp type codes: Hibernate 6+ maps Instant to TIMESTAMP_UTC,
		// not plain TIMESTAMP, so we must cover all variants to intercept Instant binding.
		typeContributions.contributeJdbcType(new NanoTimestampJdbcType(Types.TIMESTAMP));
		typeContributions.contributeJdbcType(new NanoTimestampJdbcType(Types.TIMESTAMP_WITH_TIMEZONE));
		typeContributions.contributeJdbcType(new NanoTimestampJdbcType(SqlTypes.TIMESTAMP_UTC));
	}

	@Override
	public void initializeFunctionRegistry(FunctionContributions functionContributions) {
		super.initializeFunctionRegistry(functionContributions);
		var functionRegistry = functionContributions.getFunctionRegistry();
		var string = functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.STRING);
		var bool = functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.BOOLEAN);
		var integer = functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.INTEGER);
		var doubleType = functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(StandardBasicTypes.DOUBLE);
		var jsonb = functionContributions.getTypeConfiguration().getBasicTypeRegistry().resolve(Object.class, SqlTypes.JSON);
		// jsonb_exists: check if a JSON array elements or object keys contain the given value (PostgreSQL-compatible)
		functionRegistry.registerPattern(
			"jsonb_exists",
			"(CASE " +
				"WHEN json_type(?1) = 'object' " +
					"THEN (?2 IN (SELECT je.key FROM json_each(?1) je)) " +
				"ELSE (?2 IN (SELECT je.value FROM json_each(?1) je)) " +
			"END)",
			bool
		);
		// jsonb_exists_any: check if a JSON array elements or object keys contain any of the values in a comma-separated list
		functionRegistry.registerPattern(
			"jsonb_exists_any",
			"(CASE " +
				"WHEN json_type(?1) = 'object' " +
					"THEN EXISTS (SELECT 1 FROM json_each(?1) je WHERE INSTR(',' || ?2 || ',', ',' || je.key || ',') > 0) " +
				"ELSE EXISTS (SELECT 1 FROM json_each(?1) je WHERE INSTR(',' || ?2 || ',', ',' || je.value || ',') > 0) " +
			"END)",
			bool
		);
		// jsonb_extract_path: extract a JSON value at a dotted path (used with simple keys)
		functionRegistry.registerPattern("jsonb_extract_path", "json_extract(?1, '$.' || ?2)", jsonb);
		functionRegistry.registerPattern("jsonb_extract_path_text",
			"(CASE " +
				"WHEN json_type(?1, '$.' || ?2) IN ('true', 'false') " +
					"THEN json_type(?1, '$.' || ?2) " +
				"ELSE CAST(json_extract(?1, '$.' || ?2) AS TEXT) " +
			"END)",
			string);
		// jsonb_object_field: get a JSON field by key (equivalent to PostgreSQL's -> operator)
		// Use quoted key to handle keys with special characters (e.g., plugin/user/...)
		functionRegistry.registerPattern(
			"jsonb_object_field",
			"json_extract(?1, '$.\"' || REPLACE(?2, '\"', '\"\"') || '\"')",
			jsonb
		);
		// jsonb_object_field_text: get a JSON field as text (equivalent to PostgreSQL's ->> operator)
		// Uses json_type() for booleans since json_extract returns 1/0 for JSON true/false,
		// while PostgreSQL's ->> returns "true"/"false" text.
		functionRegistry.registerPattern(
			"jsonb_object_field_text",
			"(CASE " +
				"WHEN json_type(?1, '$.\"' || REPLACE(?2, '\"', '\"\"') || '\"') IN ('true', 'false') " +
					"THEN json_type(?1, '$.\"' || REPLACE(?2, '\"', '\"\"') || '\"') " +
				"ELSE CAST(json_extract(?1, '$.\"' || REPLACE(?2, '\"', '\"\"') || '\"') AS TEXT) " +
			"END)",
			string
		);
		// jsonb_set: set a JSON value at a path, converting PostgreSQL path {key} to SQLite $.key
		// 4-arg version: 4th arg (create_if_missing) is always true in SQLite's json_set, so ignored
		functionRegistry.registerPattern("jsonb_set", "json_set(?1, '$.' || REPLACE(REPLACE(?2, '{', ''), '}', ''), CASE WHEN ?4 IS NOT NULL THEN ?3 ELSE ?3 END)", jsonb);
		// cast_to_jsonb: cast text to JSON
		functionRegistry.registerPattern("cast_to_jsonb", "json(?1)", jsonb);
		// jsonb_concat: merge two JSON objects (like PostgreSQL's || operator for objects)
		functionRegistry.registerPattern("jsonb_concat", "json_patch(?1, ?2)", jsonb);
		// cast_to_int / cast_to_numeric
		functionRegistry.registerPattern("cast_to_int", "CAST((?1) AS INTEGER)", integer);
		functionRegistry.registerPattern("cast_to_numeric", "CAST((?1) AS REAL)", doubleType);
		// jsonb_array_length
		functionRegistry.registerPattern("jsonb_array_length", "json_array_length(?1)", integer);
		// jsonb_array_element_text: get element at index from a JSON array
		functionRegistry.registerPattern("jsonb_array_element_text", "json_extract(?1, '$[' || ?2 || ']')", string);
		// string_to_array: in SQLite just pass through the comma-separated string (used with jsonb_exists_any)
		functionRegistry.registerPattern("string_to_array", "(?1 || SUBSTR('', 1, 0 * LENGTH(?2)))", string);
		// origin_nesting: returns 0 for blank or '@', otherwise count of '.' + 1
		functionRegistry.registerPattern("origin_nesting", "CASE WHEN ?1 = '' OR ?1 = '@' THEN 0 ELSE (LENGTH(?1) - LENGTH(REPLACE(?1, '.', '')) + 1) END", integer);
		// tag_levels: returns 0 for blank tag, otherwise count of '/' + 1
		functionRegistry.registerPattern("tag_levels", "CASE WHEN ?1 = '' THEN 0 ELSE (LENGTH(?1) - LENGTH(REPLACE(?1, '/', '')) + 1) END", integer);
		// Vote sorting functions using SQLite's json_extract with quoted keys for paths containing '/'
		functionRegistry.registerPattern("vote_top", "COALESCE(CAST(json_extract(?1, '$.plugins.\"plugin/user/vote/up\"') AS INTEGER), 0) + COALESCE(CAST(json_extract(?1, '$.plugins.\"plugin/user/vote/down\"') AS INTEGER), 0)", integer);
		functionRegistry.registerPattern("vote_score", "COALESCE(CAST(json_extract(?1, '$.plugins.\"plugin/user/vote/up\"') AS INTEGER), 0) - COALESCE(CAST(json_extract(?1, '$.plugins.\"plugin/user/vote/down\"') AS INTEGER), 0)", doubleType);
		// vote_decay: simplified decay formula for SQLite (uses julianday for time difference)
		functionRegistry.registerPattern("vote_decay", "(3 + COALESCE(CAST(json_extract(?1, '$.plugins.\"plugin/user/vote/up\"') AS INTEGER), 0) - COALESCE(CAST(json_extract(?1, '$.plugins.\"plugin/user/vote/down\"') AS INTEGER), 0)) * 1.0 / (1 + (julianday('now') - julianday(?2)) * 6)", doubleType);
		// Collation function for binary sorting (SQLite uses BINARY collation)
		functionRegistry.registerPattern("collate_c", "(?1) COLLATE BINARY", string);
		// Full-text search: use FTS5 via textsearch_en (stores rowid) correlated with ref_fts virtual table
		functionRegistry.registerPattern("websearch_to_tsquery", "'\"' || REPLACE(?1, '\"', '\"\"') || '\"'", string);
		functionRegistry.registerPattern("ts_match_vq", "EXISTS (SELECT 1 FROM ref_fts WHERE ref_fts MATCH ?2 AND ref_fts.rowid = CAST(?1 AS INTEGER))", bool);
		functionRegistry.registerPattern("ts_rank_cd", "COALESCE((SELECT bm25(ref_fts) FROM ref_fts WHERE ref_fts MATCH ?2 AND ref_fts.rowid = CAST(?1 AS INTEGER)), 0)", doubleType);
		// jsonb_array_append: append a text value to a JSON array
		functionRegistry.registerPattern("jsonb_array_append", "json_insert(?1, '$[#]', ?2)", jsonb);
	}

	/**
	 * Custom JdbcType that stores {@link Instant} as TEXT with fixed 9-digit
	 * nanosecond precision (e.g., {@code 2024-01-15T10:30:45.123456789Z}).
	 * <p>
	 * The SQLite JDBC driver's built-in timestamp formatting uses
	 * {@code SimpleDateFormat} which only supports millisecond precision.
	 * This type bypasses that by using {@code setString}/{@code getString}
	 * with a {@code DateTimeFormatter} that preserves full nanosecond precision.
	 * <p>
	 * The fixed-width format ensures correct lexicographic ordering in SQLite
	 * TEXT columns and correct UNIQUE constraint behavior at nanosecond granularity.
	 */
	static class NanoTimestampJdbcType implements JdbcType {
		private final int jdbcTypeCode;

		NanoTimestampJdbcType(int jdbcTypeCode) {
			this.jdbcTypeCode = jdbcTypeCode;
		}

		static final DateTimeFormatter NANO_TIMESTAMP_FORMATTER = new DateTimeFormatterBuilder()
			.appendPattern("yyyy-MM-dd'T'HH:mm:ss")
			.appendFraction(ChronoField.NANO_OF_SECOND, 9, 9, true)
			.appendLiteral('Z')
			.toFormatter()
			.withZone(ZoneOffset.UTC);

		@Override
		public int getJdbcTypeCode() {
			return jdbcTypeCode;
		}

		@Override
		public int getDefaultSqlTypeCode() {
			return jdbcTypeCode;
		}

		@Override
		public String getFriendlyName() {
			return "NANO_TIMESTAMP_TEXT";
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaType<X> javaType) {
			return new BasicBinder<>(javaType, this) {
				@Override
				protected void doBind(PreparedStatement st, X value, int index, WrapperOptions options) throws SQLException {
					var instant = javaType.unwrap(value, Instant.class, options);
					st.setString(index, instant == null ? null : NANO_TIMESTAMP_FORMATTER.format(instant));
				}

				@Override
				protected void doBind(CallableStatement st, X value, String name, WrapperOptions options) throws SQLException {
					var instant = javaType.unwrap(value, Instant.class, options);
					st.setString(name, instant == null ? null : NANO_TIMESTAMP_FORMATTER.format(instant));
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(JavaType<X> javaType) {
			return new BasicExtractor<>(javaType, this) {
				@Override
				protected X doExtract(ResultSet rs, int paramIndex, WrapperOptions options) throws SQLException {
					var s = rs.getString(paramIndex);
					return s == null ? null : javaType.wrap(Instant.parse(s), options);
				}

				@Override
				protected X doExtract(CallableStatement cs, int paramIndex, WrapperOptions options) throws SQLException {
					var s = cs.getString(paramIndex);
					return s == null ? null : javaType.wrap(Instant.parse(s), options);
				}

				@Override
				protected X doExtract(CallableStatement cs, String name, WrapperOptions options) throws SQLException {
					var s = cs.getString(name);
					return s == null ? null : javaType.wrap(Instant.parse(s), options);
				}
			};
		}
	}
}
