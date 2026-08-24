package jasper.web.rest;

import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.mergepatch.JsonMergePatch;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jasper.aop.ClearIdle;
import jasper.component.HttpCache;
import jasper.domain.User;
import jasper.errors.NotFoundException;
import jasper.repository.filter.TagFilter;
import jasper.service.UserService;
import jasper.service.dto.RolesDto;
import jasper.service.dto.UserDto;
import org.hibernate.validator.constraints.Length;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;

import static jasper.domain.proj.Tag.QTAG_LEN;
import static jasper.repository.filter.Query.QUERY_LEN;
import static jasper.repository.filter.Query.SEARCH_LEN;

@ClearIdle
@RestController
@RequestMapping("api/v1/user")
@Validated
@Tag(name = "User")
@ApiResponses({
	@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	@ApiResponse(responseCode = "403", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
})
public class UserController {

	@Autowired
	UserService userService;

	@Autowired
	HttpCache httpCache;

	@ApiResponses({
		@ApiResponse(responseCode = "201"),
		@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	Instant createUser(
		@RequestBody @Valid User user
	) {
		return userService.create(user);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "304", content = @Content()),
		@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@GetMapping
	HttpEntity<UserDto> getUser(
		@RequestParam @Length(max = QTAG_LEN) @Pattern(regexp = User.QTAG_REGEX) String tag
	) {
		try {
			return httpCache.ifNotModified(userService.get(tag));
		} catch (NotFoundException e) {
			// Catch to avoid error logging
			return ResponseEntity.notFound().build();
		}
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "304", content = @Content()),
	})
	@GetMapping("page")
	HttpEntity<Page<UserDto>> getUserPage(
		@PageableDefault(sort = "tag") @ParameterObject Pageable pageable,
		@RequestParam(required = false) @Length(max = QUERY_LEN) @Pattern(regexp = TagFilter.QUERY) String query,
		@RequestParam(required = false) Integer nesting,
		@RequestParam(required = false) Integer level,
		@RequestParam(required = false) Boolean deleted,
		@RequestParam(required = false) Instant modifiedBefore,
		@RequestParam(required = false) Instant modifiedAfter,
		@RequestParam(required = false) @Length(max = SEARCH_LEN) String search
	) {
		return httpCache.ifNotModifiedPage(userService.page(
			TagFilter.builder()
				.query(query)
				.nesting(nesting)
				.level(level)
				.deleted(deleted)
				.search(search)
				.modifiedBefore(modifiedBefore)
				.modifiedAfter(modifiedAfter)
				.build(),
			pageable));
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
		@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PutMapping
	Instant updateUser(
		@RequestBody @Valid User user
	) {
		return userService.update(user);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PatchMapping(consumes = "application/json-patch+json")
	Instant patchUser(
		@RequestParam @Length(max = QTAG_LEN) @Pattern(regexp = jasper.domain.proj.Tag.QTAG_REGEX) String tag,
		@RequestParam Instant cursor,
		@RequestBody JsonPatch patch
	) {
		return userService.patch(tag, cursor, patch);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PatchMapping(consumes = "application/merge-patch+json")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Instant mergeUser(
		@RequestParam @Length(max = QTAG_LEN) @Pattern(regexp = jasper.domain.proj.Tag.QTAG_REGEX) String tag,
		@RequestParam Instant cursor,
		@RequestBody JsonMergePatch patch
	) {
		return userService.patch(tag, cursor, patch);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
		@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PostMapping("keygen")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	Instant keygen(
			@RequestParam @Length(max = QTAG_LEN) @Pattern(regexp = User.QTAG_REGEX) String tag
	) throws NoSuchAlgorithmException, IOException {
		return userService.keygen(tag);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteUser(
		@RequestParam @Length(max = QTAG_LEN) @Pattern(regexp = User.QTAG_REGEX) String tag
	) {
		userService.delete(tag);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("whoami")
	RolesDto whoAmI() {
		return userService.whoAmI();
	}
}
