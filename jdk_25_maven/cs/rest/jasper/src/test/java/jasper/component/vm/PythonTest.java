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
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PythonTest {

	Python vm = new Python();

	@BeforeEach
	public void init() throws IOException {
		var mapper = new ObjectMapper(new YAMLFactory());
		var app = mapper.readValue(new File("src/test/resources/config/application.yml"), ObjectNode.class);
		var props = app.get("jasper");

		var python = System.getenv("JASPER_PYTHON");
		if (isBlank(python)) {
			python = props.get("python").textValue();
		}
		python = python.replaceFirst("^~", System.getProperty("user.home"));
		vm.props = new Props();
		vm.props.setPython(python);
		vm.api = "http://localhost:10344";
	}

	@Test
	void testRunPython() throws IOException, InterruptedException, ScriptException, NoSuchAlgorithmException {
		// language=Python
		var targetScript = """
import sys
print(sys.stdin.read().upper())
		""";
		var input = "test";

		var output = vm.runPython("", targetScript, input, 30_000);

		assertThat(output).isEqualToIgnoringWhitespace("TEST");
	}

	@Test
	void testPythonRequirements() throws IOException, InterruptedException, ScriptException, NoSuchAlgorithmException {
		// language=Python
		var targetScript = """
import requests
print(f"requests: {requests.__version__}")
		""";

		var output = vm.runPython("requests==2.31.0", targetScript, "", 30_000);

		assertThat(output).contains("requests: 2.31.0");
	}

	@Test
	void testRunPythonTimeout() {
		// language=Python
		var targetScript = """
import sys
import time
print(sys.stdin.read().upper())
time.sleep(2)
        """;
		var input = "test";

		assertThatThrownBy(() -> vm.runPython("", targetScript, input, 1_000))
			.isInstanceOf(ScriptException.class)
			.hasMessageContaining("Script execution timed out");
	}

	@Test
	void testRunPythonError() {
		// language=Python
		var targetScript = """
open('non_existent_file')
        """;
		var input = "test";

		assertThatThrownBy(() -> vm.runPython("", targetScript, input, 30_000))
			.isInstanceOf(ScriptException.class)
			.hasMessageContaining("Script execution failed with exit code:");
	}

	@Test
	void testRunPythonFillStdoutBuffer() {
		// language=Python
		var targetScript = """
print("a" * 65_536)
        """;
		var input = "test";

		var future = CompletableFuture.supplyAsync(() -> {
			try {
				return vm.runPython("", targetScript, input, 30_000);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		assertThat(future)
			.succeedsWithin(Duration.ofSeconds(2));
	}

}
