package jasper.component.script;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.FeignException;
import jasper.client.JasperClient;
import jasper.client.dto.JasperMapper;
import jasper.component.HttpClientFactory;
import jasper.component.Ingest;
import jasper.component.Tagger;
import jasper.config.JacksonConfiguration;
import jasper.domain.Ref;
import jasper.plugin.Feed;
import jasper.repository.RefRepository;
import jasper.security.HostCheck;
import jasper.service.dto.RefReplDto;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jasper.security.Auth.LOCAL_ORIGIN_HEADER;
import static jasper.security.Auth.USER_TAG_HEADER;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class RssParserTest {

	@InjectMocks
	RssParser rssParser;

	@Mock
	JasperClient jasperClient;

	@Mock
	JasperMapper mapper;

	@Mock
	Tagger tagger;

	@Mock
	HostCheck hostCheck;

	@Mock
	HttpClientFactory httpClientFactory;

	@Mock
	CloseableHttpClient httpClient;

	@Mock
	CloseableHttpResponse httpResponse;

	@Mock
	HttpEntity httpEntity;

	@Mock
	StatusLine statusLine;

	@Mock
	Ingest ingest;

	@Mock
	RefRepository refRepository;

	static final String FEED_URL = "https://example.com/feed.xml";
	static final String ORIGIN = "";

	static final ObjectMapper om = new ObjectMapper();

	static final String RSS_FEED = """
		<?xml version="1.0" encoding="UTF-8"?>
		<rss version="2.0">
		  <channel>
		    <title>Test Feed</title>
		    <link>https://example.com</link>
		    <description>Test</description>
		    <item>
		      <title>Test Entry</title>
		      <link>https://example.com/entry1</link>
		      <pubDate>Mon, 01 Jan 2024 00:00:00 GMT</pubDate>
		    </item>
		  </channel>
		</rss>
		""";

	@BeforeAll
	static void setUpJackson() {
		ReflectionTestUtils.setField(JacksonConfiguration.class, "om", om);
	}

	@BeforeEach
	void setUp() throws Exception {
		MockitoAnnotations.openMocks(this);
		ReflectionTestUtils.setField(rssParser, "api", "http://localhost:8081");
		when(httpClientFactory.getClient()).thenReturn(httpClient);
		when(mapper.domainToDto(any(Ref.class))).thenReturn(new RefReplDto());
	}

	void setUpValidHttpResponse() throws Exception {
		when(hostCheck.validHost(any(URI.class))).thenReturn(true);
		when(statusLine.getStatusCode()).thenReturn(200);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(httpResponse.getEntity()).thenReturn(httpEntity);
		when(httpResponse.getFirstHeader(anyString())).thenReturn(null);
		when(httpEntity.getContent()).thenAnswer(inv -> new ByteArrayInputStream(RSS_FEED.getBytes()));
		when(httpClient.execute(any(HttpGet.class))).thenReturn(httpResponse);
		when(refRepository.existsByUrlAndOrigin(anyString(), anyString())).thenReturn(false);
	}

	Ref feedRef(String... tags) {
		var ref = new Ref();
		ref.setUrl(FEED_URL);
		ref.setOrigin(ORIGIN);
		ref.setPublished(Instant.EPOCH);
		ref.setTags(new ArrayList<>(List.of(tags)));
		return ref;
	}

	Ref feedWithAddTag(String addTag, String... authorTags) {
		var ref = feedRef(authorTags);
		var feed = new Feed();
		feed.setAddTags(new ArrayList<>(List.of(addTag)));
		ObjectNode plugins = om.createObjectNode();
		plugins.set("plugin/script/feed", om.convertValue(feed, ObjectNode.class));
		ref.setPlugins(plugins);
		return ref;
	}

	@Test
	void testInvalidHost_NeverCallsJasperClient() {
		when(hostCheck.validHost(any(URI.class))).thenReturn(false);
		var feed = feedWithAddTag("_private/tag", "+user/alice");

		rssParser.runScript(feed, "plugin/script/feed");

		verify(jasperClient, never()).refPush(any(), any(), any(), any());
		verify(tagger, never()).attachError(anyString(), anyString(), anyString(), anyString());
	}

	@Test
	void testSuccessfulCreate_NoError() throws Exception {
		setUpValidHttpResponse();
		var feed = feedWithAddTag("science", "+user/alice");

		rssParser.runScript(feed, "plugin/script/feed");

		verify(tagger, never()).attachError(anyString(), anyString(), anyString(), anyString());
	}

	@Test
	@SuppressWarnings("unchecked")
	void testCreateRef_AuthorHeaderIncludesFirstAuthor() throws Exception {
		setUpValidHttpResponse();
		var feed = feedWithAddTag("_private/tag", "+user/alice");

		rssParser.runScript(feed, "plugin/script/feed");

		var captor = ArgumentCaptor.forClass(Map.class);
		verify(jasperClient).refPush(any(URI.class), captor.capture(), anyString(), anyList());
		assertThat(captor.getValue()).containsEntry(USER_TAG_HEADER, "+user/alice");
		assertThat(captor.getValue()).containsEntry(LOCAL_ORIGIN_HEADER, ORIGIN);
	}

	@Test
	@SuppressWarnings("unchecked")
	void testCreateRef_EmptyAuthorHeader_WhenNoAuthor() throws Exception {
		setUpValidHttpResponse();
		var feed = feedWithAddTag("_private/tag" /* no author tags */);

		rssParser.runScript(feed, "plugin/script/feed");

		var captor = ArgumentCaptor.forClass(Map.class);
		verify(jasperClient).refPush(any(URI.class), captor.capture(), anyString(), anyList());
		assertThat(captor.getValue()).containsEntry(USER_TAG_HEADER, "");
	}

	@Test
	void testForbiddenResponse_ErrorAttached() throws Exception {
		setUpValidHttpResponse();
		var feed = feedWithAddTag("_private/tag", "+user/alice");
		var forbiddenEx = mock(FeignException.Forbidden.class);
		when(forbiddenEx.contentUTF8()).thenReturn("Forbidden");
		doThrow(forbiddenEx).when(jasperClient).refPush(any(), any(), any(), any());

		rssParser.runScript(feed, "plugin/script/feed");

		verify(tagger).attachError(eq(FEED_URL), eq(ORIGIN), eq("Author not authorized to add tags"), anyString());
	}

	@Test
	void testAlreadyExists_SilentlySkipped() throws Exception {
		setUpValidHttpResponse();
		var feed = feedWithAddTag("science", "+user/alice");
		when(refRepository.existsByUrlAndOrigin(anyString(), anyString())).thenReturn(true);

		rssParser.runScript(feed, "plugin/script/feed");

		verify(jasperClient, never()).refPush(any(), any(), any(), any());
		verify(tagger, never()).attachError(anyString(), anyString(), anyString(), anyString());
	}
}
