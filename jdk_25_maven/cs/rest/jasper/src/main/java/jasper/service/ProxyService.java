package jasper.service;

import io.micrometer.core.annotation.Timed;
import jasper.component.FileCache;
import jasper.component.Proxy;
import jasper.errors.NotAvailableException;
import jasper.errors.NotFoundException;
import jasper.plugin.Cache;
import jasper.repository.RefRepository;
import jasper.security.Auth;
import jasper.service.dto.DtoMapper;
import jasper.service.dto.RefDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

import static jasper.plugin.Cache.getCache;
import static jasper.security.AuthoritiesConstants.USER;

@Service
public class ProxyService {

	@Autowired
	RefRepository refRepository;

	@Autowired
	Auth auth;

	@Autowired
	Optional<FileCache> fileCache;

	@Autowired
	Proxy proxy;

	@Autowired
	DtoMapper mapper;

	@PreAuthorize("@auth.hasRole('USER') && @auth.subOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "proxy"}, histogram = true)
	public void preFetch(String url, String origin, boolean thumbnail) {
		if (fileCache.isEmpty()) throw new NotAvailableException();
		fileCache.get().preFetch(url, origin, thumbnail);
	}

	@PreAuthorize("@auth.subOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "proxy"}, histogram = true)
	public InputStream fetchIfExists(String url, String origin) {
		if (fileCache.isEmpty()) throw new NotAvailableException();
		if (!refRepository.existsByUrlAndOrigin(url, origin)) throw new NotFoundException("Cache not found");
		return fileCache.get().fetch(url, origin);
	}

	@PreAuthorize("@auth.subOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "proxy"}, histogram = true)
	public RefDto stat(String url, String origin, boolean thumbnail) {
		// Only require role for new scrapes
		if (!url.startsWith("cache:") && !auth.hasRole(USER) && !refRepository.existsByUrlAndOrigin(url, origin)) throw new AccessDeniedException("Requires USER role to scrape.");
		return mapper.domainToDto(thumbnail
			? proxy.statThumbnail(url, origin)
			: proxy.stat(url, origin));
	}

	@PreAuthorize("@auth.subOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "proxy"}, histogram = true)
	public Cache cache(String url, String origin, boolean thumbnail) {
		return getCache(thumbnail
			? proxy.statThumbnail(url, origin)
			: proxy.stat(url, origin));
	}

	@PreAuthorize("@auth.subOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "proxy"}, histogram = true)
	public InputStream fetch(String url, String origin, boolean thumbnail) {
		// Only require role for new scrapes
		if (!url.startsWith("cache:") && !auth.minFetchRole() && !refRepository.existsByUrlAndOrigin(url, origin)) {
			throw new AccessDeniedException("Not found and not allowed to scrape.");
		}
		return thumbnail
			? proxy.fetchThumbnail(url, origin)
			: proxy.fetch(url, origin);
	}

	@PreAuthorize("@auth.hasRole('USER') && @auth.subOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "proxy"}, histogram = true)
	public RefDto save(String origin, String title, InputStream in, String mime) throws IOException {
		if (fileCache.isEmpty()) throw new NotAvailableException();
		return mapper.domainToDto(fileCache.get().save(origin, title, in, mime, "plugin/file", auth.getUserTag() == null ? null : auth.getUserTag().tag));
	}

	@PreAuthorize("@auth.hasRole('MOD') && @auth.subOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "proxy"}, histogram = true)
	public void push(String url, String origin, InputStream in) throws IOException {
		if (fileCache.isEmpty()) throw new NotAvailableException();
		fileCache.get().push(url, origin, in);
	}

	@PreAuthorize("@auth.hasRole('MOD') && @auth.subOrigin(#origin)")
	@Timed(value = "jasper.service", extraTags = {"service", "proxy"}, histogram = true)
	public void clearDeleted(String origin) {
		if (fileCache.isEmpty()) throw new NotAvailableException();
		fileCache.get().clearDeleted(origin);
	}
}
