package jasper.web.rest;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jasper.aop.ClearIdle;
import jasper.component.HttpCache;
import jasper.domain.Ref;
import jasper.domain.proj.HasOrigin;
import jasper.domain.proj.Tag;
import jasper.service.TaggingService;
import jasper.service.dto.RefDto;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

import static jasper.domain.Ref.URL_LEN;
import static jasper.domain.proj.HasOrigin.ORIGIN_LEN;
import static jasper.domain.proj.Tag.TAG_LEN;

@ClearIdle
@RestController
@RequestMapping("api/v1/tags")
@Validated
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tagging")
@ApiResponses({
	@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	@ApiResponse(responseCode = "403", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
})
public class TaggingController {

	@Autowired
	TaggingService taggingService;

	@Autowired
	HttpCache httpCache;

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PostMapping
	Instant createTags(
		@RequestParam @Length(max = TAG_LEN) @Pattern(regexp = Tag.REGEX) String tag,
		@RequestParam @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url,
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin
	) {
		return taggingService.create(tag, url, origin);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@DeleteMapping
	Instant deleteTags(
		@RequestParam @Length(max = TAG_LEN) @Pattern(regexp = Tag.REGEX) String tag,
		@RequestParam @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url,
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin
	) {
		return taggingService.delete(tag, url, origin);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PatchMapping
	Instant patchTags(
		@RequestParam List<@NotBlank @Length(max = TAG_LEN + 1) @Pattern(regexp = Tag.ADD_REMOVE_REGEX) String> tags,
		@RequestParam @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url,
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin
	) {
		return taggingService.tag(tags, url, origin);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("response")
	HttpEntity<RefDto> getResponse(
		@RequestParam(defaultValue = "") @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url
	) {
		return httpCache.ifNotModified(taggingService.getResponse(url));
	}

	@ApiResponses({
		@ApiResponse(responseCode = "201"),
		@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PostMapping("response")
	@ResponseStatus(HttpStatus.CREATED)
	void createResponse(
		@RequestParam(defaultValue = "") @Length(max = TAG_LEN) @Pattern(regexp = Tag.REGEX) String tag,
		@RequestParam(defaultValue = "") @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url
	) {
		taggingService.createResponse(tag, url);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@DeleteMapping("response")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteResponse(
		@RequestParam @Length(max = TAG_LEN) @Pattern(regexp = Tag.REGEX) String tag,
		@RequestParam(defaultValue = "") @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url
	) {
		taggingService.deleteResponse(tag, url);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PatchMapping("response")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void respond(
		@RequestParam List<@NotBlank @Length(max = TAG_LEN + 1) @Pattern(regexp = Tag.ADD_REMOVE_REGEX) String> tags,
		@RequestParam(defaultValue = "") @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url
	) {
		taggingService.respond(tags, url, null);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PatchMapping(value = "response", consumes = "application/json-patch+json")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void patchResponse(
		@RequestParam List<@NotBlank @Length(max = TAG_LEN + 1) @Pattern(regexp = Tag.ADD_REMOVE_REGEX) String> tags,
		@RequestParam(defaultValue = "") @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url,
		@RequestBody JsonPatch patch
	) {
		taggingService.respond(tags, url, patch);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PatchMapping(value = "response", consumes = "application/merge-patch+json")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void mergeResponse(
		@RequestParam List<@NotBlank @Length(max = TAG_LEN + 1) @Pattern(regexp = Tag.ADD_REMOVE_REGEX) String> tags,
		@RequestParam(defaultValue = "") @Length(max = URL_LEN) @Pattern(regexp = Ref.REGEX) String url,
		@RequestBody JsonMergePatch patch
	) {
		taggingService.respond(tags, url, patch);
	}
}
