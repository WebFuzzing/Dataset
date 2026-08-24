package jasper.web.rest;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jasper.aop.ClearIdle;
import jasper.domain.proj.HasOrigin;
import jasper.service.SmtpService;
import jasper.service.dto.SmtpWebhookDto;
import org.hibernate.validator.constraints.Length;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static jasper.domain.proj.HasOrigin.ORIGIN_LEN;

@ClearIdle
@RestController
@RequestMapping("api/v1/webhook")
@Validated
@Tag(name = "webhook")
@ApiResponses({
	@ApiResponse(responseCode = "201"),
	@ApiResponse(responseCode = "400", content = @Content()),
	@ApiResponse(responseCode = "403", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
})
public class WebhookController {
	private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

	@Autowired
	SmtpService smtpService;

	@PostMapping("smtp")
	void smtp(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestBody SmtpWebhookDto email
	) {
		smtpService.create(email, origin);
	}
}
