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

import static jasper.component.vm.RunProcess.runProcess;

@Component
public class Shell {
	private static final Logger logger = LoggerFactory.getLogger(Shell.class);

	@Autowired
	Props props;

	@Value("http://localhost:${server.port}")
	String api;

	// language=Sh
	private final String wrapperScript = """
set -e
CURRENT_SHELL=$(basename "$0")
IFS= read -r -d '' TARGET_SCRIPT
IFS= read -r -d '' INPUT_STRING
SCRIPT_DIR=$(mktemp -d)
trap 'rm -rf "$SCRIPT_DIR"' EXIT
SCRIPT_FILE="$SCRIPT_DIR/script.sh"
printf '%s\\n' "$TARGET_SCRIPT" > "$SCRIPT_FILE"
chmod +x "$SCRIPT_FILE"
TIMEOUT_SECONDS=$((($1 + 999) / 1000))
JASPER_API="$2" timeout "$TIMEOUT_SECONDS" "$CURRENT_SHELL" "$SCRIPT_FILE" << EOF
$INPUT_STRING
EOF
    """;

	@Timed("jasper.vm")
	public String runShellScript(String targetScript, String inputString, int timeoutMs) throws ScriptException, IOException {
		var process = new ProcessBuilder(props.getShell(), "-c", wrapperScript, props.getShell(), String.valueOf(timeoutMs), api).start();
		try (var writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
			writer.write(targetScript);
			writer.write("\0"); // null character as delimiter
			writer.write(inputString);
			writer.write("\0"); // both inputs must be null terminated
			writer.flush();
		} catch (IOException e) {
			logger.warn("Script terminated before receiving input.");
		}
		return runProcess(process, timeoutMs);
	}
}
