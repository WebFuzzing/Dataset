package jasper.component.cron;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import jasper.IntegrationTest;
import jasper.component.IngestPlugin;
import jasper.config.Props;
import jasper.domain.Plugin;
import jasper.domain.Ref;
import jasper.repository.PluginRepository;
import jasper.repository.RefRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.nio.file.Files.exists;
import static java.util.concurrent.TimeUnit.SECONDS;
import static jasper.repository.spec.RefSpec.hasSource;
import static jasper.repository.spec.RefSpec.hasTag;
import static org.assertj.core.api.Assertions.assertThat;

@IntegrationTest
public class ScriptIT {

	@Autowired
	Props props;

	@Autowired
	Script cronScript;

	@Autowired
	RefRepository refRepository;

	@Autowired
	PluginRepository pluginRepository;

	@Autowired
	IngestPlugin ingestPlugin;

	@TempDir
	Path tempDir;

	Plugin getScriptPlugin(String tag, String language, String script) {
		var plugin = new Plugin();
		plugin.setTag(tag);
		var mapper = new ObjectMapper();
		try {
			plugin.setConfig((ObjectNode) mapper.readTree("""
			{
				"timeoutMs": 30000,
				"format": "json",
				"language": "",
				"script": ""
			}"""));
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}
		plugin.getConfig().set("language", TextNode.valueOf(language));
		plugin.getConfig().set("script", TextNode.valueOf(script));
		return plugin;
	}

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
	}

	@Test
	void testJavaScriptUpperCaseRef() throws Exception {
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
		pluginRepository.save(getScriptPlugin("plugin/script/test", "javascript", upperCaseScript));
		var url = "comment:" + UUID.randomUUID();
		var input = getRef(url, "My Ref", "test", "public", "+plugin/cron", "plugin/script/test");
		refRepository.save(input);

		cronScript.run(input);

		var responses = refRepository.findAll(hasSource(url).and(hasTag("+needle")));
		assertThat(responses.size()).isEqualTo(1);
		var output = responses.get(0);
		assertThat(output.getComment()).isEqualTo("TEST");
	}

	@Test
	void testUninstallCancelsScript() throws Exception {
		var started = tempDir.resolve("script-started");
		var completed = tempDir.resolve("script-completed");
		// language=Shell Script
		var slowScript = """
			touch '%s'
			sleep 30
			touch '%s'
			""".formatted(started, completed);
		pluginRepository.save(getScriptPlugin("plugin/script/cancel", "shell", slowScript));
		var url = "comment:" + UUID.randomUUID();
		var input = getRef(url, "My Ref", "test", "public", "+plugin/cron", "plugin/script/cancel");
		refRepository.save(input);

		var run = CompletableFuture.runAsync(() -> {
			try {
				cronScript.run(input);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
		var startDeadline = System.nanoTime() + SECONDS.toNanos(30);
		while (!exists(started) && System.nanoTime() < startDeadline) {
			Thread.sleep(10);
		}
		assertThat(started).exists();

		ingestPlugin.delete("plugin/script/cancel");

		// An uncancelled script sleeps for 30 seconds, so completing within 15 proves interruption
		run.get(15, SECONDS);
		assertThat(completed).doesNotExist();
	}

	@Test
	void testPythonUpperCaseRef() throws Exception {
		// language=Python
		var upperCaseScript = """
import sys
import json
from uuid import uuid4
ref = json.loads(sys.stdin.read());
output = {
  'url': 'comment:' + str(uuid4()),
  'sources': [ref['url']],
  'title': 'Re: ' + ref['title'],
  'comment': ref['comment'].upper(),
  'tags': ['public', '+needle'],
};
print(json.dumps({
  'ref': [output],
}))
		""";
		pluginRepository.save(getScriptPlugin("plugin/script/test", "python", upperCaseScript));
		var url = "comment:" + UUID.randomUUID();
		var input = getRef(url, "My Ref", "test", "public", "+plugin/cron", "plugin/script/test");
		refRepository.save(input);

		cronScript.run(input);

		var responses = refRepository.findAll(hasSource(url).and(hasTag("+needle")));
		assertThat(responses.size()).isEqualTo(1);
		var output = responses.get(0);
		assertThat(output.getComment()).isEqualTo("TEST");
	}

}
