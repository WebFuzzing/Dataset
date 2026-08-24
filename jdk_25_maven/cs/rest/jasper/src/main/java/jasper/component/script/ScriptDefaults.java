package jasper.component.script;

import jasper.domain.Ref;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class ScriptDefaults {

	@Autowired
	RssParser rssParser;

	@Autowired
	Optional<DeltaCache> deltaCache;

	@Autowired
	Optional<DeltaScrape> deltaScrape;

	public void runScript(Ref ref, String scriptTag) {
		switch (scriptTag) {
			case "plugin/script/feed":
				rssParser.runScript(ref, "plugin/script/feed");
				break;
			case "_plugin/delta/scrape":
				if (deltaScrape.isPresent()) deltaScrape.get().runScript(ref);
				break;
			case "_plugin/delta/cache":
				if (deltaCache.isPresent()) deltaCache.get().runScript(ref);
				break;
		}
	}
}
