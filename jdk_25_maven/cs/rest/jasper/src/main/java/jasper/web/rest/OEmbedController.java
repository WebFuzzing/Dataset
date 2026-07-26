package jasper.web.rest;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jasper.aop.ClearIdle;
import jasper.service.OembedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@ClearIdle
@RestController
@RequestMapping("api/v1/oembed")
@Validated
@Tag(name = "oEmbed")
public class OEmbedController {

	@Autowired
	OembedService oembedService;

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "400", content = @Content()),
		@ApiResponse(responseCode = "404", content = @Content()),
	})
	@GetMapping
	ResponseEntity<JsonNode> oembed(@RequestParam Map<String, String> params) {
		return ResponseEntity.ok()
			.cacheControl(CacheControl.maxAge(100, TimeUnit.DAYS).cachePrivate())
			.body(oembedService.get(params));
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@PostMapping("defaults")
	void defaults() throws IOException {
		oembedService.restoreDefaults();
	}
}
