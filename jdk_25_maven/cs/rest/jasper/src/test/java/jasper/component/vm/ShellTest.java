package jasper.component.vm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import jasper.config.Props;
import jasper.errors.ScriptException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ShellTest {

	Shell vm = new Shell();

	@BeforeEach
	public void init() throws IOException {
		var mapper = new ObjectMapper(new YAMLFactory());
		var app = mapper.readValue(new File("src/test/resources/config/application.yml"), ObjectNode.class);
		var props = app.get("jasper");

		var shell = System.getenv("JASPER_SHELL");
		if (isBlank(shell)) {
			shell = props.get("shell").textValue();
		}
		shell = shell.replaceFirst("^~", System.getProperty("user.home"));
		vm.props = new Props();
		vm.props.setShell(shell);
		vm.api = "http://localhost:10344";
	}

	@Test
	void testRunShellScript() throws IOException, InterruptedException, ScriptException {
		// language=sh
		var targetScript = """
            echo $(cat) | tr '[:lower:]' '[:upper:]'
        """;
		var input = "test";

		var output = vm.runShellScript(targetScript, input, 30_000);

		assertThat(output).isEqualToIgnoringWhitespace("TEST");
	}

	@Test
	void testRunShellScriptTimeout() {
		// language=Bash
		var targetScript = """
            sleep 2
            echo $(cat) | tr '[:lower:]' '[:upper:]'
        """;
		var input = "test";

		assertThatThrownBy(() -> vm.runShellScript(targetScript, input, 1_000))
			.isInstanceOf(ScriptException.class)
			.hasMessageContaining("Script execution timed out");
	}

	@Test
	void testRunShellScriptError() {
		// language=Bash
		var targetScript = """
            cat non_existent_file
        """;
		var input = "test";

		assertThatThrownBy(() -> vm.runShellScript(targetScript, input, 30_000))
			.isInstanceOf(ScriptException.class)
			.hasMessageContaining("Script execution failed with exit code:");
	}

	@Test
	void testRunShellFillStdoutBuffer() {
		// language=Bash
		var targetScript = """
            printf 'a%.0s' {1..65537}
        """;
		var input = "test";

		var future = CompletableFuture.supplyAsync(() -> {
			try {
				return vm.runShellScript(targetScript, input, 30_000);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		assertThat(future)
			.succeedsWithin(Duration.ofSeconds(2));
	}
}
