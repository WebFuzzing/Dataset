package jasper.component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import jakarta.persistence.EntityManager;
import jasper.component.Storage.Zipped;
import jasper.config.Props;
import jasper.domain.Ext;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.domain.Template;
import jasper.domain.User;
import jasper.domain.proj.Cursor;
import jasper.repository.BackfillRepository;
import jasper.repository.ExtRepository;
import jasper.repository.PluginRepository;
import jasper.repository.RefRepository;
import jasper.repository.StreamMixin;
import jasper.repository.TemplateRepository;
import jasper.repository.UserRepository;
import jasper.service.dto.BackupOptionsDto;
import jasper.util.JsonArrayStreamDataSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static jasper.component.FileCache.CACHE;

@Component
public class Backup {
	private final Logger logger = LoggerFactory.getLogger(Backup.class);
	private static final String BACKUPS = "backups";

	@Autowired
	Props props;

	@Autowired
	RefRepository refRepository;

	@Autowired
	BackfillRepository backfillRepository;

	@Autowired
	ExtRepository extRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PluginRepository pluginRepository;

	@Autowired
	TemplateRepository templateRepository;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	EntityManager entityManager;

	@Autowired
	PlatformTransactionManager transactionManager;

	@Autowired
	Optional<Storage> storage;

	public record BackupStream(InputStream inputStream, long size) {}

	@Async
	@Transactional(readOnly = true)
	@Counted(value = "jasper.backup")
	public void createBackup(String origin, String id, BackupOptionsDto options) throws IOException {
		if (storage.isEmpty()) {
			logger.error("{} Backup create failed: No storage present.", origin);
			return;
		}
		var start = Instant.now();
		logger.info("{} Creating Backup", origin);
		try (var zipped = storage.get().zipAt(origin, BACKUPS, id + ".zip")) {
			if (options.isRef()) {
				backupRepo(refRepository, origin, options.getNewerThan(), zipped.out("ref.json"), false);
			}
			if (options.isExt()) {
				backupRepo(extRepository, origin, options.getNewerThan(), zipped.out("ext.json"));
			}
			if (options.isUser()) {
				backupRepo(userRepository, origin, options.getNewerThan(), zipped.out("user.json"));
			}
			if (options.isPlugin()) {
				backupRepo(pluginRepository, origin, options.getNewerThan(), zipped.out("plugin.json"));
			}
			if (options.isTemplate()) {
				backupRepo(templateRepository, origin, options.getNewerThan(), zipped.out("template.json"));
			}
			if (options.isCache()) {
				backupCache(origin, options.getNewerThan(), zipped);
			}
		}
		logger.info("{} Finished Backup in {}", origin, Duration.between(start, Instant.now()));
	}

	void backupRepo(StreamMixin<?> repo, String origin, Instant newerThan, OutputStream out) throws IOException {
		backupRepo(repo, origin, newerThan, out, true);
	}

	void backupRepo(StreamMixin<?> repo, String origin, Instant newerThan, OutputStream out, boolean evict) throws IOException {
		try (out) {
			var firstElementProcessed = new AtomicBoolean(false);
			var buf = new StringBuilder();
			buf.append("[");
			var buffSize = props.getBackupBufferSize();
			Stream<?> stream;
			if (newerThan != null) {
				stream = repo.streamAllByOriginAndModifiedGreaterThanEqualOrderByModifiedDesc(origin, newerThan);
			} else {
				stream = repo.streamAllByOriginOrderByModifiedDesc(origin);
			}
			stream.forEach(entity -> {
				try {
					if (firstElementProcessed.getAndSet(true)) {
						buf.append(",\n");
					}
					buf.append(objectMapper.writeValueAsString(entity));
					if (buf.length() > buffSize) {
						logger.debug("Flushing buffer {} bytes", buf.length());
						StreamUtils.copy(buf.toString().getBytes(), out);
						buf.setLength(0);
						entityManager.clear();
					}
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
				if (evict) {
					entityManager.detach(entity);
				}
			});
			buf.append("]");
			logger.debug("Flushing buffer {} bytes", buf.length());
			StreamUtils.copy(buf.toString().getBytes(), out);
		}
	}

	void backupCache(String origin, Instant newerThan, Zipped backup) {
		try {
			storage.get().backup(origin, CACHE, backup, newerThan);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Timed(value = "jasper.backup", histogram = true)
	public BackupStream get(String origin, String id) {
		if (storage.isEmpty()) {
			logger.error("Backup get failed: No storage present.");
			return null;
		}
		return new BackupStream(
			storage.get().stream(origin, BACKUPS, id + ".zip"),
			storage.get().size(origin, BACKUPS, id + ".zip")
		);
	}

	public boolean exists(String origin, String id) {
		if (storage.isEmpty()) {
			logger.error("Backup exist check failed: No storage present.");
			return false;
		}
		return storage.get().exists(origin, BACKUPS, id + ".zip");
	}

	public List<Storage.StorageRef> listBackups(String origin) {
		if (storage.isEmpty()) {
			logger.error("Backup list failed: No storage present.");
			return null;
		}
		return storage.get().listStorage(origin, BACKUPS).stream()
			.filter(n -> n.id().endsWith(".zip")).toList();
	}

	@Async
	@Counted(value = "jasper.backup")
	public void restore(String origin, String id, BackupOptionsDto options) {
		if (storage.isEmpty()) {
			logger.error("{} Backup restore failed: No storage present.", origin);
			return;
		}
		var start = Instant.now();
		logger.info("{} Restoring Backup", origin);
		try (var zipped = storage.get().streamZip(origin, BACKUPS, id + ".zip")) {
			if (options == null || options.isRef()) {
				restoreRepo(refRepository, origin, zipped.list("ref.*\\.json"), Ref.class);
			}
			if (options == null || options.isExt()) {
				restoreRepo(extRepository, origin, zipped.list("ext.*\\.json"), Ext.class);
			}
			if (options == null || options.isUser()) {
				restoreRepo(userRepository, origin, zipped.list("user.*\\.json"), User.class);
			}
			if (options == null || options.isPlugin()) {
				restoreRepo(pluginRepository, origin, zipped.list("plugin.*\\.json"), Plugin.class);
			}
			if (options == null || options.isTemplate()) {
				restoreRepo(templateRepository, origin, zipped.list("template.*\\.json"), Template.class);
			}
			if (options == null || options.isCache()) {
				restoreCache(origin, zipped);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		logger.info("{} Finished Restore in {}", origin, Duration.between(start, Instant.now()));
	}

	<T extends Cursor> void restoreRepo(JpaRepository<T, ?> repo, String origin, Iterator<InputStream> files, Class<T> type) {
		files.forEachRemaining(file -> {
			restoreRepo(repo, origin, file, type);
		});
    }

	<T extends Cursor> void restoreRepo(JpaRepository<T, ?> repo, String origin, InputStream file, Class<T> type) {
		if (file == null) return; // Silently ignore missing files
		var done = new AtomicBoolean(false);
		var it = new JsonArrayStreamDataSupplier<>(file, type, objectMapper);
		int count = 0;
		try {
			while (!done.get()) {
				if (count > 0) logger.info("{} {} {} restored...", origin, type.getSimpleName(), count * props.getRestoreBatchSize());
				var lastBatchCount = count * props.getRestoreBatchSize();
				TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
				transactionTemplate.execute(status -> {
					for (var i = 0; i < props.getRestoreBatchSize(); i++) {
						if (!it.hasNext()) {
							done.set(true);
							logger.info("{} {} {} restored...", origin, type.getSimpleName(), lastBatchCount + i);
							return null;
						}
						var t = it.next();
						try {
							t.setOrigin(origin);
							repo.save(t);
						} catch (Exception e) {
							try {
								logger.error("{} Skipping {} {} due to constraint violation", origin, type.getSimpleName(), objectMapper.writeValueAsString(t), e);
							} catch (JsonProcessingException ex) {
								logger.error("{} Skipping {} {} due to constraint violation", origin, type.getSimpleName(), type, e);
							}
						}
					}
					return null;
				});
				count++;
			}
		} catch (Exception e) {
			logger.error("Failed to restore", e);
		}
    }

	private void restoreCache(String origin, Zipped backup) {
		try {
			storage.get().restore(origin, CACHE, backup);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Timed(value = "jasper.backup", histogram = true)
	public void store(String origin, String id, InputStream zipFile) throws IOException {
		if (storage.isEmpty()) {
			logger.error("Backup store failed: No storage present.");
			return;
		}
		storage.get().storeAt(origin, BACKUPS, id+ ".zip", zipFile);
	}

	/**
	 * Will first drop and then regenerate all metadata
	 * for the given origin and all sub-origins.
	 */
	@Async
	@Counted(value = "jasper.backup")
	public void regen(String origin) {
		var start = Instant.now();
		logger.info("{} Starting Backfill", origin);
		refRepository.dropMetadata(origin);
		logger.info("{} Cleared old metadata", origin);
		int count = 0;
		while (props.getBackfillBatchSize() == backfillRepository.backfillMetadata(origin, props.getBackfillBatchSize())) {
			count += props.getBackfillBatchSize();
			logger.info("{} Generating metadata... {} done", origin, count);
		}
		logger.info("{} Finished Backfill in {}", origin, Duration.between(start, Instant.now()));
	}

	@Timed(value = "jasper.backup", histogram = true)
	public void delete(String origin, String id) throws IOException {
		if (storage.isEmpty()) {
			logger.error("Backup delete failed: No storage present.");
			return;
		}
		storage.get().delete(origin, BACKUPS, id + ".zip");
	}
}
