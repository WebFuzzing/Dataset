package jasper.component;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import jasper.errors.NotFoundException;
import jasper.errors.ScrapeProtocolException;
import jasper.security.HostCheck;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import static jasper.domain.proj.HasTags.hasMatchingTag;
import static jasper.plugin.Pull.getPull;
import static org.apache.http.util.EntityUtils.consumeQuietly;

@Profile("proxy")
@Component
public class FetchImplHttp implements Fetch {
	private static final Logger logger = LoggerFactory.getLogger(FetchImplHttp.class);

	@Autowired
	HostCheck hostCheck;

	@Autowired
	ConfigCache configs;

	@Autowired
	HttpClientFactory httpClientFactory;

	@Autowired
	Replicator replicator;

	@Bulkhead(name = "fetch")
	public FileRequest doScrape(String url, String origin) throws IOException {
		var remote = configs.getRemote(origin);
		var pull = getPull(remote);
		if (url.startsWith("cache:") || pull.isCacheProxy()) {
			if (hasMatchingTag(remote, "+plugin/error")) return null;
			if (remote == null) {
				logger.warn("{} Can't find remote for cache {}", origin, url);
				return null;
			}
			return replicator.fetch(url, remote);
		}
		if (url.startsWith("http:") || url.startsWith("https:")) {
			return wrap(doWebScrape(url));
		}
		throw new ScrapeProtocolException(url.contains(":") ? url.substring(0, url.indexOf(":")) : "unknown");
	}

	private CloseableHttpResponse doWebScrape(String url) throws IOException {
		logger.debug("Starting request to {}", url);
		HttpUriRequest request = new HttpGet(url);
		if (!hostCheck.validHost(request.getURI())) {
			logger.info("Invalid host {}", request.getURI().getHost());
			throw new NotFoundException("Invalid host.");
		}
		request.setHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/100.0.4896.75 Safari/537.36");
		var http = httpClientFactory.getClient();
		var res = http.execute(request);
		if (res == null) return null;
		if (res.getStatusLine().getStatusCode() == 301 || res.getStatusLine().getStatusCode() == 304) {
			try {
				var location = res.getFirstHeader("Location").getElements()[0].getValue();
				logger.debug("Forwarding request to {} -> {}", url, location);
				return doWebScrape(location);
			} catch (Exception e) {
				logger.error("Error forwarding request from {}", url, e);
				return null;
			} finally {
				res.close();
			}
		}
		logger.debug("Request completed {}", url);
		return res;
	}

	private FileRequest wrap(CloseableHttpResponse res) {
		if (res == null) return null;
		return new FileRequest() {
			@Override
			public String getMimeType() {
				var header = res.getFirstHeader(HttpHeaders.CONTENT_TYPE);
				return header == null ? null : header.getValue();
			}

			@Override
			public InputStream getInputStream() throws IOException {
				return new FilterInputStream(res.getEntity().getContent()) {
					@Override
					public void close() throws IOException {
						try {
							super.close();
						} finally {
							res.close();
						}
					}
				};
			}

			@Override
			public void close() throws IOException {
				consumeQuietly(res.getEntity());
				res.close();
			}
		};
	}

}
