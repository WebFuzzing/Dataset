package jasper.component.script;

import jakarta.persistence.EntityManager;
import jasper.component.Ingest;
import jasper.component.Scraper;
import jasper.component.Tagger;
import jasper.domain.Ref;
import jasper.errors.ModifiedException;
import jasper.errors.NotFoundException;
import jasper.repository.RefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static jasper.domain.proj.HasOrigin.origin;
import static jasper.domain.proj.Tag.matchesTag;
import static jasper.util.Logging.getMessage;

@Profile("proxy")
@Component
public class DeltaScrape {
	private static final Logger logger = LoggerFactory.getLogger(DeltaScrape.class);

	@Autowired
	Scraper scraper;

	@Autowired
	Ingest ingest;

	@Autowired
	Tagger tagger;

	@Autowired
	RefRepository refRepository;

	@Autowired
	EntityManager em;

	public void runScript(Ref ref) {
		try {
			logger.info("{} Scraping {}", ref.getOrigin(), ref.getUrl());
			var tags = ref.getExpandedTags();
			tagger.tag(ref.getUrl(), ref.getOrigin(), "-_plugin/delta/scrape");
			var web = scraper.web(ref.getUrl(), ref.getOrigin());
			// Fetch Ref again in case scrape modified it
			ref = fetch(ref.getUrl(), ref.getOrigin());
			em.detach(ref);
			var scrapeAll = tags.stream().anyMatch(t -> matchesTag("_plugin/delta/scrape/ref", t));
			if (scrapeAll || tags.stream().anyMatch(t -> matchesTag("_plugin/delta/scrape/title", t))) ref.setTitle(web.getTitle());
			if (scrapeAll || tags.stream().anyMatch(t -> matchesTag("_plugin/delta/scrape/comment", t))) ref.setComment(web.getComment());
			if (scrapeAll || tags.stream().anyMatch(t -> matchesTag("_plugin/delta/scrape/sources", t))) ref.setSources(web.getSources());
			if (scrapeAll || tags.stream().anyMatch(t -> matchesTag("_plugin/delta/scrape/alts", t))) ref.setAlternateUrls(web.getAlternateUrls());
			if (scrapeAll || tags.stream().anyMatch(t -> matchesTag("_plugin/delta/scrape/plugins", t))) ref.setPlugins(web.getPlugins());
			if (scrapeAll || tags.stream().anyMatch(t -> matchesTag("_plugin/delta/scrape/tags", t))) ref.setTags(web.getTags());
			if (scrapeAll || tags.stream().anyMatch(t -> matchesTag("_plugin/delta/scrape/published", t))) ref.setPublished(web.getPublished());
			ref.removeTag("_plugin/delta/scrape");
			ingest.update(ref.getOrigin(), ref);
		} catch (ModifiedException ignored) {
		} catch (Exception e) {
			logger.warn("{} Unexpected error scraping Ref {}", ref.getOrigin(), ref.getUrl());
			tagger.attachError(ref.getUrl(), ref.getOrigin(), "Error Fetching for _plugin/delta/scrape", getMessage(e));
		}
	}

	private Ref fetch(String url, String origin) {
		return refRepository.findOneByUrlAndOrigin(url, origin(origin))
			.orElseThrow(() -> new NotFoundException("Async"));
	}
}
