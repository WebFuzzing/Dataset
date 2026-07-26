package jasper.component;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import jasper.errors.NotAvailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static jasper.domain.proj.HasTags.hasMatchingTag;

@Profile("!proxy")
@Component
public class FetchImplCache implements Fetch {
	private static final Logger logger = LoggerFactory.getLogger(FetchImplCache.class);

	@Autowired
	ConfigCache configs;

	@Autowired
	Replicator replicator;

	@Bulkhead(name = "fetch")
	public FileRequest doScrape(String url, String origin) {
		var remote = configs.getRemote(origin);
		if (remote == null) throw new NotAvailableException();
		if (hasMatchingTag(remote, "+plugin/error")) return null;
		return replicator.fetch(url, remote);
	}

}
