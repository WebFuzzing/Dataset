package jasper.component.vm;

import io.micrometer.core.annotation.Timed;
import jasper.config.Props;
import jasper.errors.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import static jasper.component.vm.RunProcess.runProcess;
import static java.lang.System.getProperty;
import static java.nio.file.Files.createDirectories;
import static java.nio.file.Files.exists;
import static java.nio.file.Files.setAttribute;
import static java.nio.file.Files.writeString;
import static java.security.MessageDigest.getInstance;
import static java.time.Instant.now;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Component
public class Python {
	private static final Logger logger = LoggerFactory.getLogger(Python.class);
	private static final Duration UPDATE_COOLDOWN = Duration.of(15, ChronoUnit.MINUTES);

	@Autowired
	Props props;

	@Value("http://localhost:${server.port}")
	String api;

	private final Map<String, Instant> lastUpdate = new HashMap<>();
	private final ConcurrentHashMap<String, ReentrantLock> venvLocks = new ConcurrentHashMap<>();

	// language=Python
	private final String pythonVmWrapperScript = """
import sys, os
import subprocess
timeout_ms = int(sys.argv[1])
env = os.environ.copy()
env['JASPER_API'] = sys.argv[2]
input_data = sys.stdin.read()
target_script, input_string = input_data.split('\\0', 1)
process = subprocess.Popen([sys.executable, '-c', target_script], stdin=subprocess.PIPE, stdout=sys.stdout, stderr=sys.stderr, env=env)
try:
	process.communicate(input=input_string.encode(), timeout=timeout_ms / 1000)
except subprocess.TimeoutExpired:
	process.kill()
	sys.exit(1)
if process.returncode != 0:
	sys.exit(process.returncode)
	""";

	@Timed("jasper.vm")
	public String runPython(String requirements, String targetScript, String inputString, int timeoutMs) throws ScriptException, IOException, NoSuchAlgorithmException {
		var python = props.getPython();
		if (isNotBlank(requirements)) {
			var requirementsHash = encodeHexString(getInstance("SHA-256").digest(requirements.getBytes(StandardCharsets.UTF_8)));
			var tmpDir = Objects.toString(getProperty("java.io.tmpdir"), "/tmp");
			var venv = Paths.get(tmpDir).resolve(requirementsHash).toAbsolutePath();
			var requirementsFile = Paths.get(venv + "/requirements.txt");

			var lock = venvLocks.computeIfAbsent(requirementsHash, k -> new ReentrantLock());
			lock.lock();
			try {
				// Create virtual environment if it doesn't exist
				if (!exists(requirementsFile)) {
					var venvProcess = new ProcessBuilder(python, "-m", "venv", venv.toString()).start();
					runProcess(venvProcess, 60_000);
					createDirectories(venv);
					writeString(requirementsFile, requirements);
				}
				python = venv.resolve("bin/python").toString();

				if (!lastUpdate.containsKey(requirementsHash) || lastUpdate.get(requirementsHash).isBefore(now().minus(UPDATE_COOLDOWN))) {
					lastUpdate.put(requirementsHash, now());
					// Mark requirements.txt as accessed to prevent deletion for 24 hours
					var now = FileTime.from(now());
					setAttribute(requirementsFile, "lastAccessTime", now);
					setAttribute(requirementsFile, "lastModifiedTime", now);
					// Install requirements using pip
					var pip = venv.resolve("bin/pip").toString();
					var pipProcess = new ProcessBuilder(pip, "install", "--upgrade", "-r", requirementsFile.toString()).start();
					runProcess(pipProcess, 300_000);
				}
			} finally {
				lock.unlock();
			}
		}
		var scriptProcess = new ProcessBuilder(python, "-c", pythonVmWrapperScript, ""+timeoutMs, api).start();
		try (OutputStreamWriter writer = new OutputStreamWriter(scriptProcess.getOutputStream(), StandardCharsets.UTF_8)) {
			writer.write(targetScript);
			writer.write("\0"); // Null byte delimiter
			writer.write(inputString);
			writer.flush();
		} catch (IOException e) {
			logger.warn("Script terminated before receiving input.");
		}
		return runProcess(scriptProcess, timeoutMs);
	}
}
