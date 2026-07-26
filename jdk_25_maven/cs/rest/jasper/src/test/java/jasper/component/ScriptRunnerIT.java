package jasper.component;

import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import jasper.IntegrationTest;
import jasper.config.Props;
import jasper.component.vm.JavaScript;
import jasper.component.vm.Python;
import jasper.component.vm.Shell;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.errors.OperationForbiddenOnOriginException;
import jasper.errors.ScriptException;
import jasper.errors.UntrustedScriptException;
import jasper.plugin.config.Script;
import jasper.repository.PluginRepository;
import jasper.repository.RefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static jasper.repository.spec.RefSpec.hasSource;
import static jasper.repository.spec.RefSpec.hasTag;
import static java.nio.file.Files.createFile;
import static java.nio.file.Files.exists;
import static java.time.Duration.ofSeconds;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.util.ReflectionTestUtils.getField;

@IntegrationTest
public class ScriptRunnerIT {

	@Autowired
	Props props;

	@Autowired
    ScriptRunner scriptRunner;

	@Autowired
	RefRepository refRepository;

	@Autowired
	PluginRepository pluginRepository;

	@Autowired
	IngestPlugin ingestPlugin;

	@Autowired
	ScriptExecutorFactory scriptExecutorFactory;

	@Autowired
	BulkheadRegistry bulkheadRegistry;

	@Autowired
	JavaScript javaScript;

	@Autowired
	Python python;

	@Autowired
	Shell shell;

	@TempDir
	Path tempDir;

	Ref getRef(String url, String title, String comment, String ...tags) {
		var ref = new Ref();
		ref.setUrl(url);
		ref.setTitle(title);
		ref.setComment(comment);
		ref.setTags(new ArrayList<>(List.of(tags)));
		return ref;
	}

	@BeforeEach
	void init() {
		props.setNode(props.getNode().replaceFirst("^~", System.getProperty("user.home")));
		refRepository.deleteAll();
		pluginRepository.deleteAll();
	}


	@Test
	void testJavaScriptJson() throws UntrustedScriptException {
		// language=JavaScript
		var upperCaseScript = """
			const fs = require('fs');
			const uuid = require('uuid');
			const ref = JSON.parse(fs.readFileSync(0, 'utf-8'));
			var output = {
			  url: 'comment:' + uuid.v4(),
			  sources: [ref.url],
		  	  title: 'Re: ' + ref.title,
		  	  comment: ref.comment.toUpperCase(),
			  tags: ['public', '+needle'],
			};
			console.log(JSON.stringify({
			  ref: [output],
			}));
		""";
		var script = Script.builder()
			.timeoutMs(30_000)
			.language("javascript")
			.format("json")
			.script(upperCaseScript)
			.build();
		var url = "comment:" + UUID.randomUUID();
		var input = getRef(url, "My Ref", "test", "public");

		scriptRunner.runScripts(input, "plugin/script/uppercase", script);

		var responses = refRepository.findAll(hasSource(url).and(hasTag("+needle")));
		assertThat(responses.size()).isEqualTo(1);
		var output = responses.get(0);
		assertThat(output.getComment()).isEqualTo("TEST");
	}

	@Test
	void testPythonJson() throws UntrustedScriptException {
		// language=Python
		var upperCaseScript = """
import sys
import json
from uuid import uuid4
ref = json.loads(sys.stdin.read())
output = {
  'url': 'comment:' + str(uuid4()),
  'sources': [ref['url']],
  'title': 'Re: ' + ref['title'],
  'comment': ref['comment'].upper(),
  'tags': ['public', '+needle'],
}
print(json.dumps({
  'ref': [output],
}))
		""";
		var script = Script.builder()
			.timeoutMs(30_000)
			.language("python")
			.format("json")
			.script(upperCaseScript)
			.build();
		var url = "comment:" + UUID.randomUUID();
		var input = getRef(url, "My Ref", "test", "public");

		scriptRunner.runScripts(input, "plugin/script/uppercase", script);

		var responses = refRepository.findAll(hasSource(url).and(hasTag("+needle")));
		assertThat(responses.size()).isEqualTo(1);
		var output = responses.get(0);
		assertThat(output.getComment()).isEqualTo("TEST");
	}

	@Test
	void testJavaScriptYaml() throws UntrustedScriptException {
		// language=JavaScript
		var upperCaseScript = """
			const fs = require('fs');
			const yaml = require('js-yaml');
			const uuid = require('uuid');
			const ref = yaml.load(fs.readFileSync(0, 'utf-8'));
			var output = {
			  url: 'comment:' + uuid.v4(),
			  sources: [ref.url],
		  	  title: 'Re: ' + ref.title,
		  	  comment: ref.comment.toUpperCase(),
			  tags: ['public', '+needle'],
			};
			console.log(yaml.dump({
			  ref: [output],
			}));
		""";
		var script = Script.builder()
			.timeoutMs(30_000)
			.language("javascript")
			.format("yaml")
			.script(upperCaseScript)
			.build();
		var url = "comment:" + UUID.randomUUID();
		var input = getRef(url, "My Ref", "test", "public");

		scriptRunner.runScripts(input, "plugin/script/uppercase", script);

		var responses = refRepository.findAll(hasSource(url).and(hasTag("+needle")));
		assertThat(responses.size()).isEqualTo(1);
		var output = responses.get(0);
		assertThat(output.getComment()).isEqualTo("TEST");
	}

	@Test
	void testPythonYaml() throws UntrustedScriptException {
		// language=Python
		var upperCaseScript = """
import sys
import yaml
from uuid import uuid4
ref = yaml.safe_load(sys.stdin.read())
output = {
  'url': 'comment:' + str(uuid4()),
  'sources': [ref['url']],
  'title': 'Re: ' + ref['title'],
  'comment': ref['comment'].upper(),
  'tags': ['public', '+needle'],
}
print(yaml.dump({
  'ref': [output],
}))
		""";
		var script = Script.builder()
			.timeoutMs(30_000)
			.language("python")
			.format("yaml")
			.script(upperCaseScript)
			.build();
		var url = "comment:" + UUID.randomUUID();
		var input = getRef(url, "My Ref", "test", "public");

		scriptRunner.runScripts(input, "plugin/script/uppercase", script);

		var responses = refRepository.findAll(hasSource(url).and(hasTag("+needle")));
		assertThat(responses.size()).isEqualTo(1);
		var output = responses.get(0);
		assertThat(output.getComment()).isEqualTo("TEST");
	}

	@Test
	void testShellJson() throws UntrustedScriptException {
		// language=Bash
		var upperCaseScript = """
			jq --arg uuid $(uuidgen) '{
			  ref: [{
				url: ("comment:" + $uuid),
				title: ("Re: " + .title),
				comment: .comment | ascii_upcase,
				tags: (.tags + ["+needle"]),
				sources: [.url]
			  }]
			}'
		""";
		var script = Script.builder()
			.timeoutMs(30_000)
			.language("shell")
			.format("json")
			.script(upperCaseScript)
			.build();
		var url = "comment:" + UUID.randomUUID();
		var input = getRef(url, "My Ref", "test", "public");

		scriptRunner.runScripts(input, "plugin/script/uppercase", script);

		var responses = refRepository.findAll(hasSource(url).and(hasTag("+needle")));
		assertThat(responses.size()).isEqualTo(1);
		var output = responses.get(0);
		assertThat(output.getComment()).isEqualTo("TEST");
	}

	@Test
	void testUninstallCancelsBunScript() throws Exception {
		var started = tempDir.resolve("bun-started");
		var completed = tempDir.resolve("bun-completed");
		assertUninstallCancels("plugin/script/bun.cancel", started, completed, () -> javaScript.runJavaScript("""
			const fs = require('fs');
			fs.writeFileSync('%s', '');
			await Bun.sleep(10_000);
			fs.writeFileSync('%s', '');
			""".formatted(started, completed), "", 30_000));
	}

	@Test
	void testUninstallCancelsPythonScript() throws Exception {
		var started = tempDir.resolve("python-started");
		var completed = tempDir.resolve("python-completed");
		assertUninstallCancels("plugin/script/python.cancel", started, completed, () -> python.runPython("", """
			import time
			open('%s', 'w').close()
			time.sleep(10)
			open('%s', 'w').close()
			""".formatted(started, completed), "", 30_000));
	}

	@Test
	void testUninstallCancelsBashScript() throws Exception {
		var started = tempDir.resolve("bash-started");
		var completed = tempDir.resolve("bash-completed");
		assertUninstallCancels("plugin/script/bash.cancel", started, completed, () -> shell.runShellScript("""
			touch '%s'
			sleep 10
			touch '%s'
			""".formatted(started, completed), "", 30_000));
	}

	@Test
	void testUninstallCancelsQueuedScript() throws Exception {
		var tag = "plugin/script/queued.cancel";
		var queuedMarker = tempDir.resolve("queued-ran");
		var plugin = new Plugin();
		plugin.setTag(tag);
		pluginRepository.save(plugin);
		var bulkhead = bulkheadRegistry.bulkhead(tag, BulkheadConfig.custom()
			.maxConcurrentCalls(1)
			.maxWaitDuration(ofSeconds(30))
			.build());
		bulkhead.acquirePermission();

		try {
			var queued = scriptExecutorFactory.run(tag, "", () -> {
				try {
					createFile(queuedMarker);
				} catch (IOException e) {
					throw new RuntimeException(e);
				}
			});
			var queuedDeadline = System.nanoTime() + SECONDS.toNanos(2);
			while (executionCount(tag) < 1 && System.nanoTime() < queuedDeadline) {
				Thread.sleep(10);
			}
			assertThat(executionCount(tag)).isEqualTo(1);

			ingestPlugin.delete(tag);

			assertThatThrownBy(() -> queued.get(2, SECONDS))
				.isInstanceOf(ExecutionException.class);
			assertThat(queuedMarker).doesNotExist();
		} finally {
			bulkhead.releasePermission();
		}
	}

	private int executionCount(String tag) {
		var executions = (Map<?, ?>) getField(scriptExecutorFactory, "executions");
		var threads = (Set<?>) executions.get(tag);
		return threads == null ? 0 : threads.size();
	}

	private void assertUninstallCancels(String tag, Path started, Path completed, ScriptExecution execution) throws Exception {
		var plugin = new Plugin();
		plugin.setTag(tag);
		pluginRepository.save(plugin);
		var future = scriptExecutorFactory.run(tag, "", () -> {
			try {
				execution.run();
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		var startDeadline = System.nanoTime() + SECONDS.toNanos(5);
		while (!exists(started) && System.nanoTime() < startDeadline) {
			Thread.sleep(10);
		}
		assertThat(started).exists();

		ingestPlugin.delete(tag);

		try {
			future.get(2, SECONDS);
		} catch (ExecutionException expected) {
			assertThat(expected)
				.hasRootCauseInstanceOf(ScriptException.class)
				.hasRootCauseMessage("Script execution interrupted");
		}
		assertThat(completed).doesNotExist();
	}

	@FunctionalInterface
	private interface ScriptExecution {
		void run() throws Exception;
	}

	@Test
	void testUninstalledScriptDoesNotRun() throws UntrustedScriptException {
		var input = getRef("comment:" + UUID.randomUUID(), "My Ref", "test", "public");
		var tag = "plugin/script/uninstalled";
		var plugin = new Plugin();
		plugin.setTag(tag);
		pluginRepository.save(plugin);

		scriptRunner.runScripts(input, tag);
		ingestPlugin.delete(tag);

		assertThatThrownBy(() -> scriptRunner.runScripts(input, tag))
			.isInstanceOf(OperationForbiddenOnOriginException.class);
	}

}
