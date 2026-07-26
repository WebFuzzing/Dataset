package jasper.component.script;

import jasper.component.FileCache;
import jasper.component.Tagger;
import jasper.domain.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static jasper.util.Logging.getMessage;

@Profile("proxy & file-cache")
@Component
public class DeltaCache {
	private static final Logger logger = LoggerFactory.getLogger(DeltaCache.class);

	@Autowired
	FileCache fileCache;

	@Autowired
	Tagger tagger;

	public void runScript(Ref ref) {
		logger.info("{} Caching {}", ref.getOrigin(), ref.getUrl());
		tagger.tag(ref.getUrl(), ref.getOrigin(), "-_plugin/delta/cache", "_plugin/cache");
		try {
			fileCache.refresh(ref.getUrl(), ref.getOrigin());
		} catch (Exception e) {
			tagger.attachError(ref.getOrigin(),
				tagger.plugin(ref.getUrl(), ref.getOrigin(), "_plugin/cache", null, "-_plugin/delta/cache"),
				"Error Fetching for _plugin/delta/cache", getMessage(e));
		}
	}
}
