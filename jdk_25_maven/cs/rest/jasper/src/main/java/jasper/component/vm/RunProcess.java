package jasper.component.vm;

import jasper.errors.ScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

public class RunProcess {
	private static final Logger logger = LoggerFactory.getLogger(RunProcess.class);
	// Generous timeout to allow reader threads to drain remaining output after process termination
	private static final int READER_THREAD_JOIN_TIMEOUT_MS = 5_000;

	public static String runProcess(Process process, int timeoutMs) throws ScriptException {
		// StringBuffer is thread-safe, so reads stay consistent even if a reader thread outlives its join timeout
		final var output = new StringBuffer();
		final var errors = new StringBuffer();
		var outputThread = Thread.ofVirtual().start(() -> readStream(process.getInputStream(), output, "output"));
		var errorThread = Thread.ofVirtual().start(() -> readStream(process.getErrorStream(), errors, "error"));

		boolean finished;
		try {
			finished = process.waitFor(timeoutMs, TimeUnit.MILLISECONDS);
		} catch (InterruptedException e) {
			destroyTree(process);
			joinReaders(outputThread, errorThread);
			Thread.currentThread().interrupt();
			throw new ScriptException("Script execution interrupted", ""+errors + output);
		}
		destroyTree(process);
		joinReaders(outputThread, errorThread);
		if (!finished) {
			throw new ScriptException("Script execution timed out", ""+errors + output);
		}
		var exitCode = process.exitValue();
		if (exitCode != 0) {
			throw new ScriptException("Script execution failed with exit code: " + exitCode, ""+errors + output);
		}
		return output.toString();
	}

	private static void joinReaders(Thread outputThread, Thread errorThread) {
		try {
			outputThread.join(READER_THREAD_JOIN_TIMEOUT_MS);
			errorThread.join(READER_THREAD_JOIN_TIMEOUT_MS);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	private static void readStream(InputStream stream, StringBuffer sink, String name) {
		try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
			var buffer = new char[8192];
			int read;
			while ((read = reader.read(buffer)) != -1) {
				sink.append(buffer, 0, read);
			}
		} catch (IOException e) {
			logger.error("Error reading {} stream: {}", name, e.getMessage());
		}
	}

	private static void destroyTree(Process process) {
		// Snapshot descendants before killing the parent, since they reparent once it exits
		var descendants = process.descendants().toList();
		// Destroy the parent first so it cannot spawn new children
		process.destroy();
		descendants.forEach(ProcessHandle::destroy);
		try {
			if (!process.waitFor(1, TimeUnit.SECONDS)) {
				process.destroyForcibly();
			}
		} catch (InterruptedException e) {
			process.destroyForcibly();
			Thread.currentThread().interrupt();
		}
		descendants.stream().filter(ProcessHandle::isAlive).forEach(ProcessHandle::destroyForcibly);
	}
}
