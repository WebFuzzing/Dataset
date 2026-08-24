package jasper.web.rest;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jasper.aop.ClearIdle;
import jasper.domain.Ref;
import jasper.domain.proj.HasOrigin;
import jasper.errors.NotFoundException;
import jasper.service.ProxyService;
import jasper.service.dto.RefDto;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

import static jasper.domain.Ref.URL_LEN;
import static jasper.domain.proj.HasOrigin.ORIGIN_LEN;
import static java.lang.Long.parseLong;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;
import static org.springframework.http.MediaType.parseMediaType;

@ClearIdle
@RestController
@RequestMapping("api/v1/proxy")
@Validated
@Tag(name = "Proxy")
@ApiResponses({
	@ApiResponse(responseCode = "500", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	@ApiResponse(responseCode = "503", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
})
public class ProxyController {

	@Autowired
	ProxyService proxyService;

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping({"prefetch", "prefetch/{filename:.+}"})
	ResponseEntity<String> preFetch(
		@RequestParam @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url,
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestParam(defaultValue = "false") boolean thumbnail,
		@PathVariable(required = false) String filename
	) {
		proxyService.preFetch(url, origin, thumbnail);
		return ResponseEntity.noContent()
			.cacheControl(CacheControl.maxAge(100, TimeUnit.DAYS).cachePrivate())
			.build();
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "206"),
		@ApiResponse(responseCode = "404"),
		@ApiResponse(responseCode = "416", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@GetMapping({"", "{filename:.+}"})
	ResponseEntity<StreamingResponseBody> fetch(
		@RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader,
		@RequestParam @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url,
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestParam(defaultValue = "false") boolean thumbnail,
		@PathVariable(required = false) String filename
	) {
		var is = proxyService.fetch(url, origin, thumbnail);
		if (is == null) throw new NotFoundException(url);
		var ref = proxyService.stat(url, origin, thumbnail);
		var cache = proxyService.cache(url, origin, thumbnail);
		if (isBlank(filename) || filename.equals(url)) {
			filename = "file";
			try {
				filename
					= isNotBlank(getName(new URI(url).getPath())) ? getName(new URI(url).getPath())
					: ref != null && isNotBlank(ref.getTitle()) ? ref.getTitle()
					: filename;
			} catch (URISyntaxException ignored) { }
		}
		var contentLength = cache != null ? cache.getContentLength() : null;
		var contentType = cache != null && isNotBlank(cache.getMimeType())
			? parseMediaType(cache.getMimeType())
			: APPLICATION_OCTET_STREAM;
		var contentDisposition = "inline; filename*=UTF-8''" +
			URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
		if (rangeHeader != null && contentLength != null && rangeHeader.startsWith("bytes=")) {
			try {
				return handleRangeRequest(is, rangeHeader, contentLength, contentType, contentDisposition);
			} catch (NumberFormatException e) {
				// RFC 7233 Section 3.1: Ignore syntactically invalid range headers and return full content
				// Fall through to return full content below
			}
		}
		var responseBuilder = ResponseEntity.ok();
		if (contentLength != null) {
			responseBuilder.header(HttpHeaders.ACCEPT_RANGES, "bytes");
			responseBuilder.contentLength(contentLength);
		} else {
			responseBuilder.header(HttpHeaders.ACCEPT_RANGES, "none");
		}
		return responseBuilder
			.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
			.contentType(contentType)
			.cacheControl(CacheControl.maxAge(100, TimeUnit.DAYS).cachePrivate())
			.body(outputStream -> streamContent(is, outputStream, 0, null));
	}

	private ResponseEntity<StreamingResponseBody> handleRangeRequest(InputStream is, String rangeHeader, long contentLength, MediaType contentType, String contentDisposition) throws NumberFormatException {
		// Parse "bytes=start-end" (end is optional)
		var rangeValue = rangeHeader.substring("bytes=".length());
		var ranges = rangeValue.split("-");
		var start = isBlank(ranges[0])
			? contentLength - parseLong(ranges[1])
			: parseLong(ranges[0]);
		var end = ranges.length > 1 && isNotBlank(ranges[0]) && isNotBlank(ranges[1])
			? parseLong(ranges[1])
			: contentLength - 1;

		// RFC 7233: If end >= contentLength, adjust to contentLength - 1
		if (end >= contentLength) {
			end = contentLength - 1;
		}
		// RFC 7233: If suffix-byte-range-spec exceeds content length, clamp start to 0
		if (start < 0) {
			start = 0;
		}
		// Only return 416 if start is beyond content or start > end after adjustment
		if (start >= contentLength || start > end) {
			try { is.close(); } catch (IOException ignored) { }
			return ResponseEntity.status(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE)
				.header(HttpHeaders.CONTENT_RANGE, "bytes */" + contentLength)
				.build();
		}
		long rangeStart = start;
		long rangeLength = end - start + 1;
		return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
			.header(HttpHeaders.ACCEPT_RANGES, "bytes")
			.header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + contentLength)
			.header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
			.contentLength(rangeLength)
			.contentType(contentType)
			.cacheControl(CacheControl.maxAge(100, TimeUnit.DAYS).cachePrivate())
			.body(outputStream -> streamContent(is, outputStream, rangeStart, rangeLength));
	}

	private void streamContent(InputStream is, OutputStream outputStream, long skip, Long length) throws IOException {
		try (is) {
			if (skip > 0) {
				is.skipNBytes(skip);
			}
			var buffer = new byte[64 * 1024];
			int bytesRead;
			var remaining = length != null ? length : Long.MAX_VALUE;
			while (remaining > 0 && (bytesRead = is.read(buffer, 0, (int) Math.min(buffer.length, remaining))) != -1) {
				outputStream.write(buffer, 0, bytesRead);
				remaining -= bytesRead;
			}
		}
	}

	@ApiResponses({
		@ApiResponse(responseCode = "201"),
	})
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping
	RefDto save(
		@RequestParam(required = false) String title,
		@RequestParam(required = false) String mime,
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		InputStream data
	) throws IOException {
		return proxyService.save(origin, title, data, mime);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@ResponseStatus(HttpStatus.NO_CONTENT)
	@DeleteMapping
	void clearDeleted(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin
	) {
		proxyService.clearDeleted(origin);
	}
}
