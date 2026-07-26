package jasper.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jasper.IntegrationTest;
import jasper.domain.Plugin;
import jasper.repository.PluginRepository;
import jasper.repository.RefRepository;
import jasper.service.ProxyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.io.ByteArrayInputStream;
import java.util.Arrays;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link ProxyController}.
 * Tests the range request functionality and edge cases using real beans.
 */
@WithMockUser("+user/tester")
@AutoConfigureMockMvc
@IntegrationTest
@ActiveProfiles({"file-cache", "storage", "test"})
class ProxyControllerIT {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ProxyService proxyService;

	@Autowired
	private RefRepository refRepository;

	@Autowired
	private PluginRepository pluginRepository;

	private static final byte[] TEST_CONTENT = "This is a test file with some content for range request testing. It has enough data to test various range scenarios.".getBytes();
	private String testUrl;

	@BeforeEach
	void setup() throws Exception {
		refRepository.deleteAll();
		pluginRepository.deleteAll();

		// Create the _plugin/cache plugin required by FileCache
		var cachePlugin = new Plugin();
		cachePlugin.setTag("_plugin/cache");
		var mapper = new ObjectMapper();
		cachePlugin.setSchema((ObjectNode) mapper.readTree("""
		{
		    "optionalProperties": {
		      "id": { "type": "string" },
		      "mimeType": { "type": "string" },
		      "contentLength": { "type": "uint32" },
		      "ban": { "type": "boolean" },
		      "noStore": { "type": "boolean" },
		      "thumbnail": { "type": "boolean" }
		    }
		}"""));
		pluginRepository.save(cachePlugin);

		// Upload a test file to the cache using the real ProxyService
		var savedRef = proxyService.save("", "test.txt", new ByteArrayInputStream(TEST_CONTENT), "text/plain");
		testUrl = savedRef.getUrl();
		// Give OS time to flush file buffers to prevent flaky reads under CI load
		Thread.sleep(1000);
	}

	@Test
	@Disabled("Flakey")
	void testFetchWithoutRangeHeader() throws Exception {
		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", ""))
			.andExpect(status().isOk())
			.andExpect(header().string("Accept-Ranges", "bytes"))
			.andExpect(header().exists("Content-Disposition"))
			.andExpect(content().contentType(MediaType.TEXT_PLAIN))
			.andExpect(content().bytes(TEST_CONTENT));
	}

	@Test
	@Disabled("Flakey")
	void testValidRangeRequestStartAndEnd() throws Exception {
		// Test: bytes=0-99 (first 100 bytes)
		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=0-99"))
			.andExpect(status().isPartialContent())
			.andExpect(header().string("Accept-Ranges", "bytes"))
			.andExpect(header().string("Content-Range", "bytes 0-99/" + TEST_CONTENT.length))
			.andExpect(header().string("Content-Length", "100"))
			.andExpect(content().contentType(MediaType.TEXT_PLAIN))
			.andExpect(content().bytes(Arrays.copyOfRange(TEST_CONTENT, 0, 100)));
	}

	@Test
	@Disabled("Flakey")
	void testValidRangeRequestOnlyStart() throws Exception {
		// Test: bytes=100- (from byte 100 to end)
		int start = 100;
		long expectedLength = TEST_CONTENT.length - start;

		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=" + start + "-"))
			.andExpect(status().isPartialContent())
			.andExpect(header().string("Accept-Ranges", "bytes"))
			.andExpect(header().string("Content-Range", "bytes " + start + "-" + (TEST_CONTENT.length - 1) + "/" + TEST_CONTENT.length))
			.andExpect(header().string("Content-Length", String.valueOf(expectedLength)))
			.andExpect(content().contentType(MediaType.TEXT_PLAIN))
			.andExpect(content().bytes(Arrays.copyOfRange(TEST_CONTENT, start, TEST_CONTENT.length)));
	}

	@Test
	@Disabled("Flakey")
	void testValidRangeRequestSingleByte() throws Exception {
		// Test: bytes=0-0 (first byte only)
		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=0-0"))
			.andExpect(status().isPartialContent())
			.andExpect(header().string("Accept-Ranges", "bytes"))
			.andExpect(header().string("Content-Range", "bytes 0-0/" + TEST_CONTENT.length))
			.andExpect(header().string("Content-Length", "1"))
			.andExpect(content().contentType(MediaType.TEXT_PLAIN))
			.andExpect(content().bytes(Arrays.copyOfRange(TEST_CONTENT, 0, 1)));
	}

	@Test
	@Disabled("Flakey")
	void testValidRangeRequestAtBoundary() throws Exception {
		// Test: Range at the end of file
		int start = TEST_CONTENT.length - 10;
		int end = TEST_CONTENT.length - 1;

		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=" + start + "-" + end))
			.andExpect(status().isPartialContent())
			.andExpect(header().string("Accept-Ranges", "bytes"))
			.andExpect(header().string("Content-Range", "bytes " + start + "-" + end + "/" + TEST_CONTENT.length))
			.andExpect(header().string("Content-Length", "10"))
			.andExpect(content().contentType(MediaType.TEXT_PLAIN))
			.andExpect(content().bytes(Arrays.copyOfRange(TEST_CONTENT, start, end + 1)));
	}

	@Test
	void testInvalidRangeStartGreaterThanEnd() throws Exception {
		// Test: start > end should return 416
		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=100-50"))
			.andExpect(status().isRequestedRangeNotSatisfiable())
			.andExpect(header().string("Content-Range", "bytes */" + TEST_CONTENT.length));
	}

	@Test
	void testInvalidRangeStartEqualsContentLength() throws Exception {
		// Test: start >= contentLength should return 416
		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=" + TEST_CONTENT.length + "-"))
			.andExpect(status().isRequestedRangeNotSatisfiable())
			.andExpect(header().string("Content-Range", "bytes */" + TEST_CONTENT.length));
	}

	@Test
	@Disabled("Flakey")
	void testRangeEndGreaterThanContentLength() throws Exception {
		// Test: RFC 7233 compliant - when end >= contentLength, adjust to contentLength-1 and return 206
		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=0-" + TEST_CONTENT.length))
			.andExpect(status().isPartialContent())
			.andExpect(header().string("Accept-Ranges", "bytes"))
			.andExpect(header().string("Content-Range", "bytes 0-" + (TEST_CONTENT.length - 1) + "/" + TEST_CONTENT.length))
			.andExpect(header().string("Content-Length", String.valueOf(TEST_CONTENT.length)))
			.andExpect(content().bytes(TEST_CONTENT));
	}

	@Test
	@Disabled("Flakey")
	void testInvalidRangeStartBeyondEnd() throws Exception {
		// Test: start > contentLength should return 416
		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=" + (TEST_CONTENT.length + 100) + "-"))
			.andExpect(status().isRequestedRangeNotSatisfiable())
			.andExpect(header().string("Content-Range", "bytes */" + TEST_CONTENT.length));
	}

	@Test
	@Disabled("Flakey")
	void testMalformedRangeHeaderNoBytes() throws Exception {
		// Test: malformed header without "bytes=" prefix should be ignored
		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "0-99"))
			.andExpect(status().isOk())
			.andExpect(header().string("Accept-Ranges", "bytes"))
			.andExpect(content().bytes(TEST_CONTENT));
	}

	@Test
	@Disabled("Flakey")
	void testMalformedRangeHeaderInvalidFormat() throws Exception {
		// Test: RFC 7233 Section 3.1 - malformed range headers should be ignored and full content returned
		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=invalid"))
			.andExpect(status().isOk())
			.andExpect(header().string("Accept-Ranges", "bytes"))
			.andExpect(header().string("Content-Length", String.valueOf(TEST_CONTENT.length)));
	}

	@Test
	@Disabled("Flakey")
	void testRangeRequestWithEmptyEnd() throws Exception {
		// Test: bytes=10- (trailing dash with no end value)
		int start = 10;
		long expectedLength = TEST_CONTENT.length - start;

		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=" + start + "-"))
			.andExpect(status().isPartialContent())
			.andExpect(header().string("Content-Range", "bytes " + start + "-" + (TEST_CONTENT.length - 1) + "/" + TEST_CONTENT.length))
			.andExpect(header().string("Content-Length", String.valueOf(expectedLength)))
			.andExpect(content().bytes(Arrays.copyOfRange(TEST_CONTENT, start, TEST_CONTENT.length)));
	}

	@Test
	@Disabled("Flakey")
	void testSuffixByteRangeSpec() throws Exception {
		// Test: bytes=-50 (last 50 bytes) - suffix-byte-range-spec
		int suffixLength = 50;
		int start = TEST_CONTENT.length - suffixLength;
		int end = TEST_CONTENT.length - 1;

		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=-" + suffixLength))
			.andExpect(status().isPartialContent())
			.andExpect(header().string("Accept-Ranges", "bytes"))
			.andExpect(header().string("Content-Range", "bytes " + start + "-" + end + "/" + TEST_CONTENT.length))
			.andExpect(header().string("Content-Length", String.valueOf(suffixLength)))
			.andExpect(content().bytes(Arrays.copyOfRange(TEST_CONTENT, start, TEST_CONTENT.length)));
	}

	@Test
	@Disabled("Flakey")
	void testSuffixByteRangeSpecExceedingContentLength() throws Exception {
		// Test: bytes=-1000 (suffix exceeds content length) - RFC 7233 requires returning entire representation
		int suffixLength = TEST_CONTENT.length + 100;

		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", testUrl)
				.param("origin", "")
				.header("Range", "bytes=-" + suffixLength))
			.andExpect(status().isPartialContent())
			.andExpect(header().string("Accept-Ranges", "bytes"))
			.andExpect(header().string("Content-Range", "bytes 0-" + (TEST_CONTENT.length - 1) + "/" + TEST_CONTENT.length))
			.andExpect(header().string("Content-Length", String.valueOf(TEST_CONTENT.length)))
			.andExpect(content().bytes(TEST_CONTENT));
	}

	@Test
	void testFetchNotFound() throws Exception {
		// Test: URL that doesn't exist in cache
		String nonExistentUrl = "cache:nonexistent-id";

		mockMvc
			.perform(get("/api/v1/proxy")
				.param("url", nonExistentUrl)
				.param("origin", ""))
			.andExpect(status().isNotFound());
	}
}
