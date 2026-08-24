package jasper.repository.spec;

import jasper.domain.Ref;
import jasper.domain.proj.HasOrigin;
import jasper.domain.proj.Tag;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.stream.Collectors;

import static jasper.repository.spec.OriginSpec.isOrigin;
import static jasper.repository.spec.RefSpec.hasDownwardTag;
import static jasper.repository.spec.RefSpec.hasTag;
import static jasper.repository.spec.TagSpec.isDownwardTag;
import static jasper.repository.spec.TagSpec.isTag;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class QualifiedTag {
	public static final String SELECTOR = "(?:\\*|" + Tag.REGEX + "|(?:" + Tag.REGEX + ")?(?:" + HasOrigin.REGEX_NOT_BLANK + "(?:[.][*])?|@\\*?))";

	public final boolean not;
	public final String tag;
	public final String origin;

	protected QualifiedTag(String qt) {
		if (isBlank(qt)) {
			not = false;
			tag =  "";
			origin = "";
		} else {
			not = qt.startsWith("!");
			if (not) qt = qt.substring(1);
			var index = qt.indexOf("@");
			if (index == -1) {
				if (qt.equals("*")) {
					tag =  "";
					origin = "";
				} else {
					tag =  qt;
					origin = "@*";
				}
			} else {
				var rhs = qt.substring(index);
				tag = qt.substring(0, index);
				origin = rhs.equals("@") ? "" : rhs;
			}
		}
	}

	@Override
	public String toString() {
		return (not ? "!" : "") + tag
			+ (origin.equals("@") ? ""
			: origin.equals("@*") ? ""
			: origin);
	}

	public boolean matches(String qt) {
		return matches(selector(qt));
	}

	public boolean matches(QualifiedTag qt) {
		return tag.equals(qt.tag) && origin.equals(qt.origin) && not == qt.not;
	}

	public boolean matchesDownwards(QualifiedTag qt) {
		return Tag.matchesDownwards(tag, qt.tag) && origin.equals(qt.origin) && not == qt.not;
	}

	public boolean captures(String c) {
		return captures(selector(c));
	}

	public boolean captures(QualifiedTag c) {
		if (!tag.isEmpty() && !(tag.equals(c.tag) || c.tag.startsWith(tag + "/"))) return not;
		if (!origin.endsWith("*") && !origin.equals(c.origin)) return not;
		if (origin.endsWith(".*") && !c.origin.equals(origin.substring(0, origin.length() - 2)) && !c.origin.startsWith(origin.substring(0, origin.length() - 1))) return not;
		return !not;
	}

	public boolean capturesDownwards(QualifiedTag c) {
		if (!tag.isEmpty() && !Tag.capturesDownwards(tag, c.tag)) return not;
		if (!origin.endsWith("*") && !origin.equals(c.origin)) return not;
		if (origin.endsWith(".*") && !c.origin.equals(origin.substring(0, origin.length() - 2)) && !c.origin.startsWith(origin.substring(0, origin.length() - 1))) return not;
		return !not;
	}

	public Specification<Ref> refSpec() {
		var spec = Specification.<Ref>unrestricted();
		if (isNotBlank(tag)) spec = spec.and(hasTag(tag));
		spec = spec.and(isOrigin(origin));
		return not ? Specification.not(spec) : spec;
	}

	public Specification<Ref> downwardRefSpec() {
		var spec = Specification.<Ref>unrestricted();
		if (isNotBlank(tag)) spec = spec.and(hasDownwardTag(tag));
		spec = spec.and(isOrigin(origin));
		return not ? Specification.not(spec) : spec;
	}

	public <T extends Tag> Specification<T> spec() {
		var spec = Specification.<T>unrestricted();
		if (isNotBlank(tag)) spec = spec.and(isTag(tag));
		spec = spec.and(isOrigin(origin));
		return not ? Specification.not(spec) : spec;
	}

	public <T extends Tag> Specification<T> downwardSpec() {
		var spec = Specification.<T>unrestricted();
		if (isNotBlank(tag)) spec = spec.and(isDownwardTag(tag));
		spec = spec.and(isOrigin(origin));
		return not ? Specification.not(spec) : spec;
	}

	/**
	 * Selector that represents an origin, but may be a wildcard or fixed tag.
	 * Missing origins will be set to the default origin.
	 */
	public static QualifiedTag tagOriginSelector(String qt) {
		if (qt.startsWith("!")) throw new UnsupportedOperationException();
		if (qt.startsWith("*")) throw new UnsupportedOperationException();
		if (!qt.contains("@")) qt += "@"; // Missing origin implies default origin, not wildcard
		return new QualifiedTag(qt);
	}

	/**
	 * Selector that represents a fixed tag and origin.
	 * Missing origins will be set to the default origin.
	 */
	public static QualifiedTag qt(String qt) {
		if (qt.startsWith("!")) throw new UnsupportedOperationException();
		if (qt.startsWith("@")) throw new UnsupportedOperationException();
		if (qt.contains("*")) throw new UnsupportedOperationException();
		if (!qt.contains("@")) qt += "@"; // Missing origin implies default origin, not wildcard
		return new QualifiedTag(qt);
	}

	/**
	 * Selector that can include wildcards.
	 */
	public static QualifiedTag selector(String qt) {
		if (qt.startsWith("!")) throw new UnsupportedOperationException();
		return new QualifiedTag(qt);
	}

	/**
	 * Origin selector that can include wildcards.
	 */
	public static QualifiedTag originSelector(String qt) {
		if (qt.isEmpty()) return selector("*");
		return selector(qt);
	}

	public static String concat(String ...tags) {
		var result = new StringBuilder();
		for (var tag : tags) {
			if (isEmpty(tag)) continue;
			if (tag.startsWith("@")) tag = tag.substring(1);
			if (!result.isEmpty()) {
				result.append("/");
				if (tag.startsWith("+") || tag.startsWith("_")) tag = tag.substring(1);
			}
			result.append(tag);
		}
		return result.toString();
	}

	/**
	 * Selector that represents a query atom. Can include wildcards or negations.
	 */
	public static QualifiedTag atom(String qt) {
		return new QualifiedTag(qt);
	}

	public static List<QualifiedTag> qtList(String defaultOrigin, List<String> tags) {
		var origin = isBlank(defaultOrigin) ? "@" : defaultOrigin;
		return tags.stream()
			.map(t -> t.contains("@") ? t : t + origin)
			.map(QualifiedTag::qt)
			.collect(Collectors.toList());
	}

	public static List<QualifiedTag> tagOriginList(List<String> tags) {
		return tags.stream()
			.map(QualifiedTag::tagOriginSelector)
			.collect(Collectors.toList());
	}

	public static List<QualifiedTag> selectors(String defaultOrigin, List<String> tags) {
		var origin = isBlank(defaultOrigin) ? "@" : defaultOrigin;
		return tags.stream()
			.map(t -> t.contains("@") ? t : t + origin)
			.map(QualifiedTag::selector)
			.collect(Collectors.toList());
	}
}
