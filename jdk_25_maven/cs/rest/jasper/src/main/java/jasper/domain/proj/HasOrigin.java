package jasper.domain.proj;

import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static jasper.domain.proj.Tag.tagOrigin;
import static java.util.Arrays.stream;
import static org.apache.commons.lang3.StringUtils.countMatches;
import static org.apache.commons.lang3.StringUtils.isBlank;

public interface HasOrigin {
	String REGEX_NOT_BLANK = "@[a-z0-9]+(?:[.][a-z0-9]+)*";
	String REGEX = "(?:" + REGEX_NOT_BLANK + ")?";
	int ORIGIN_LEN = 64;

	String getOrigin();
	void setOrigin(String origin);

	static String origin(String origin) {
		if (origin == null) return "";
		if (origin.equals("default")) return "";
		return origin;
	}

	static String formatOrigin(Object origin) {
		if (isBlank((String) origin)) return "default";
		return origin.toString();
	}

	static String subOrigin(String local, String origin) {
		if (local == null) local = "";
		if (origin == null) origin = "";
		if (isBlank(local)) return origin;
		if (isBlank(origin)) return local;
		if (origin.startsWith("@")) origin = origin.substring(1);
		return local + '.' + origin;
	}

	static int nesting(String origin) {
		if (isBlank(origin) || origin.equals("@")) return 0;
		return countMatches(origin, '.') + 1;
	}

	static String[] parts(String origin) {
		if (isBlank(origin)) return new String[]{ "" };
		return origin.substring(1).split("\\.");
	}

	static String parentOrigin(String origin) {
		if (isBlank(origin)) return "";
		if (!origin.contains(".")) return "";
		return origin.substring(0, origin.lastIndexOf("."));
	}

	static String fromParts(String ...parts) {
		if (parts == null) return "";
		if (parts.length == 0) return "";
		if (parts.length == 1 && isBlank(parts[0])) return "";
		return "@" + String.join(".",
			stream(parts)
			.map(p -> p.replaceAll("[@]", ""))
			.filter(StringUtils::isNotBlank)
			.toList());
	}

	static boolean isSubOrigin(String local, String origin) {
		if (isBlank(local)) return true;
		origin = tagOrigin(origin);
		if (isBlank(origin)) return isBlank(local);
		if (origin.equals(local)) return true;
		return origin.startsWith(local+".");
	}

	static List<String> originHierarchy(@Nullable Object o) {
		var origin = origin((String) o);
		if (isBlank(origin)) return List.of("");
		var result = new ArrayList<String>();
		result.add(origin);
		while (origin.contains(".")) {
			origin = origin.substring(0, origin.lastIndexOf("."));
			result.add(origin);
		}
		result.add("");
		return result;
	}
}
