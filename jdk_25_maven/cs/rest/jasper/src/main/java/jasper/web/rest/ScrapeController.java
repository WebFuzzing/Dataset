package jasper.web.rest;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jasper.aop.ClearIdle;
import jasper.service.ScrapeService;
import jasper.service.dto.RefDto;
import org.hibernate.validator.constraints.Length;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static jasper.domain.Ref.URL_LEN;

@Profile("proxy | file-cache")
@ClearIdle
@RestController
@RequestMapping("api/v1/scrape")
@Validated
@Tag(name = "Scrape")
public class ScrapeController {

	@Autowired
	ScrapeService scrapeService;

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "500", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@GetMapping("web")
	ResponseEntity<RefDto> scrapeWebpage(@RequestParam @Length(max = URL_LEN) @URL String url) throws IOException, URISyntaxException {
		return ResponseEntity.ok()
			.cacheControl(CacheControl.maxAge(100, TimeUnit.DAYS).cachePrivate())
			.body(scrapeService.webpage(url));
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "500", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@GetMapping("rss")
	ResponseEntity<String> rss(@RequestParam @Length(max = URL_LEN) String url) throws IOException {
		return ResponseEntity.ok()
			.cacheControl(CacheControl.maxAge(100, TimeUnit.DAYS).cachePrivate())
			.body(scrapeService.rss(url));
	}
}
