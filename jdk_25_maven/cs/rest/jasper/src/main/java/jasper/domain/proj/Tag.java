package jasper.domain.proj;

import com.fasterxml.jackson.annotation.JsonIgnore;

import static org.apache.commons.lang3.StringUtils.isBlank;

public interface Tag extends Cursor {
	String REGEX = "[_+]?[a-z0-9]+(?:[./][a-z0-9]+)*";
	String QTAG_REGEX = REGEX + "(?:@|" + HasOrigin.REGEX + ")";
	String ADD_REMOVE_REGEX = "[-]?" + REGEX;
	int TAG_LEN = 64;
	int QTAG_LEN = TAG_LEN + ORIGIN_LEN + 1;

	String getTag();
	void setTag(String tag);
	String getName();

	@JsonIgnore
	default String getQualifiedTag() {
		return getTag() + getOrigin();
	}

	static boolean userUrl(String url) {
		return
			userUrl(url, "user") || url.startsWith("tag:/user/") ||
			userUrl(url, "+user") || url.startsWith("tag:/+user/") ||
			userUrl(url, "_user") || url.startsWith("tag:/_user/");
	}

	static boolean userUrl(String url, String user) {
		return url.equals("tag:/" + user) ||
			url.startsWith("tag:/" + user + "?") ||
			url.startsWith("tag:/" + user + "/") ||
			url.equals("tag:/" + publicTag(user)) ||
			url.startsWith("tag:/" + publicTag(user) + "?") ||
			url.startsWith("tag:/" + publicTag(user) + "/");
	}

	static String urlForTag(String url, String user) {
		if (isBlank(url)) return "tag:/" + publicTag(user);
		return "tag:/" + publicTag(user) + "?url=" + url;
	}

	static boolean tagUrl(String url) {
		return url.startsWith("tag:/");
	}

	static String urlToTag(String url) {
		var tag = url.substring("tag:/".length());
		if (tag.contains("?")) return tag.substring(0, tag.indexOf("?"));
		return tag;
	}

	static String sign(String tag) {
		if (isBlank(tag)) return "";
		if (tag.startsWith("_")) return "_";
		if (tag.startsWith("+")) return "+";
		return "";
	}

	static boolean isPublicTag(String tag) {
		if (isBlank(tag)) return false;
		return !tag.startsWith("_") && !tag.startsWith("+");
	}

	static String publicTag(String tag) {
		if (isBlank(tag) || isPublicTag(tag)) return tag;
		return tag.substring(1);
	}

	static String localTag(String tag) {
		if (isBlank(tag)) return tag;
		if (!tag.contains("@")) return tag;
		return tag.substring(0, tag.indexOf("@"));
	}

	static String tagOrigin(String tag) {
		if (isBlank(tag)) return "";
		if (!tag.contains("@")) return "";
		return tag.substring(tag.indexOf("@"));
	}

	static String defaultOrigin(String tag, String origin) {
		if (isBlank(tag)) return tag;
		if (tag.endsWith("@")) return localTag(tag);
		if (tag.contains("@")) return tag;
		if (isBlank(origin)) return tag;
		if ("@".equals(origin)) return tag;
		return tag + origin;
	}

	static String reverseOrigin(String qualifiedTag) {
		var origin = tagOrigin(qualifiedTag);
		var tag = localTag(qualifiedTag);
		if (isBlank(origin)) return tag;
		if (isBlank(tag)) tag = "user";
		return origin.substring(1) + "/" + tag;
	}

	static boolean matchesTag(String prefix, String tag) {
		if (isBlank(tag) || isBlank(prefix)) return false;
		return prefix.equals(tag)
			|| tag.startsWith(prefix + "/");
	}

	static boolean matchesPublic(String prefix, String tag) {
		if (isBlank(tag) || isBlank(prefix)) return false;
		return publicTag(prefix).equals(publicTag(tag))
			|| publicTag(tag).startsWith(publicTag(prefix) + "/");
	}

	static boolean matchesTemplate(String prefix, String tag) {
		if (isBlank(tag)) return false;
		return isBlank(prefix)
			|| prefix.equals(tag)
			|| prefix.equals(publicTag(tag))
			|| tag.startsWith(prefix + "/")
			|| publicTag(tag).startsWith(prefix + "/");
	}

	/**
	 * _tag can capture _tag, +tag, and tag
	 * +tag can capture +tag and tag
	 * tag can capture tag
	 */
	static boolean matchesDownwards(String upper, String lower) {
		if (upper.equals(lower)) return true;
		if (isPublicTag(upper)) return false;
		if (upper.startsWith("_")) return publicTag(upper).equals(publicTag(lower));
		// Protected tag
		return publicTag(upper).equals(lower);
	}

	/**
	 * _tag can capture _tag, +tag, and tag
	 * +tag can capture +tag and tag
	 * tag can capture tag
	 */
	static boolean capturesDownwards(String upper, String lower) {
		if (matchesTag(upper, lower)) return true;
		if (isPublicTag(upper)) return false;
		if (upper.startsWith("_")) return matchesTag(publicTag(upper), publicTag(lower));
		// Protected tag
		return matchesTag(publicTag(upper), lower);
	}

	static String prefix(String prefix, String tag) {
		if (isBlank(prefix)) return tag;
		if (isBlank(tag)) return prefix;
		var sign = sign(tag);
		if (isBlank(sign)) sign = sign(prefix);
		return sign + publicTag(prefix) + "/" + publicTag(tag);
	}
}
