package jasper.web.rest;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jasper.aop.ClearIdle;
import jasper.domain.proj.HasOrigin;
import jasper.service.OriginService;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static jasper.domain.proj.HasOrigin.ORIGIN_LEN;

@ClearIdle
@RestController
@RequestMapping("api/v1/origin")
@Validated
@Tag(name = "Origin")
@ApiResponses({
	@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	@ApiResponse(responseCode = "403", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
})
public class OriginController {

	@Autowired
	OriginService originService;

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping
	List<String> listOrigins() {
		return originService.listOrigins();
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteOrigin(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestParam(required = false) Instant olderThan
	) {
		if (olderThan == null) olderThan = Instant.now();
		originService.delete(origin, olderThan);
	}
}
