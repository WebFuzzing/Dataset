package jasper.client;

import feign.HeaderMap;
import feign.Param;
import feign.QueryMap;
import feign.RequestLine;
import jasper.domain.Ext;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.domain.Template;
import jasper.domain.User;
import jasper.service.dto.RefReplDto;
import jasper.service.dto.UserDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static jasper.security.Auth.LOCAL_ORIGIN_HEADER;
import static jasper.security.Auth.READ_ACCESS_HEADER;
import static jasper.security.Auth.TAG_READ_ACCESS_HEADER;
import static jasper.security.Auth.TAG_WRITE_ACCESS_HEADER;
import static jasper.security.Auth.USER_ROLE_HEADER;
import static jasper.security.Auth.USER_TAG_HEADER;
import static jasper.security.Auth.WRITE_ACCESS_HEADER;

@FeignClient(value = "jasper", url = "https://jasperkm.info")
public interface JasperClient {

	@RequestLine("GET /pub/api/v1/repl/ref")
	List<Ref> refPull(URI baseUri, @QueryMap Map<String, Object> params);
	@RequestLine("GET /pub/api/v1/repl/ref")
	List<Ref> refPull(URI baseUri, @HeaderMap Map<String, Object> headers, @QueryMap Map<String, Object> params);
	@RequestLine("GET /pub/api/v1/repl/ref/cursor?origin={origin}")
	Instant refCursor(URI baseUri, @Param("origin") String origin);
	@RequestLine("GET /pub/api/v1/repl/ref/cursor?origin={origin}")
	Instant refCursor(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("origin") String origin);
	@RequestLine("POST /pub/api/v1/repl/ref?origin={origin}")
	void refPush(URI baseUri, @Param("origin") String origin, List<RefReplDto> push);
	@RequestLine("POST /pub/api/v1/repl/ref?origin={origin}")
	void refPush(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("origin") String origin, List<RefReplDto> push);

	@RequestLine("GET /pub/api/v1/repl/ext")
	List<Ext> extPull(URI baseUri, @QueryMap Map<String, Object> params);
	@RequestLine("GET /pub/api/v1/repl/ext")
	List<Ext> extPull(URI baseUri, @HeaderMap Map<String, Object> headers, @QueryMap Map<String, Object> params);
	@RequestLine("GET /pub/api/v1/repl/ext/cursor?origin={origin}")
	Instant extCursor(URI baseUri, @Param("origin") String origin);
	@RequestLine("GET /pub/api/v1/repl/ext/cursor?origin={origin}")
	Instant extCursor(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("origin") String origin);
	@RequestLine("POST /pub/api/v1/repl/ext?origin={origin}")
	void extPush(URI baseUri, @Param("origin") String origin, List<Ext> push);
	@RequestLine("POST /pub/api/v1/repl/ext?origin={origin}")
	void extPush(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("origin") String origin, List<Ext> push);

	@RequestLine("GET /pub/api/v1/repl/user")
	List<User> userPull(URI baseUri, @QueryMap Map<String, Object> params);
	@RequestLine("GET /pub/api/v1/repl/user")
	List<User> userPull(URI baseUri, @HeaderMap Map<String, Object> headers, @QueryMap Map<String, Object> params);
	@RequestLine("GET /pub/api/v1/repl/user/cursor?origin={origin}")
	Instant userCursor(URI baseUri, @Param("origin") String origin);
	@RequestLine("GET /pub/api/v1/repl/user/cursor?origin={origin}")
	Instant userCursor(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("origin") String origin);
	@RequestLine("POST /pub/api/v1/repl/user?origin={origin}")
	void userPush(URI baseUri, @Param("origin") String origin, List<UserDto> push);
	@RequestLine("POST /pub/api/v1/repl/user?origin={origin}")
	void userPush(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("origin") String origin, List<UserDto> push);

	@RequestLine("GET /pub/api/v1/repl/plugin")
	List<Plugin> pluginPull(URI baseUri, @QueryMap Map<String, Object> params);
	@RequestLine("GET /pub/api/v1/repl/plugin")
	List<Plugin> pluginPull(URI baseUri, @HeaderMap Map<String, Object> headers, @QueryMap Map<String, Object> params);
	@RequestLine("GET /pub/api/v1/repl/plugin/cursor?origin={origin}")
	Instant pluginCursor(URI baseUri, @Param("origin") String origin);
	@RequestLine("GET /pub/api/v1/repl/plugin/cursor?origin={origin}")
	Instant pluginCursor(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("origin") String origin);
	@RequestLine("POST /pub/api/v1/repl/plugin?origin={origin}")
	void pluginPush(URI baseUri, @Param("origin") String origin, List<Plugin> push);
	@RequestLine("POST /pub/api/v1/repl/plugin?origin={origin}")
	void pluginPush(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("origin") String origin, List<Plugin> push);

	@RequestLine("GET /pub/api/v1/repl/template")
	List<Template> templatePull(URI baseUri, @QueryMap Map<String, Object> params);
	@RequestLine("GET /pub/api/v1/repl/template")
	List<Template> templatePull(URI baseUri, @HeaderMap Map<String, Object> headers, @QueryMap Map<String, Object> params);
	@RequestLine("GET /pub/api/v1/repl/template/cursor?origin={origin}")
	Instant templateCursor(URI baseUri, @Param("origin") String origin);
	@RequestLine("GET /pub/api/v1/repl/template/cursor?origin={origin}")
	Instant templateCursor(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("origin") String origin);
	@RequestLine("POST /pub/api/v1/repl/template?origin={origin}")
	void templatePush(URI baseUri, @Param("origin") String origin, List<Template> push);
	@RequestLine("POST /pub/api/v1/repl/template?origin={origin}")
	void templatePush(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("origin") String origin, List<Template> push);

	@RequestLine("GET /pub/api/v1/repl/cache?url={url}&origin={origin}")
	ResponseEntity<Resource> fetch(URI baseUri, @Param("url") String url, @Param("origin") String origin);
	@RequestLine("GET /pub/api/v1/repl/cache?url={url}&origin={origin}")
	ResponseEntity<Resource> fetch(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("url") String url, @Param("origin") String origin);
	@RequestLine("PUT /pub/api/v1/repl/cache?url={url}&origin={origin}")
	void push(URI baseUri, @Param("url") String url, @Param("origin") String origin, byte[] data);
	@RequestLine("PUT /pub/api/v1/repl/cache?url={url}&origin={origin}")
	void push(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("url") String url, @Param("origin") String origin, byte[] data);
	@RequestLine("POST /pub/api/v1/repl/cache?origin={origin}&title={title}&mime={mime}")
	RefReplDto save(URI baseUri, @Param("origin") String origin, @Param("title") String title, @Param("mime") String mime, byte[] data);
	@RequestLine("POST /pub/api/v1/repl/cache?origin={origin}&title={title}&mime={mime}")
	RefReplDto save(URI baseUri, @HeaderMap Map<String, Object> headers, @Param("origin") String origin, @Param("title") String title, @Param("mime") String mime, byte[] data);

	static Map<String, Object> jasperHeaders(WebRequest req) {
		return Map.of(
			"Authorization", Objects.toString(req.getHeader("Authorization"), ""),
			USER_TAG_HEADER, Objects.toString(req.getHeader(USER_TAG_HEADER), ""),
			USER_ROLE_HEADER, Objects.toString(req.getHeader(USER_ROLE_HEADER), ""),
			LOCAL_ORIGIN_HEADER, Objects.toString(req.getHeader(LOCAL_ORIGIN_HEADER), ""),
			WRITE_ACCESS_HEADER, Objects.toString(req.getHeader(WRITE_ACCESS_HEADER), ""),
			READ_ACCESS_HEADER, Objects.toString(req.getHeader(READ_ACCESS_HEADER), ""),
			TAG_WRITE_ACCESS_HEADER, Objects.toString(req.getHeader(TAG_WRITE_ACCESS_HEADER), ""),
			TAG_READ_ACCESS_HEADER, Objects.toString(req.getHeader(TAG_READ_ACCESS_HEADER), "")
		);
	}

	static Map<String, Object> params(Object... params) {
		var result = new HashMap<String, Object>();
		for (var i = 0; i < params.length; i += 2) {
			result.put(params[i].toString(), params[i+1]);
		}
		return result;
	}
}
