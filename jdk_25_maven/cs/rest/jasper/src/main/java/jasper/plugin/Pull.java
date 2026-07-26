package jasper.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import jasper.domain.Ref;
import jasper.domain.User;
import jasper.domain.proj.HasTags;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

import static jasper.domain.proj.HasTags.getPlugin;


@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class Pull implements Serializable {
	private boolean websocket = true;
	private boolean cachePrefetch = true;
	private boolean cacheProxy;
	private boolean cacheProxyPrefetch;
	private String query;
	private int batchSize;
	private boolean validatePlugins;
	private boolean stripInvalidPlugins;
	private boolean validateTemplates;
	private boolean stripInvalidTemplates;
	private List<String> addTags;
	private List<String> removeTags;

	// TODO: copy to origin post-processing
	// TODO: conditional tag/origin mapping

	public void migrate(Ref ref, Origin config) {
		if (ref.getTags() != null && removeTags != null) {
			ref.removeTags(removeTags);
			ref.clearPlugins();
		}
		if (addTags != null) {
			ref.addTags(addTags);
		}
	}

	public void migrate(User user, Origin config) {
	}

	private static final Pull DEFAULTS = new Pull();
	public static Pull getPull(HasTags ref) {
		var pull = ref == null ? null : getPlugin(ref, "+plugin/origin/pull", Pull.class);
		return pull == null ? DEFAULTS : pull;
	}
}
