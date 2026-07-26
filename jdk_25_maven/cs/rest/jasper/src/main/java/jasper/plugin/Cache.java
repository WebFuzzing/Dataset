package jasper.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jasper.domain.proj.HasTags;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

import static jasper.domain.proj.HasTags.getPlugin;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
public class Cache implements Serializable {
	private String id;
	private String mimeType;
	private boolean ban;
	private boolean noStore;
	private boolean thumbnail;
	private Long contentLength;

	public static Cache getCache(HasTags ref) {
		return ref == null ? null : getPlugin(ref, "_plugin/cache", Cache.class);
	}

	public static boolean bannedOrBroken(Cache cache) {
		return bannedOrBroken(cache, false);
	}

	public static boolean bannedOrBroken(Cache cache, boolean refresh) {
		if (cache == null) return false;
		return
			// URL has been banned
			cache.isBan() ||
			// If id is blank the last scrape must have failed
			// Wait for the user to manually refresh
			!refresh && isBlank(cache.getId());
	}
}
