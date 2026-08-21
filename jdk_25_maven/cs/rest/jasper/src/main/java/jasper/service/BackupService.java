package jasper.service;

import io.micrometer.core.annotation.Timed;
import jasper.component.Backup;
import jasper.component.Ingest;
import jasper.domain.Ref;
import jasper.errors.NotFoundException;
import jasper.repository.RefRepository;
import jasper.security.Auth;
import jasper.service.dto.BackupDto;
import jasper.service.dto.BackupOptionsDto;
import jasper.service.dto.DtoMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static jasper.repository.spec.RefSpec.isUrl;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class BackupService {

	private static final BackupOptionsDto DEFAULT_OPTIONS = BackupOptionsDto.builder()
		.ref(true)
		.ext(true)
		.user(true)
		.plugin(false)
		.template(false)
		.cache(true)
		.build();

	@Autowired
	Backup backup;

	@Autowired
	RefRepository refRepository;

	@Autowired
	Ingest ingest;

	@Autowired
	Auth auth;

	@Autowired
	DtoMapper mapper;

	@PreAuthorize("@auth.subOrigin(#origin) and @auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "backup"}, histogram = true)
	public String createBackup(String origin, BackupOptionsDto options) throws IOException {
		var id = Instant.now().toString();
		if (options == null) options = DEFAULT_OPTIONS;
		if (options.getNewerThan() != null) {
			id += "_-_" + options.getNewerThan();
		}
		backup.createBackup(origin, id, options);
		return id;
	}

	@PreAuthorize("@auth.subOrigin(#origin) and @auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "backup"}, histogram = true)
	public void uploadBackup(String origin, String id, InputStream zipFile) throws IOException {
		backup.store(origin, id, zipFile);
	}

	@PreAuthorize("@auth.subOrigin(#origin) and @auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "backup"}, histogram = true)
	public List<BackupDto> listBackups(String origin) {
		return backup.listBackups(origin).stream()
			.map(mapper::domainToDto)
			.toList();
	}

	@PreAuthorize("@auth.subOrigin(#origin) and @auth.minReadBackupRole()")
	@Timed(value = "jasper.service", extraTags = {"service", "backup"}, histogram = true)
	public Backup.BackupStream getBackup(String origin, String id) {
		return backup.get(origin, id);
	}

	@PreAuthorize("@auth.minReadBackupRole()")
	@Timed(value = "jasper.service", extraTags = {"service", "backup"}, histogram = true)
	public String getKey(String key) {
		var ref = refRepository.findOneByUrlAndOrigin("system:backup-key", auth.getOrigin()).orElse(null);
		if (ref == null) {
			ref = new Ref();
			ref.setUrl("system:backup-key");
			ref.setOrigin(auth.getOrigin());
			ref.addTag("internal");
			ref.addTag("_plugin/system");
			ref.setTitle("Backup Key");
			ref.setComment(UUID.randomUUID().toString());
			ingest.create(auth.getOrigin(), ref);
			return ref.getComment();
		}
		if (isBlank(ref.getComment()) || !ref.getComment().equals(key)) {
			ref.setComment(UUID.randomUUID().toString());
			ingest.update(auth.getOrigin(), ref);
		}
		return ref.getComment();
	}

	@Scheduled(fixedRate = 1, timeUnit = TimeUnit.MINUTES)
	public void clearBackupKey() {
		var list = refRepository.findAll(isUrl("system:backup-key"));
		for (var ref : list) {
			if (ref.getCreated().isBefore(Instant.now().minus(15, ChronoUnit.MINUTES))) {
				ingest.delete(ref.getOrigin(), "system:backup-key", ref.getOrigin());
			}
		}
	}

	public boolean unlock(String key) {
		if (isBlank(key)) return false;
		var ref = refRepository.findOneByUrlAndOrigin("system:backup-key", auth.getOrigin()).orElse(null);
		if (ref == null) return false;
		return key.equals(ref.getComment());
	}

	@PreAuthorize("@auth.subOrigin(#origin)")
	public Backup.BackupStream getBackupPreauth(String origin, String id) {
		return backup.get(origin, id);
	}

	@PreAuthorize("@auth.subOrigin(#origin) and @auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "backup"}, histogram = true)
	public void restoreBackup(String origin, String id, BackupOptionsDto options) {
		if (!backup.exists(origin, id)) throw new NotFoundException("Backup " + id);
		backup.restore(origin, id, options);
	}

	@PreAuthorize("@auth.subOrigin(#origin) and @auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "backup"}, histogram = true)
	public void regen(String origin) {
		backup.regen(origin);
	}

	@PreAuthorize("@auth.subOrigin(#origin) and @auth.hasRole('MOD')")
	@Timed(value = "jasper.service", extraTags = {"service", "backup"}, histogram = true)
	public void deleteBackup(String origin, String id) throws IOException {
		if (!backup.exists(origin, id)) return; // Delete is idempotent
		backup.delete(origin, id);
	}
}
