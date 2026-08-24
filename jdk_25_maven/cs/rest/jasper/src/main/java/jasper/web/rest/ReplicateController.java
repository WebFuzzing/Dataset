package jasper.web.rest;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jasper.client.JasperClient;
import jasper.component.ConfigCache;
import jasper.config.Props;
import jasper.domain.Ext;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.domain.Ref_;
import jasper.domain.Template;
import jasper.domain.User;
import jasper.domain.proj.HasOrigin;
import jasper.errors.NotFoundException;
import jasper.errors.TooLargeException;
import jasper.repository.filter.RefFilter;
import jasper.repository.filter.TagFilter;
import jasper.service.ExtService;
import jasper.service.PluginService;
import jasper.service.ProxyService;
import jasper.service.RefService;
import jasper.service.TemplateService;
import jasper.service.UserService;
import jasper.service.dto.DtoMapper;
import jasper.service.dto.ExtDto;
import jasper.service.dto.PluginDto;
import jasper.service.dto.RefReplDto;
import jasper.service.dto.TemplateDto;
import jasper.service.dto.UserDto;
import org.hibernate.validator.constraints.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static jasper.client.JasperClient.jasperHeaders;
import static jasper.domain.Ref.URL_LEN;
import static jasper.domain.proj.HasOrigin.ORIGIN_LEN;
import static jasper.repository.filter.Query.QUERY_LEN;
import static org.apache.commons.io.FilenameUtils.getName;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.domain.Sort.by;

@CrossOrigin
@RestController
@RequestMapping("pub/api/v1/repl")
@Validated
@Tag(name = "Repl")
@ApiResponses({
	@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	@ApiResponse(responseCode = "403", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
})
public class ReplicateController {
	private static final Logger logger = LoggerFactory.getLogger(ReplicateController.class);

	@Autowired
	Props props;

	@Autowired
	ConfigCache configs;

	@Autowired
	JasperClient jasperClient;

	@Autowired
	DtoMapper mapper;

	@Autowired
	RefService refService;

	@Autowired
	ExtService extService;

	@Autowired
	PluginService pluginService;

	@Autowired
	TemplateService templateService;

	@Autowired
	UserService userService;

	@Autowired
	ProxyService proxyService;

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("ref")
	List<RefReplDto> ref(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestParam(required = false) @Length(max = QUERY_LEN) @Pattern(regexp = RefFilter.QUERY) String query,
		@RequestParam(required = false) Instant modifiedAfter,
		@RequestParam(defaultValue = "500") int size
	) {
		if (size > configs.root().getMaxReplEntityBatch()) throw new TooLargeException(size, configs.root().getMaxReplEntityBatch());
		return refService.page(
				RefFilter.builder()
					.origin(origin)
					.query(query)
					.modifiedAfter(modifiedAfter)
					.build(),
				PageRequest.of(0, size, by(Ref_.MODIFIED)))
			.map(mapper::dtoToRepl)
			.getContent();
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("ref/cursor")
	Instant refCursor(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin
	) {
		return refService.cursor(origin);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@PostMapping("ref")
	void refPush(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestBody List<@Valid Ref> refs
	) {
		logger.debug("Receiving push of {} refs", refs.size());
		RuntimeException first = null;
		for (var ref : refs) {
			try {
				ref.setOrigin(origin);
				refService.push(ref);
			} catch (RuntimeException e) {
				// TODO: Ignore auth errors?
				first = first == null ? e : first;
			}
		}
		if (first != null) throw first;
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("ext")
	List<ExtDto> ext(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestParam(required = false) @Length(max = QUERY_LEN) @Pattern(regexp = RefFilter.QUERY) String query,
		@RequestParam(required = false) Instant modifiedAfter,
		@RequestParam(defaultValue = "500") int size
	) {
		if (size > configs.root().getMaxReplEntityBatch()) throw new TooLargeException(size, configs.root().getMaxReplEntityBatch());
		return extService.page(
				TagFilter.builder()
					.origin(origin)
					.query(query)
					.modifiedAfter(modifiedAfter)
					.build(),
				PageRequest.of(0, size, by(Ref_.MODIFIED)))
			.getContent();
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("ext/cursor")
	Instant extCursor(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin
	) {
		return extService.cursor(origin);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@PostMapping("ext")
	void extPush(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestBody List<@Valid Ext> exts
	) {
		logger.debug("Receiving push of {} exts", exts.size());
		RuntimeException first = null;
		for (var ext : exts) {
			try {
				ext.setOrigin(origin);
				extService.push(ext);
			} catch (RuntimeException e) {
				// TODO: Ignore auth errors?
				first = first == null ? e : first;
			}
		}
		if (first != null) throw first;
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("user")
	List<UserDto> user(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestParam(required = false) @Length(max = QUERY_LEN) @Pattern(regexp = RefFilter.QUERY) String query,
		@RequestParam(required = false) Instant modifiedAfter,
		@RequestParam(defaultValue = "500") int size
	) {
		if (size > configs.root().getMaxReplEntityBatch()) throw new TooLargeException(size, configs.root().getMaxReplEntityBatch());
		return userService.page(
				TagFilter.builder()
					.origin(origin)
					.query(query)
					.modifiedAfter(modifiedAfter)
					.build(),
				PageRequest.of(0, size, by(Ref_.MODIFIED)))
			.getContent();
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("user/cursor")
	Instant userCursor(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin
	) {
		return userService.cursor(origin);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@PostMapping("user")
	void userPush(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestBody List<@Valid User> users
	) {
		logger.debug("Receiving push of {} users", users.size());
		RuntimeException first = null;
		for (var user : users) {
			try {
				user.setOrigin(origin);
				userService.push(user);
			} catch (RuntimeException e) {
				// TODO: Ignore auth errors?
				first = first == null ? e : first;
			}
		}
		if (first != null) throw first;
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("plugin")
	List<PluginDto> plugin(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestParam(required = false) @Length(max = QUERY_LEN) @Pattern(regexp = RefFilter.QUERY) String query,
		@RequestParam(required = false) Instant modifiedAfter,
		@RequestParam(defaultValue = "500") int size
	) {
		if (size > configs.root().getMaxReplEntityBatch()) throw new TooLargeException(size, configs.root().getMaxReplEntityBatch());
		return pluginService.page(
				TagFilter.builder()
					.origin(origin)
					.query(query)
					.modifiedAfter(modifiedAfter)
					.build(),
				PageRequest.of(0, size, by(Ref_.MODIFIED)))
			.getContent();
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("plugin/cursor")
	Instant pluginCursor(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin
	) {
		return pluginService.cursor(origin);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@PostMapping("plugin")
	void pluginPush(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestBody List<@Valid Plugin> plugins
	) {
		logger.debug("Receiving push of {} plugins", plugins.size());
		RuntimeException first = null;
		for (var plugin : plugins) {
			try {
				plugin.setOrigin(origin);
				pluginService.push(plugin);
			} catch (RuntimeException e) {
				// TODO: Ignore auth errors?
				first = first == null ? e : first;
			}
		}
		if (first != null) throw first;
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("template")
	List<TemplateDto> template(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestParam(required = false) @Length(max = QUERY_LEN) @Pattern(regexp = RefFilter.QUERY) String query,
		@RequestParam(required = false) Instant modifiedAfter,
		@RequestParam(defaultValue = "500") int size
	) {
		if (size > configs.root().getMaxReplEntityBatch()) throw new TooLargeException(size, configs.root().getMaxReplEntityBatch());
		return templateService.page(
				TagFilter.builder()
					.origin(origin)
					.query(query)
					.modifiedAfter(modifiedAfter)
					.build(),
				PageRequest.of(0, size, by(Ref_.MODIFIED)))
			.getContent();
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("template/cursor")
	Instant templateCursor(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin
	) {
		return templateService.cursor(origin);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@PostMapping("template")
	void templatePush(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestBody List<@Valid Template> templates
	) {
		logger.debug("Receiving push of {} templates", templates.size());
		RuntimeException first = null;
		for (var template : templates) {
			try {
				template.setOrigin(origin);
				templateService.push(template);
			} catch (RuntimeException e) {
				// TODO: Ignore auth errors?
				first = first == null ? e : first;
			}
		}
		if (first != null) throw first;
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "404"),
		@ApiResponse(responseCode = "500", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@GetMapping("cache")
	ResponseEntity<StreamingResponseBody> fetch(
		WebRequest request,
		@RequestParam @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url,
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin
	) throws URISyntaxException, IOException {
		InputStream is;
		if (isNotBlank(props.getCacheApi())) {
			is = jasperClient.fetch(new URI(props.getCacheApi()), jasperHeaders(request), url, origin).getBody().getInputStream();
		} else {
			is = proxyService.fetchIfExists(url, origin);
		}
		if (is == null) throw new NotFoundException(url);
		var ref = proxyService.stat(url, origin, false);
		String filename = "file";
		try {
			filename
				= isNotBlank(getName(new URI(url).getPath())) ? getName(new URI(url).getPath())
				: ref != null && isNotBlank(ref.getTitle()) ? ref.getTitle()
				: filename;
		} catch (URISyntaxException ignored) { }
		var response = ResponseEntity.ok();
		var cache = proxyService.cache(url, origin, false);
		if (cache != null && cache.getContentLength() != null) response.contentLength(cache.getContentLength());
		return response
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20"))
			.contentType(cache != null && isNotBlank(cache.getMimeType()) ? MediaType.parseMediaType(cache.getMimeType()) : MediaType.APPLICATION_OCTET_STREAM)
			.cacheControl(CacheControl.maxAge(100, TimeUnit.DAYS).cachePrivate())
			.body(outputStream -> {
				try (is) {
					byte[] buffer = new byte[64 * 1024];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						outputStream.write(buffer, 0, bytesRead);
					}
				}
			});
	}

	@ApiResponses({
		@ApiResponse(responseCode = "201"),
		@ApiResponse(responseCode = "500", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@ResponseStatus(HttpStatus.CREATED)
	@PutMapping("cache")
	void push(
		WebRequest request,
		@RequestParam @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url,
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		InputStream data
	) throws IOException, URISyntaxException {
		if (isNotBlank(props.getCacheApi())) {
			jasperClient.push(new URI(props.getCacheApi()), jasperHeaders(request), url, origin, data.readAllBytes());
		} else {
			proxyService.push(url, origin, data);
		}
	}

	@ApiResponses({
		@ApiResponse(responseCode = "201"),
		@ApiResponse(responseCode = "500", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping("cache")
	RefReplDto save(
		WebRequest request,
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestParam(required = false) String title,
		@RequestParam(required = false) String mime,
		InputStream data
	) throws IOException, URISyntaxException {
		if (isNotBlank(props.getCacheApi())) {
			return jasperClient.save(new URI(props.getCacheApi()), jasperHeaders(request), origin, title, mime, data.readAllBytes());
		} else {
			return mapper.dtoToRepl(proxyService.save(origin, title, data, mime));
		}
	}
}
