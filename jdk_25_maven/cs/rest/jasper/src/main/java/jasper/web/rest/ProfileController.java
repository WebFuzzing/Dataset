package jasper.web.rest;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jasper.aop.ClearIdle;
import jasper.domain.User;
import jasper.service.ProfileService;
import jasper.service.dto.ProfileDto;
import org.hibernate.validator.constraints.Length;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static jasper.domain.proj.Tag.QTAG_LEN;

@Profile("scim")
@ClearIdle
@RestController
@RequestMapping("api/v1/profile")
@Validated
@Tag(name = "Profile")
@ApiResponses({
	@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	@ApiResponse(responseCode = "403", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
})
public class ProfileController {
	@Autowired
	ProfileService profileService;

	@ApiResponses({
		@ApiResponse(responseCode = "201"),
		@ApiResponse(responseCode = "409", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	void createProfile(
		@RequestBody @Valid ProfileDto profile
	) {
		profileService.create(profile.getTag(), profile.getPassword(), profile.getRole());
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "304", content = @Content()),
		@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@GetMapping
	ProfileDto getProfile(
		@RequestParam @Length(max = QTAG_LEN) @Pattern(regexp = User.QTAG_REGEX) String tag
	) {
		return profileService.get(tag);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping("page")
	Page<ProfileDto> getProfilePage(
		@PageableDefault @ParameterObject Pageable pageable
	) {
		return profileService.page(pageable.getPageNumber(), pageable.getPageSize());
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PostMapping("password")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void changeProfilePassword(
		@RequestBody @Valid ProfileDto profile
	) {
		profileService.changePassword(profile.getTag(), profile.getPassword());
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PostMapping("role")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void changeProfileRole(
		@RequestBody @Valid ProfileDto profile
	) {
		profileService.changeRole(profile.getTag(), profile.getRole());
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PostMapping("activate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void activateProfile(
		@RequestBody @Length(max = QTAG_LEN) @Pattern(regexp = User.QTAG_REGEX) String tag
	) {
		profileService.setActive(tag, true);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PostMapping("deactivate")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deactivateProfile(
		@RequestBody @Length(max = QTAG_LEN) @Pattern(regexp = User.QTAG_REGEX) String tag
	) {
		profileService.setActive(tag, false);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteProfile(
		@RequestParam @Length(max = QTAG_LEN) @Pattern(regexp = User.QTAG_REGEX) String tag
	) {
		profileService.delete(tag);
	}
}
