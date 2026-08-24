package jasper.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import jasper.client.OembedClient;
import jasper.component.OembedProviders;
import jasper.errors.NotFoundException;
import jasper.security.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class OembedService {
	private static final Logger logger = LoggerFactory.getLogger(OembedService.class);

	@Autowired
	OembedClient oembedClient;

	@Autowired
	OembedProviders oembedProviders;

	@Autowired
	Auth auth;

	@Autowired
	ObjectMapper objectMapper;

	@Cacheable(value = "oembed-cache", key = "#params.get('theme') + '-' + #params.get('maxwidth') + '-' + #params.get('maxheight') + '-' + #params.get('url')")
	@Transactional(readOnly = true)
	@PreAuthorize("@auth.hasRole('VIEWER')")
	@Timed(value = "jasper.service", extraTags = {"service", "oembed"}, histogram = true)
	public JsonNode get(Map<String, String> params) {
		if (isBlank(params.get("url"))) return null;
		var config = oembedProviders.getProvider(auth.getOrigin(), params.get("url"));
		if (config == null) return null;
		params.put("format", "json");
		try {
			return objectMapper.readTree(oembedClient.oembed(new URI(config.getUrl().replace("{format}", "json")), params));
		} catch (Exception e) {
			throw new NotFoundException(e.getMessage());
		}
	}

	@PreAuthorize("@auth.canAddTag('+plugin/oembed')")
	@Timed(value = "jasper.service", extraTags = {"service", "oembed"}, histogram = true)
	public void restoreDefaults() throws IOException {
		oembedProviders.defaults(auth.getOrigin());
	}
}
