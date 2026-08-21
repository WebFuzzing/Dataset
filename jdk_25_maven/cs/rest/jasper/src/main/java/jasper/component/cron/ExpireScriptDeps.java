package jasper.component.cron;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import static java.lang.System.getProperty;
import static java.nio.file.Files.delete;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.getLastModifiedTime;
import static java.nio.file.Files.walkFileTree;
import static java.time.Instant.now;

@Profile("scripts")
@Component
public class ExpireScriptDeps {
	private static final Logger logger = LoggerFactory.getLogger(ExpireScriptDeps.class);

	@Scheduled(fixedDelay = 24, initialDelay = 24, timeUnit = TimeUnit.HOURS)
	public void removeExpiredScriptDeps() throws IOException {
		var tmpDir = Paths.get(getProperty("java.io.tmpdir"));
		var cutoff = now().minus(24, ChronoUnit.HOURS);

		walkFileTree(tmpDir, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				if (dir.equals(tmpDir)) return FileVisitResult.CONTINUE;

				var requirementsFile = dir.resolve("requirements.txt");
				if (!exists(requirementsFile)) return FileVisitResult.CONTINUE;
				if (getLastModifiedTime(requirementsFile).toInstant().isAfter(cutoff)) return FileVisitResult.CONTINUE;

				deleteDirectoryContents(dir);
				logger.info("Deleted expired venv: {}", dir);
				return FileVisitResult.SKIP_SUBTREE;
			}
		});
	}

	private void deleteDirectoryContents(Path dir) throws IOException {
		walkFileTree(dir, new SimpleFileVisitor<>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
				if (!d.equals(dir)) {
					delete(d);
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
