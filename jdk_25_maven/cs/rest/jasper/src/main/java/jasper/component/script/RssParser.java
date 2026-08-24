package jasper.component.script;

import com.rometools.modules.itunes.ITunes;
import com.rometools.modules.mediarss.MediaEntryModuleImpl;
import com.rometools.modules.mediarss.MediaModule;
import com.rometools.rome.feed.module.DCModule;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.ParsingFeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import feign.FeignException;
import jasper.client.JasperClient;
import jasper.client.dto.JasperMapper;
import jasper.component.HttpClientFactory;
import jasper.component.Ingest;
import jasper.component.Sanitizer;
import jasper.component.Tagger;
import jasper.domain.Ref;
import jasper.domain.proj.HasTags;
import jasper.errors.NotFoundException;
import jasper.plugin.Audio;
import jasper.plugin.Feed;
import jasper.plugin.Thumbnail;
import jasper.plugin.Video;
import jasper.repository.RefRepository;
import jasper.security.HostCheck;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.jdom2.input.JDOMParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.Year;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static jasper.plugin.Cron.getCron;
import static jasper.plugin.Feed.getFeed;
import static jasper.security.Auth.LOCAL_ORIGIN_HEADER;
import static jasper.security.Auth.USER_ROLE_HEADER;
import static jasper.security.Auth.USER_TAG_HEADER;
import static jasper.security.AuthoritiesConstants.USER;
import static jasper.util.Logging.getMessage;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class RssParser {
	private static final Logger logger = LoggerFactory.getLogger(RssParser.class);

	@Value("http://localhost:${server.port}")
	String api;

	@Autowired
	JasperClient jasperClient;

	@Autowired
	JasperMapper mapper;

	@Autowired
	HostCheck hostCheck;

	@Autowired
	Ingest ingest;

	@Autowired
	Tagger tagger;

	@Autowired
	Sanitizer sanitizer;

	@Autowired
	RefRepository refRepository;

	@Autowired
	HttpClientFactory httpClientFactory;

	public void runScript(Ref ref, String scriptTag) {
		logger.info("{} Scraping {} feed: {}.", ref.getOrigin(), ref.getTitle(), ref.getUrl());
		try {
			scrape(ref, scriptTag);
		} catch (ParsingFeedException e) {
			if (e.getLineNumber() == 1 || e.getCause() instanceof JDOMParseException) {
				// Temporary error page, retry later
				logger.warn("{} Error parsing feed {}: {}", ref.getOrigin(), ref.getUrl(), getMessage(e));
			} else {
				tagger.attachError(ref.getUrl(), ref.getOrigin(), "Error parsing feed", getMessage(e));
			}
		} catch (IllegalArgumentException e) {
			// Temporary error page, retry later
			logger.warn("{} Error parsing feed {}: {}", ref.getOrigin(), ref.getUrl(), getMessage(e));
		} catch (FeedException e) {
			tagger.attachError(ref.getUrl(), ref.getOrigin(), "Error scraping feed", getMessage(e));
		} catch (SSLException e) {
			// Temporary error page, retry later
			tagger.attachLogs(ref.getUrl(), ref.getOrigin(), "Error with feed SSL", getMessage(e));
		} catch (SocketTimeoutException e) {
			// Temporary network timeout, retry later
			tagger.attachLogs(ref.getUrl(), ref.getOrigin(), "Timeout loading feed", getMessage(e));
		} catch (IOException e) {
			// Temporary network timeout, retry later
			tagger.attachLogs(ref.getUrl(), ref.getOrigin(), "Error loading feed", getMessage(e));
		} catch (Throwable e) {
			tagger.attachError(ref.getUrl(), ref.getOrigin(), "Unexpected error scraping feed", getMessage(e));
		}
		logger.info("{} Finished scraping feed: {}.", ref.getOrigin(), ref.getUrl());
	}

	private void scrape(Ref feed, String scriptTag) throws IOException, FeedException {
		var config = getFeed(feed, scriptTag);

		try (var client = httpClientFactory.getClient()) {
			var request = new HttpGet(feed.getUrl());
			if (!hostCheck.validHost(request.getURI())) {
				logger.info("{} Invalid host {}", feed.getOrigin(), request.getURI().getHost());
				return;
			}

			if (!config.isDisableEtag() && config.getEtag() != null) {
				request.setHeader(HttpHeaders.IF_NONE_MATCH, config.getEtag());
			}
			Instant lastScrape = null;
			var cron = getCron(feed);
			if (cron != null && cron.getInterval() != null) {
				lastScrape = Instant.now().minus(cron.getInterval());
				if (lastScrape.isAfter(feed.getModified()) &&
					lastScrape.isAfter(Instant.now().minus(ManagementFactory.getRuntimeMXBean().getUptime(), ChronoUnit.MILLIS))) {
					request.setHeader(HttpHeaders.IF_MODIFIED_SINCE, DateTimeFormatter.RFC_1123_DATE_TIME.format(lastScrape.atZone(ZoneId.of("GMT"))));
				}
			}
			request.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.75 Safari/537.36");
			try (var response = client.execute(request)) {
				if (response.getStatusLine().getStatusCode() == 304) {
					if (lastScrape == null) {
						logger.info("{} Feed {} not modified", feed.getOrigin(), feed.getTitle());
					} else {
						logger.info("{} Feed {} not modified since {}", feed.getOrigin(), feed.getTitle(), lastScrape);
					}
					return;
				}
				try (var stream = response.getEntity().getContent()) {
					if (!config.isDisableEtag()) {
						var etag = response.getFirstHeader(HttpHeaders.ETAG);
						if (etag != null && (config.getEtag() == null || !config.getEtag().equals(etag.getValue()))) {
							config.setEtag(etag.getValue());
							feed.setPlugin(scriptTag, config);
							ingest.update(feed.getOrigin(), feed);
						} else if (etag == null && config.getEtag() != null) {
							config.setEtag(null);
							feed.setPlugin(scriptTag, config);
							ingest.update(feed.getOrigin(), feed);
						}
					}
					var syndFeed = new SyndFeedInput().build(new XmlReader(stream));
					if (syndFeed.getImage() != null) {
						var image = syndFeed.getImage().getUrl();
						cacheLater(image, feed.getOrigin());
						if (!feed.hasTag("plugin/thumbnail")) {
							feed.setPlugin("plugin/thumbnail", Thumbnail.builder().url(image).build());
							ingest.update(feed.getOrigin(), feed);
						}
					}
					for (var entry : syndFeed.getEntries().reversed()) {
						try {
							var link = entryLink(feed, config, entry);
							if (refRepository.existsByUrlAndOrigin(link, feed.getOrigin())) {
								logger.debug("{} Skipping RSS entry in feed {} which already exists. {} {}",
									feed.getOrigin(), feed.getTitle(), entry.getTitle(), entry.getLink());
								continue;
							}
							var ref = parseEntry(feed, config, link, entry, config.getDefaultThumbnail());
							ref.setOrigin(feed.getOrigin());
							if (ref.getPublished().isBefore(feed.getPublished())) {
								logger.warn("{} RSS entry in feed {} which was published before feed publish date. {} {}",
									feed.getOrigin(), feed.getTitle(), ref.getTitle(), ref.getUrl());
								feed.setPublished(ref.getPublished().minus(1, ChronoUnit.DAYS));
								ingest.update(feed.getOrigin(), feed);
							}
							jasperClient.refPush(URI.create(api), authorHeaders(feed), feed.getOrigin(), List.of(mapper.domainToDto(ref)));
						} catch (NotFoundException e) {
							logger.debug("{} Skipping RSS entry in feed {} which failed matching conditions. {} {}",
								feed.getOrigin(), feed.getTitle(), entry.getTitle(), entry.getLink());
						} catch (FeignException.Forbidden | FeignException.Unauthorized e) {
							logger.warn("{} Feed scrape blocked: author not authorized. {}", feed.getOrigin(), feed.getUrl());
							tagger.attachError(feed.getUrl(), feed.getOrigin(), "Author not authorized to add tags", e.contentUTF8());
							return; // addTags apply to all entries; if one fails, all will fail
						} catch (Exception e) {
							logger.error("{} Error processing entry {}: {}", feed.getOrigin(), feed.getUrl(), entry.getLink());
							tagger.attachLogs(feed.getOrigin(), feed, "Error processing entry " + entry.getLink(), getMessage(e));
						}
					}
				}
			}
		}
	}

	private String entryLink(Ref feed, Feed config, SyndEntry entry) {
		var link = entry.getLink();
		if (entry.getUri() != null && entry.getUri().startsWith(link)) {
			// Atom ID, RSS GUID
			link = entry.getUri();
		}
		if (config.isStripQuery() && link.contains("?")) {
			if (link.contains("#") && !config.isStripHash()) {
				link = link.substring(0, link.indexOf("?")) + link.substring(link.indexOf("#"));
			} else {
				link = link.substring(0, link.indexOf("?"));
			}
		}
		if (config.isStripHash() && link.contains("#")) {
			link = link.substring(0, link.indexOf("#"));
		}
		try {
			new URI(link).toURL();
		} catch (IllegalArgumentException e) {
			try {
				link = new URL(new URI(feed.getUrl()).toURL(), link).toExternalForm();
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return link;
	}

	private Ref parseEntry(Ref feed, Feed config, String link, SyndEntry entry, Thumbnail defaultThumbnail) {
		if (config.getMatchText() != null && !config.getMatchText().isEmpty()) {
			var title = entry.getTitle().toLowerCase();
			if (config.getMatchText().stream().noneMatch(t -> title.contains(t.toLowerCase()))) {
				throw new NotFoundException(entry.getTitle());
			}
		}
		var ref = new Ref();
		if (config.isScrapeWebpage()) {
			ref.addTag("_plugin/delta/scrape/ref");
		}
		ref.setUrl(link);
		ref.setTitle(entry.getTitle());
		ref.setSources(List.of(feed.getUrl()));
		ref.addTags(config.getAddTags());
		if (entry.getPublishedDate() != null) {
			ref.setPublished(entry.getPublishedDate().toInstant());
		} else if (link.contains("arxiv.org")) {
			var publishDate = link.substring(link.lastIndexOf("/") + 1, link.lastIndexOf("/") + 5);
			var publishYear = publishDate.substring(0, 2);
			if (Integer.parseInt("20" + publishYear) > Year.now().getValue() + 10) {
				publishYear = "19" + publishYear;
			} else {
				publishYear = "20" + publishYear;
			}
			var publishMonth = publishDate.substring(2);
			ref.setPublished(Instant.parse(publishYear + "-" + publishMonth + "-01T00:00:00.00Z"));
		} else if (entry.getUpdatedDate() != null) {
			ref.setPublished(entry.getUpdatedDate().toInstant());
		}
		var comment = isNotBlank(ref.getComment()) ? ref.getComment() : "";
		if (config.isScrapeDescription() || config.isScrapeContents()) {
			SyndContent desc = null;
			if (config.isScrapeContents() && !entry.getContents().isEmpty()) {
				desc = getPreferredType(entry.getContents());
			} else if (config.isScrapeDescription() && entry.getDescription() != null) {
				desc = entry.getDescription();
			}
			if (desc != null) {
				comment = desc.getValue();
				if (isHtml(desc)) {
					comment = sanitizer.clean(comment);
				}
			}
		}
		var dc = (DCModule) entry.getModule(DCModule.URI);
		if (config.isScrapeAuthors() && !dc.getCreators().isEmpty()) {
			if (!comment.isBlank()) comment += "\n\n\n\n";
			comment += String.join(", ", dc.getCreators());
		}
		ref.setComment(comment);
		if (config.isScrapeThumbnail()) {
			parseThumbnail(entry, ref);
			if (!ref.hasPlugin("plugin/thumbnail")) {
				if (defaultThumbnail != null && !defaultThumbnail.isBlank()) {
					ref.setPlugin("plugin/thumbnail", defaultThumbnail);
				}
			}
			if (ref.hasPlugin("plugin/thumbnail")) {
				cacheLater(ref.getPlugin("plugin/thumbnail", Thumbnail.class).getUrl(), feed.getOrigin());
			}
		}
		if (config.isScrapeAudio()) {
			parseAudio(entry, ref);
			if (ref.hasPlugin("plugin/audio")) {
				cacheLater(ref.getPlugin("plugin/audio", Audio.class).getUrl(), feed.getOrigin());
			}
		}
		if (config.isScrapeVideo()) {
			parseVideo(entry, ref);
			if (ref.hasPlugin("plugin/video")) {
				cacheLater(ref.getPlugin("plugin/video", Video.class).getUrl(), feed.getOrigin());
			} else {
				parseEmbed(entry, ref);
			}
		}
		return ref;
	}

	private SyndContent getPreferredType(List<SyndContent> contents) {
		for (var c : contents) if (isHtml(c)) return c;
		for (var c : contents) if (isText(c)) return c;
		return contents.get(0);
	}

	private boolean isHtml(SyndContent content) {
		var t = content.getType();
		return t != null && (t.equals("text/html") || t.equals("html"));
	}

	private boolean isText(SyndContent content) {
		var t = content.getType();
		// When used for the description of an entry, if null 'text/plain' must be assumed.
		return t == null || t.equals("text/plain");
	}

	private void parseThumbnail(SyndEntry entry, Ref ref) {
		if (entry.getEnclosures() != null) {
			for (var e : entry.getEnclosures()) {
				if ("image/jpg".equals(e.getType()) || "image/png".equals(e.getType())) {
					ref.setPlugin("plugin/thumbnail", Thumbnail.builder().url(e.getUrl()).build());
					return;
				}
			}
		}
		var itunes = (ITunes) entry.getModule(ITunes.URI);
		if (itunes != null &&
			itunes.getImageUri() != null) {
			ref.setPlugin("plugin/thumbnail", Map.of("url", itunes.getImageUri()));
			return;
		}
		var media = (MediaEntryModuleImpl) entry.getModule(MediaModule.URI);
		if (media == null) return;
		if (media.getMetadata() != null &&
			media.getMetadata().getThumbnail() != null &&
			media.getMetadata().getThumbnail().length != 0) {
			ref.setPlugin("plugin/thumbnail", Map.of("url", media.getMetadata().getThumbnail()[0].getUrl()));
			return;
		}
		if (media.getMetadata() != null &&
			media.getMediaContents() != null) {
			for (var c : media.getMediaContents()) {
				if ("image".equals(c.getMedium()) || "image/jpeg".equals(c.getType()) || "application/octet-stream".equals(c.getType())) {
					ref.setPlugin("plugin/thumbnail", c.getReference());
					return;
				}
			}
		}
		if (media.getMediaGroups().length == 0) return;
		var group = media.getMediaGroups()[0];
		if (group.getMetadata().getThumbnail().length == 0) return;
		ref.setPlugin("plugin/thumbnail", Map.of("url", group.getMetadata().getThumbnail()[0].getUrl()));
	}

	private void parseAudio(SyndEntry entry, Ref ref) {
		if (entry.getEnclosures() != null) {
			for (var e : entry.getEnclosures()) {
				if (e.getType() == null) continue;
				if (e.getType().startsWith("audio/")) {
					ref.setPlugin("plugin/audio", Map.of("url", e.getUrl()));
					return;
				}
			}
		}
	}

	private void parseVideo(SyndEntry entry, Ref ref) {
		if (entry.getEnclosures() != null) {
			for (var e : entry.getEnclosures()) {
				if ("video/mp4".equals(e.getType())) {
					ref.setPlugin("plugin/video", Map.of("url", e.getUrl()));
					return;
				}
			}
		}
	}

	private void parseEmbed(SyndEntry entry, Ref ref) {
		var youtubeEmbed = entry
			.getForeignMarkup()
			.stream()
			.filter(e -> "videoId".equals(e.getName()))
			.filter(e -> "yt".equals(e.getNamespacePrefix()))
			.findFirst();
		if (youtubeEmbed.isPresent()) {
			ref.setPlugin("plugin/embed", Map.of("url", "https://www.youtube.com/embed/" + youtubeEmbed.get().getValue()));
		}
	}

	private Map<String, Object> authorHeaders(Ref feed) {
		var authors = HasTags.authors(feed);
		return Map.of(
			LOCAL_ORIGIN_HEADER, Objects.toString(feed.getOrigin(), ""),
			USER_TAG_HEADER, authors.isEmpty() ? "" : authors.getFirst(),
			USER_ROLE_HEADER, USER
		);
	}

	private void cacheLater(String url, String origin) {
		if (isBlank(url)) return;
		var ref = refRepository.findOneByUrlAndOrigin(url, origin).orElse(null);
		if (ref != null && (ref.hasTag("_plugin/cache") || ref.hasTag("_plugin/delta/cache"))) return;
		tagger.internalTag(url, origin, "_plugin/delta/cache");
	}
}
