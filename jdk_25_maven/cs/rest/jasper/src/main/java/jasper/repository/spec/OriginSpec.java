package jasper.repository.spec;

import jasper.domain.proj.HasOrigin;
import org.springframework.data.jpa.domain.Specification;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class OriginSpec {

	public static <T extends HasOrigin> Specification<T> isOrigin(String origin) {
		if (origin.equals("@*")) return any();
		if (isBlank(origin) || origin.equals("@")) {
			return (root, query, cb) ->
				cb.equal(
					root.get("origin"),
					"");
		} else if (origin.endsWith(".*")) {
			return isUnderOrigin(origin);
		} else {
			return (root, query, cb) ->
				cb.equal(
					root.get("origin"),
					origin);
		}
	}

	public static <T extends HasOrigin> Specification<T> isUnderOrigin(String origin) {
		if (isBlank(origin) || origin.equals("@") || origin.equals("@*")) return any();
		var rootOrigin = origin.endsWith(".*") ? origin.substring(0, origin.length() - 2) : origin;
		return (root, query, cb) ->
			cb.or(
				cb.equal(
					root.get("origin"),
					rootOrigin),
				cb.like(
					root.get("origin"),
					rootOrigin + ".%"));
	}

	public static <T extends HasOrigin> Specification<T> isNesting(int nesting) {
		return (root, query, cb) ->
			cb.equal(
				cb.function("origin_nesting", Integer.class, root.get("origin")),
				nesting);
	}

	public static <T extends HasOrigin> Specification<T> any() {
		return (root, query, cb) -> cb.conjunction();
	}

	public static <T extends HasOrigin> Specification<T> none() {
		return (root, query, cb) -> cb.disjunction();
	}
}
