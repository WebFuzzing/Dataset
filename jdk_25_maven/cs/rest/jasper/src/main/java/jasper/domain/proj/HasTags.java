package jasper.domain.proj;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jasper.repository.spec.QualifiedTag;
import jasper.service.dto.RefDto;

import java.util.List;

import static jasper.config.JacksonConfiguration.om;
import static jasper.domain.proj.Tag.matchesTag;
import static org.apache.commons.lang3.StringUtils.isBlank;

public interface HasTags extends Cursor {
	String getTitle();
	String getUrl();
	List<String> getTags();
	void setTags(List<String> tags);
	ObjectNode getPlugins();
	void setPlugins(ObjectNode plugins);

	static String formatTag(Object tag) {
		if (isBlank((String) tag)) return "~";
		return tag.toString();
	}

	static boolean hasMedia(HasTags hasTags) {
		if (hasTags == null) return false;
		if (hasTags.getTags() == null) return false;
		return hasMatchingTag(hasTags, "plugin/audio") ||
			hasMatchingTag(hasTags, "plugin/video") ||
			hasMatchingTag(hasTags, "plugin/image") ||
			hasMatchingTag(hasTags, "plugin/embed");
	}

	static boolean hasMatchingTag(HasTags hasTags, String prefix) {
		if (hasTags == null) return false;
		if (hasTags.getTags() == null) return false;
		return hasTags.getTags().stream().anyMatch(t -> matchesTag(prefix, t));
	}

	static boolean hasCapturingTag(List<String> tags, String origin, QualifiedTag selector) {
		if (tags == null) return false;
		return tags.stream().anyMatch(t -> selector.captures(t + origin));
	}

	static boolean author(String tag) {
		if (isBlank(tag)) return false;
		return "+user".equals(tag)
			|| "_user".equals(tag)
			|| tag.startsWith("+user/")
			|| tag.startsWith("_user/");
	}

	static List<String> authors(HasTags hasTags) {
		if (hasTags == null) return List.of();
		if (hasTags.getTags() == null) return List.of();
		return hasTags.getTags().stream()
					  .filter(HasTags::author)
					  .toList();
	}

	static <T> T getPlugin(HasTags ref, String tag, Class<T> toValueType) {
		if (ref == null) return null;
		if (ref.getPlugins() == null) return null;
		if (!ref.getPlugins().has(tag)) return null;
		return om().convertValue(ref.getPlugins().get(tag), toValueType);
	}

	static boolean hasPluginResponse(RefDto ref, String tag) {
		if (ref.getMetadata() == null) return false;
		if (ref.getMetadata().getPlugins() == null) return false;
		return ref.getMetadata().getPlugins().keySet().stream()
			.filter(t -> matchesTag(tag, t))
			.anyMatch(t -> ref.getMetadata().getPlugins().get(t) > 0);
	}
}
