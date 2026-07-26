package jasper.web.rest;

import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Pattern;
import jasper.aop.ClearIdle;
import jasper.component.Backup;
import jasper.domain.proj.HasOrigin;
import jasper.errors.NotFoundException;
import jasper.service.BackupService;
import jasper.service.dto.BackupDto;
import jasper.service.dto.BackupOptionsDto;
import org.hibernate.validator.constraints.Length;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static jasper.domain.proj.HasOrigin.ORIGIN_LEN;
import static jasper.service.dto.BackupOptionsDto.ID_LEN;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.http.MediaType.parseMediaType;

@ClearIdle
@RestController
@RequestMapping("api/v1/backup")
@Tag(name = "Backup")
@ApiResponses({
	@ApiResponse(responseCode = "403", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
})
public class BackupController {

	@Autowired
	BackupService backupService;

	@ApiResponses({
		@ApiResponse(responseCode = "201"),
	})
	@PostMapping
	String createBackup(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@RequestBody(required = false) BackupOptionsDto options
	) throws IOException {
		return backupService.createBackup(origin, options);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
	})
	@GetMapping
	List<BackupDto> listBackups(@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin) {
		return backupService.listBackups(origin);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "304", content = @Content()),
		@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
		@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@GetMapping("{id}")
	ResponseEntity<StreamingResponseBody> downloadBackup(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@PathVariable @Length(max = ID_LEN) String id,
		@RequestParam(defaultValue = "") String p
	) {
		if (id.endsWith(".zip")) {
			id = id.substring(0, id.length() - 4);
		}
		Backup.BackupStream b;
		if (isBlank(p)) {
			b = backupService.getBackup(origin, id);
		} else {
			if (!backupService.unlock(p)) throw new NotFoundException(id);
			b = backupService.getBackupPreauth(origin, id);
		}
		if (b == null) throw new NotFoundException("Storage unavailable!");
		return ResponseEntity.ok()
			.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + URLEncoder.encode(id + ".zip", StandardCharsets.UTF_8).replace("+", "%20"))
			.contentLength(b.size())
			.contentType(parseMediaType("application/zip"))
			.cacheControl(CacheControl.maxAge(100, TimeUnit.DAYS).cachePrivate())
			.body(outputStream -> {
				try (var is = b.inputStream()) {
					byte[] buffer = new byte[64 * 1024];
					int bytesRead;
					while ((bytesRead = is.read(buffer)) != -1) {
						outputStream.write(buffer, 0, bytesRead);
					}
				}
			});
	}

	@ApiResponses({
		@ApiResponse(responseCode = "200"),
		@ApiResponse(responseCode = "304", content = @Content()),
		@ApiResponse(responseCode = "400", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
		@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PostMapping("key")
	String getBackupKey(
		@RequestParam(defaultValue = "") String key
	) {
		return backupService.getKey(key);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "201"),
	})
	@PostMapping("upload/{id}")
	@ResponseStatus(HttpStatus.CREATED)
	void uploadBackup(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@PathVariable @Length(max = ID_LEN) String id,
		InputStream zipFile
	) throws IOException {
		if (id.endsWith(".zip")) {
			id = id.substring(0, id.length() - 4);
		}
		backupService.uploadBackup(origin, id, zipFile);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
		@ApiResponse(responseCode = "404", content = @Content(schema = @Schema(ref = "https://opensource.zalando.com/problem/schema.yaml#/Problem"))),
	})
	@PostMapping("restore/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void restoreBackup(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@PathVariable @Length(max = ID_LEN) String id,
		@RequestBody(required = false) BackupOptionsDto options
	) {
		if (id.endsWith(".zip")) {
			id = id.substring(0, id.length() - 4);
		}
		backupService.restoreBackup(origin, id, options);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@PostMapping("regen")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void regen(@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin) {
		backupService.regen(origin);
	}

	@ApiResponses({
		@ApiResponse(responseCode = "204"),
	})
	@DeleteMapping("{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	void deleteBackup(
		@RequestParam(defaultValue = "") @Length(max = ORIGIN_LEN) @Pattern(regexp = HasOrigin.REGEX) String origin,
		@PathVariable @Length(max = ID_LEN) String id
	) throws IOException {
		if (id.endsWith(".zip")) {
			id = id.substring(0, id.length() - 4);
		}
		backupService.deleteBackup(origin, id);
	}
}
