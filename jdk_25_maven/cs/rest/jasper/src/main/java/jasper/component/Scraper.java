package jasper.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mdimension.jchronic.Chronic;
import com.mdimension.jchronic.Options;
import com.mdimension.jchronic.tags.Pointer;
import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityManager;
import jasper.component.dto.JsonLd;
import jasper.domain.Ref;
import jasper.plugin.Scrape;
import jasper.plugin.Video;
import jasper.repository.RefRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static jasper.domain.Ref.from;
import static jasper.domain.proj.HasTags.hasMedia;
import static jasper.util.Logging.getMessage;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Profile("proxy | file-cache")
@Component
public class Scraper {
	private static final Logger logger = LoggerFactory.getLogger(Scraper.class);

	@Autowired
	ConfigCache configs;

	@Autowired
	Tagger tagger;

	@Autowired
	Proxy proxy;

	@Autowired
	Sanitizer sanitizer;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	RefRepository refRepository;

	@Autowired
	EntityManager em;

	@Timed(value = "jasper.scrape", histogram = true)
	public Scrape getConfig(String url, String origin) {
		var providers = configs.getAllConfigs(origin, "+plugin/scrape", Scrape.class);
		for (var c : providers) {
			if (c.getSchemes() == null) continue;
			for (var s : c.getSchemes()) {
				var regex = Pattern.quote(s).replace("*", "\\E.*\\Q");
				if (url.matches(regex)) return c;
			}
		}
		return configs.getConfig("config:scrape-catchall", origin, "+plugin/scrape", Scrape.class);
	}

	@Timed(value = "jasper.scrape")
	public String rss(String url) throws IOException {
		var data = proxy.fetchString(url);
		if (!data.trim().startsWith("<")) return null;
		var doc = Jsoup.parse(data, url);
		// TODO: support application/feed+json
		return doc.getElementsByTag("link").stream()
			.filter(t -> t.attr("type").equals("application/rss+xml") || t.attr("type").equals("application/atom+xml"))
			.filter(t -> t.hasAttr("href"))
			.map(t -> t.absUrl("href"))
			.findFirst().orElse(null);
	}

	@Timed(value = "jasper.scrape")
	public Ref web(String url, String origin) throws IOException, URISyntaxException {
		var config = getConfig(url, origin);
		if (config == null) return null;
		var data = proxy.fetchString(url, origin, refRepository.existsByUrlAndOrigin(url, origin));
		if (isBlank(data) || !data.trim().startsWith("<")) return from(url, origin);
		var result = refRepository.findOneByUrlAndOrigin(url, origin).orElse(from(url, origin));
		// Update ref but don't persist changes
		em.detach(result);
		var doc = Jsoup.parse(data, url);
		result.setTitle(doc.title());
		fixImages(doc, config);
		parseImages(result, doc, config);
		parseThumbnails(result, doc, config);
		parsePublished(result, doc, config);
		removeSelectors(doc, config);
		removeStyleSelectors(doc, config);
		parseVideos(result, doc, config);
		for (var v : doc.select("video")) {
			if (v.select("source").isEmpty()) v.remove();
		}
		parseAudio(result, doc, config);
		for (var a : doc.select("audio")) {
			if (a.select("source").isEmpty()) a.remove();
		}
		parseOpenGraph(result, doc, config);
		parseOembed(result, doc, config);
		parseLinkedData(result, doc, config);
		parseText(result, doc, config);
		return result;
	}

	private void fixImages(Document doc, Scrape config) {
		var images = doc.select("img");
		for (var image : images) {
			var src = image.absUrl("src");
			var dataSrc = image.absUrl("data-src");
			if (isNotBlank(dataSrc)) {
				image.attr("src", src = dataSrc);
			}
			var dataSrcset = image.absUrl("data-srcset");
			if (isNotBlank(dataSrcset)) {
				image.attr("src", src = getImage(dataSrcset.split(",")[0]));
			}
			if (config.getImageFixRegex() == null) continue;
			for (var query : config.getImageFixRegex()) {
				if (src.matches(query)) {
					image.attr("src", src.replaceAll(query, ""));
					break;
				}
			}
		}
	}

	private void parseImages(Ref result, Document doc, Scrape config) {
		if (config.getImageSelectors() == null) return;
		for (var s : config.getImageSelectors()) {
			var image = doc.select(s).first();
			if (image == null) continue;
			if (image.tagName().equals("a")) {
				var src = image.absUrl("href");
				cacheLater(src, result.getOrigin());
				addPluginUrl(result, "plugin/image", getImage(src));
				addThumbnailUrl(result, getThumbnail(src));
			} else if (image.hasAttr("data-srcset")){
				var srcset = image.absUrl("data-srcset").split(",");
				var src = srcset[srcset.length - 1].split(" ")[0];
				cacheLater(src, result.getOrigin());
				addPluginUrl(result, "plugin/image", getImage(src));
				addThumbnailUrl(result, getThumbnail(src));
				image.parent().remove();
			} else if (image.hasAttr("srcset")){
				var srcset = image.absUrl("srcset").split(",");
				var src = srcset[srcset.length - 1].split(" ")[0];
				cacheLater(src, result.getOrigin());
				addPluginUrl(result, "plugin/image", getImage(src));
				addThumbnailUrl(result, getThumbnail(src));
				image.parent().remove();
			} else if (image.hasAttr("src")){
				var src = image.absUrl("src");
				cacheLater(src, result.getOrigin());
				addPluginUrl(result, "plugin/image", getImage(src));
				addThumbnailUrl(result, getThumbnail(src));
				image.parent().remove();
			}
		}
	}

	private void parseThumbnails(Ref result, Document doc, Scrape config) {
		if (config.getThumbnailSelectors() == null) return;
		for (var s : config.getThumbnailSelectors()) {
			for (var thumbnail : doc.select(s)) {
				if (thumbnail.tagName().equals("svg")) {
					addThumbnailUrl(result, svgToUrl(sanitizer.clean(thumbnail.outerHtml(), result.getUrl())));
				} else if (thumbnail.hasAttr("href")) {
					var src = thumbnail.absUrl("href");
					cacheLater(src, result.getOrigin());
					addThumbnailUrl(result, getThumbnail(src));
				} else if (thumbnail.hasAttr("data-srcset")){
					var srcset = thumbnail.absUrl("data-srcset").split(",");
					var src = srcset[srcset.length - 1].split(" ")[0];
					cacheLater(src, result.getOrigin());
					addThumbnailUrl(result, getThumbnail(src));
				} else if (thumbnail.hasAttr("srcset")){
					var srcset = thumbnail.absUrl("srcset").split(",");
					var src = srcset[srcset.length - 1].split(" ")[0];
					cacheLater(src, result.getOrigin());
					addThumbnailUrl(result, getThumbnail(src));
				} else if (thumbnail.hasAttr("src")){
					var src = thumbnail.absUrl("src");
					cacheLater(src, result.getOrigin());
					addThumbnailUrl(result, getThumbnail(src));
				}
				thumbnail.parent().remove();
			}
		}
	}

	private static Options opts = new Options(Pointer.PointerType.PAST);;
	private void parsePublished(Ref result, Document doc, Scrape config) {
		if (config.getPublishedSelectors() == null) return;
		for (var s : config.getPublishedSelectors()) {
			var published = doc.select(s).first();
			if (published == null) continue;
			String date = "";
			if (published.tagName().equals("time")) {
				date = published.attr("datetime");
			} else {
				result.setPublished(Instant.ofEpochSecond(Chronic.parse(published.text(), opts).getBegin()));
				return;
			}
			if (isBlank(date)) continue;
			try {
				result.setPublished(parseDate(date));
				return;
			} catch (DateTimeParseException ignored) {}
		}
	}

	private void removeSelectors(Document doc, Scrape config) {
		if (config.getRemoveSelectors() == null) return;
		for (var r : config.getRemoveSelectors()) doc.select(r).remove();
	}

	private void removeStyleSelectors(Document doc, Scrape config) {
		if (config.getRemoveStyleSelectors() == null) return;
		for (var r : config.getRemoveStyleSelectors()) doc.select(r).removeAttr("style");
	}

	private void parseVideos(Ref result, Document doc, Scrape config) {
		if (config.getVideoSelectors() == null) return;
		for (var s : config.getVideoSelectors()) {
			for (var video : doc.select(s)) {
				if (video.tagName().equals("div")) {
					var src = video.absUrl("data-stream");
					cacheLater(src, result.getOrigin());
					addVideoUrl(result, getVideo(src));
				} else if (video.hasAttr("src")) {
					var src = video.absUrl("src");
					cacheLater(src, result.getOrigin());
					addVideoUrl(result, getVideo(src));
					addWeakThumbnail(result, getThumbnail(src));
					video.parent().remove();
				}
			}
		}
	}

	private void parseAudio(Ref result, Document doc, Scrape config) {
		if (config.getAudioSelectors() == null) return;
		for (var s : config.getAudioSelectors()) {
			for (var audio : doc.select(s)) {
				if (audio.hasAttr("src")) {
					var src = audio.absUrl("src");
					cacheLater(src, result.getOrigin());
					addPluginUrl(result, "plugin/audio", getVideo(src));
					audio.parent().remove();
				}
			}
		}
	}

	private void parseOpenGraph(Ref result, Document doc, Scrape config) {
		if (!config.isOpenGraph()) return;
		for (var metaAudio : doc.select("meta[property=og:audio]")) {
			if (isBlank(metaAudio.attr("content"))) continue;
			addPluginUrl(result, "plugin/audio", metaAudio.absUrl("content"));
		}
		for (var metaVideo : doc.select("meta[property=og:video]")) {
			var videoUrl = metaVideo.absUrl("content");
			if (isBlank(videoUrl)) continue;
			// TODO: video filetypes
			if (videoUrl.endsWith(".m3u8") || videoUrl.endsWith(".mp4")) {
				addVideoUrl(result, videoUrl);
			} else {
				addPluginUrl(result, "plugin/embed", videoUrl);
			}
		}
		for (var metaImage : doc.select("meta[property=og:image]")) {
			if (isBlank(metaImage.attr("content"))) continue;
			// TODO: In some cases load plugin/image
			addThumbnailUrl(result, metaImage.absUrl("content"));
		}
		var metaTitle = doc.select("meta[property=og:title]").first();
		if (metaTitle != null && isNotBlank(metaTitle.attr("content"))) {
			result.setTitle(metaTitle.attr("content"));
		}
		var metaPublished = doc.select("meta[property=article:published_time]").first();
		if (metaPublished != null && isNotBlank(metaPublished.attr("content"))) {
			result.setPublished(parseDate(metaPublished.attr("content")));
		}
		metaPublished = doc.select("meta[property=og:article:published_time]").first();
		if (metaPublished != null && isNotBlank(metaPublished.attr("content"))) {
			result.setPublished(parseDate(metaPublished.attr("content")));
		}
		var metaReleased = doc.select("meta[property=og:book:release_date]").first();
		if (metaReleased != null && isNotBlank(metaReleased.attr("content"))) {
			result.setPublished(parseDate(metaReleased.attr("content")));
		}
	}

	private void parseOembed(Ref result, Document doc, Scrape config) {
		if (!config.isOembedJson()) return;
		var oembed = doc.select("link[type=application/json+oembed]").first();
		if (oembed != null) {
			var oembedUrl = oembed.absUrl("href");
			// TODO: embedded oembed
		}
	}

	private void parseLinkedData(Ref result, Document doc, Scrape config) {
		if (!config.isLdJson());
		var jsonlds = doc.select("script[type=application/ld+json]");
		for (var jsonld : jsonlds) {
			var json = jsonld.html().trim().replaceAll("\n", " ");
			try {
				var configs = json.startsWith("{") ?
					List.of(objectMapper.readValue(json, JsonLd.class)) :
					objectMapper.readValue(json, new TypeReference<List<JsonLd>>(){});
				for (var c : configs) parseLd(result, c);
			} catch (Exception e) {
				logger.warn("Invalid LD+JSON. {}", getMessage(e));
				logger.debug(json);
			}
		}
	}

	private void parseText(Ref result, Document doc, Scrape config) {
		if (!config.isText()) return;
		if (config.getTextSelectors() != null) {
			for (var s : config.getTextSelectors()) {
				var el = doc.body().select(s).first();
				if (el != null) {
					for (var r : config.getRemoveAfterSelectors()) el.select(r).remove();
					result.setComment(sanitizer.clean(el.html(), result.getUrl()));
					return;
				}
			}
		}
		result.setComment(doc.body()
			.wholeText()
			.trim()
			.replaceAll("\t", "")
			.replaceAll("[\n\r]", "\n\n"));
	}

	private void addWeakThumbnail(Ref ref, String url) {
		if (!ref.hasTag("plugin/thumbnail")) addThumbnailUrl(ref, url);
	}

	private void addThumbnailUrl(Ref ref, String url) {
		if (url.endsWith(".com") || url.endsWith(".m3u8")) return;
		// TODO: Fallback if image can't load
		addPluginUrl(ref, "plugin/thumbnail", url);
	}

	private void addVideoUrl(Ref ref, String url) {
		if (isBlank(url)) return;
		if (ref.hasTag("plugin/video") && url.endsWith(".m3u8")
			&& !ref.getPlugin("plugin/video", Video.class).getUrl().endsWith(".m3u8")) return;
		addPluginUrl(ref, "plugin/video", url);
	}

	private void parseLd(Ref result, JsonLd ld) {
		if (isNotBlank(ld.getThumbnailUrl())) addWeakThumbnail(result, ld.getThumbnailUrl());
		if ("NewsArticle".equals(ld.getType())) {
			if (isNotBlank(ld.getDatePublished())) {
				result.setPublished(parseDate(ld.getDatePublished()));
			}
		}
		if ("AudioObject".equals(ld.getType())) {
			if (isNotBlank(ld.getEmbedUrl())) {
				addPluginUrl(result, "plugin/embed", ld.getEmbedUrl());
			} else if (isNotBlank(ld.getUrl())) {
				addPluginUrl(result, "plugin/audio", ld.getUrl());
			}
		}
		if (("VideoObject".equals(ld.getType())) || "http://schema.org/VideoObject".equals(ld.getType())) {
			if (isNotBlank(ld.getContentUrl())) {
				addVideoUrl(result, ld.getContentUrl());
				if (isBlank(ld.getThumbnailUrl())) addThumbnailUrl(result, ld.getContentUrl());
			} else if (isNotBlank(ld.getEmbedUrl())) {
				addPluginUrl(result, "plugin/embed", ld.getEmbedUrl());
			}
		}
		if ("SocialMediaPosting".equals(ld.getType())) {
		}
		if ("ImageObject".equals(ld.getType())) {
			if (isNotBlank(ld.getEmbedUrl())) {
				addPluginUrl(result, "plugin/embed", ld.getEmbedUrl());
			} else if (isNotBlank(ld.getContentUrl())) {
				if (isBlank(ld.getThumbnailUrl())) {
					addThumbnailUrl(result, ld.getContentUrl());
				} else if (!hasMedia(result)) {
					addPluginUrl(result, "plugin/image", ld.getContentUrl());
				}
			}
		}
		if (ld.getPublisher() != null) {
			for (var p : ld.getPublisher()) {
				if (p.isTextual()) continue;
				var pub = objectMapper.convertValue(p, JsonLd.class);
				if (pub.getLogo() != null) {
					for (var icon : pub.getLogo().isArray() ? pub.getLogo() : List.of(pub.getLogo())) {
						if (icon.isObject()) {
							if (icon.has("url") && isNotBlank(icon.get("url").asText())) addWeakThumbnail(result, icon.get("url").asText());
						} if (isNotBlank(icon.asText())) {
							addWeakThumbnail(result, icon.asText());
						}
					}
				}
			}
		}
		if (ld.getImage() != null) {
			for (var image : ld.getImage()) {
				if (image.isTextual()) {
					addPluginUrl(result, "plugin/image", image.textValue());
				} else {
					parseLd(result, objectMapper.convertValue(image, JsonLd.class));
				}
			}
		}
		if (ld.getVideo() != null) {
			for (var video : ld.getVideo()) {
				if (video.isTextual()) {
					addPluginUrl(result, "plugin/video", video.textValue());
				} else {
					parseLd(result, objectMapper.convertValue(video, JsonLd.class));
				}
			}
		}
	}

	private String getVideo(String src) {
		return src;
	}

	private String getImage(String src) {
		if (src.contains("/full/max/0/")) return src.replace("/full/max/0/", "/full/!1920,1080/0/");
		if (src.contains("?resize")) return src.substring(0, src.indexOf("?resize"));
		return src;
	}

	private String getThumbnail(String src) {
		if (src.contains("/full/max/0/")) return src.replace("/full/max/0/", "/full/!300,200/0/");
		return src;
	}

	private String svgToUrl(String svg) {
		return "data:image/svg+xml," + svg
			.replaceAll("<svg", svg.contains("xmlns") ? "<svg" : "<svg xmlns='http://www.w3.org/2000/svg'")
			.replaceAll("viewbox", "viewBox")
			.replaceAll("\"", "'")
			.replaceAll("%", "%25")
			.replaceAll("#", "%23")
			.replaceAll("\\{", "%7B")
			.replaceAll("}", "%7D")
			.replaceAll("<", "%3C")
			.replaceAll(">", "%3E")
			.replaceAll("\s+"," ");
	}

	private void addPluginUrl(Ref ref, String tag, String url) {
		if (!"plugin/embed".equals(tag)) cacheLater(url, ref.getOrigin());
		ref.setPlugin(tag, Map.of("url", url));
	}

	private void cacheLater(String url, String origin) {
		if (isBlank(url)) return;
		url = fixUrl(url);
		var ref = refRepository.findOneByUrlAndOrigin(url, origin).orElse(null);
		if (ref != null && (ref.hasTag("_plugin/cache") || ref.hasTag("_plugin/delta/cache"))) return;
		tagger.internalTag(url, origin, "_plugin/delta/cache");
	}

	private String fixUrl(String url) {
		// TODO: Add plugin to override like oembeds
//		return url.replaceAll("%20", "+");
		return url.replaceAll(" ", "%20");
	}

	private Instant parseDate(String date) {
		try {
			return Instant.parse(date);
		} catch (Exception e) {
			// TODO: support other date formats
			return null;
		}
	}

}
