package jasper.component;

import io.micrometer.core.annotation.Counted;
import jakarta.annotation.PostConstruct;
import jasper.config.Props;
import jasper.domain.*;
import jasper.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Profile("preload")
@Component
public class Preload {
	private final Logger logger = LoggerFactory.getLogger(Preload.class);
	private static final String PRELOAD = "preload";

	@Autowired
	Props props;

	@Autowired
	RefRepository refRepository;

	@Autowired
	ExtRepository extRepository;

	@Autowired
	UserRepository userRepository;

	@Autowired
	PluginRepository pluginRepository;

	@Autowired
	TemplateRepository templateRepository;

	@Autowired
	Optional<Storage> storage;

	@Autowired
	Backup backup;

	@PostConstruct
	public void init() {
		if (storage.isEmpty()) {
			logger.error("Preload enabled but no storage present.");
			return;
		}
		for (var file : storage.get().listStorage(props.getOrigin(), PRELOAD)) {
			var filename = file.id().toLowerCase();
			if (filename.endsWith(".zip")) {
				loadStatic(props.getOrigin(), file.id());
			} else if (filename.matches("(ref|ext|user|plugin|template).*\\.json")) {
				loadJsonFile(props.getOrigin(), file.id());
			}
		}
	}

	@Counted(value = "jasper.preload")
	public void loadStatic(String origin, String id) {
		if (storage.isEmpty()) {
			logger.error("Error loading static files with no storage present.");
			return;
		}
		var start = Instant.now();
		logger.info("{} Preloading static files {}", origin, id);
		try (var zipped = storage.get().streamZip(origin, PRELOAD, id)) {
			backup.restoreRepo(refRepository, origin, zipped.list("ref.*\\.json"), Ref.class);
			backup.restoreRepo(extRepository, origin, zipped.list("ext.*\\.json"), Ext.class);
			backup.restoreRepo(userRepository, origin, zipped.list("user.*\\.json"), User.class);
			backup.restoreRepo(pluginRepository, origin, zipped.list("plugin.*\\.json"), Plugin.class);
			backup.restoreRepo(templateRepository, origin, zipped.list("template.*\\.json"), Template.class);
		} catch (Throwable e) {
			logger.error("{} Error preloading {}", origin, id, e);
		}
		logger.info("{} Finished Preload in {}", origin, Duration.between(start, Instant.now()));
    }

	@Counted(value = "jasper.preload")
	public void loadJsonFile(String origin, String id) {
		if (storage.isEmpty()) {
			logger.error("Error loading JSON file with no storage present.");
			return;
		}
		var start = Instant.now();
		logger.info("{} Preloading JSON file {}", origin, id);
		try {
			var filename = id.toLowerCase();
			var inputStream = storage.get().stream(origin, PRELOAD, id);
			if (filename.matches("ref.*\\.json")) {
				backup.restoreRepo(refRepository, origin, inputStream, Ref.class);
			} else if (filename.matches("ext.*\\.json")) {
				backup.restoreRepo(extRepository, origin, inputStream, Ext.class);
			} else if (filename.matches("user.*\\.json")) {
				backup.restoreRepo(userRepository, origin, inputStream, User.class);
			} else if (filename.matches("plugin.*\\.json")) {
				backup.restoreRepo(pluginRepository, origin, inputStream, Plugin.class);
			} else if (filename.matches("template.*\\.json")) {
				backup.restoreRepo(templateRepository, origin, inputStream, Template.class);
			}
		} catch (Throwable e) {
			logger.error("{} Error preloading JSON file {}", origin, id, e);
		}
		logger.info("{} Finished Preload in {}", origin, Duration.between(start, Instant.now()));
	}
}
