package jasper.service;

import io.micrometer.core.annotation.Timed;
import jasper.component.Scraper;
import jasper.security.Auth;
import jasper.service.dto.DtoMapper;
import jasper.service.dto.RefDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;

@Profile("proxy | file-cache")
@Service
public class ScrapeService {

	@Autowired
	Auth auth;

	@Autowired
	DtoMapper mapper;

	@Autowired
	Scraper scraper;

	@PreAuthorize("@auth.hasRole('USER')")
	@Timed(value = "jasper.service", extraTags = {"service", "scrape"}, histogram = true)
	public RefDto webpage(String url) throws IOException, URISyntaxException {
		var ref = scraper.web(url, auth.getOrigin());
		if (ref == null) return null;
		return mapper.domainToDto(ref);
	}

	@PreAuthorize("@auth.hasRole('USER')")
	@Timed(value = "jasper.service", extraTags = {"service", "scrape"}, histogram = true)
	public String rss(String url) throws IOException {
		return scraper.rss(url);
	}
}
