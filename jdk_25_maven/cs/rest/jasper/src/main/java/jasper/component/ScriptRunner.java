package jasper.component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.annotation.Timed;
import jasper.component.dto.Bundle;
import jasper.component.dto.ComponentDtoMapper;
import jasper.component.script.ScriptDefaults;
import jasper.component.vm.JavaScript;
import jasper.component.vm.Python;
import jasper.component.vm.Shell;
import jasper.domain.Ref;
import jasper.errors.ScriptException;
import jasper.errors.UntrustedScriptException;
import jasper.plugin.config.Script;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;

import static jasper.util.Logging.getMessage;
import static java.security.MessageDigest.getInstance;
import static org.apache.commons.codec.binary.Hex.encodeHexString;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Component
public class ScriptRunner {
	private static final Logger logger = LoggerFactory.getLogger(ScriptRunner.class);

	@Autowired
	ConfigCache configs;

	@Autowired
	IngestBundle ingest;

	@Autowired
	Tagger tagger;

	@Autowired
	ScriptDefaults scriptDefaults;

	@Autowired
	JavaScript jsVm;

	@Autowired
	Python pythonVm;

	@Autowired
	Shell shell;

	@Autowired
	ComponentDtoMapper mapper;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	@Qualifier("yamlMapper")
	ObjectMapper yamlMapper;

	@Timed("jasper.scripts")
	public void validateScript(String script) throws UntrustedScriptException {
		if (isEmpty(configs.root().getScriptWhitelist())) return;
		try {
			var scriptHash = encodeHexString(getInstance("SHA-256").digest(script.getBytes(StandardCharsets.UTF_8)));
			if (!configs.root().getScriptWhitelist().contains(scriptHash)) {
				throw new UntrustedScriptException(scriptHash);
			}
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	@Timed("jasper.scripts")
	public void runScripts(Ref ref, String scriptTag) throws UntrustedScriptException {
		var config = configs.getPluginConfig(scriptTag, ref.getOrigin(), Script.class)
			.orElse(null);
		runScripts(ref, scriptTag, config);
	}

	void runScripts(Ref ref, String scriptTag, Script config) throws UntrustedScriptException {
		if (config == null || isBlank(config.getScript())) {
			scriptDefaults.runScript(ref, scriptTag);
			return;
		}
		validateScript(config.getScript());
		logger.info("{} Running script {} on {} ({})", ref.getOrigin(), scriptTag, ref.getTitle(), ref.getUrl());
		String input;
		try {
			switch (config.getFormat().toLowerCase()) {
			case "json":
				input = objectMapper.writeValueAsString(mapper.domainToDto(ref));
				break;
			case "yaml":
				input = yamlMapper.writeValueAsString(mapper.domainToDto(ref));
				break;
			default:
				logger.error("{} Script {} format not supported {}", ref.getOrigin(), scriptTag, config.getLanguage());
				tagger.attachError(ref.getOrigin(), ref, "Script runtime not supported: " + config.getLanguage());
				return;
			}
		} catch (Exception e) {
			logger.error("{} Error serializing script {} input", ref.getOrigin(), scriptTag, e);
			tagger.attachError(ref.getUrl(), ref.getOrigin(), "Error serializing script " + scriptTag + " input");
			return;
		}
		String output;
		try {
			switch (config.getLanguage().toLowerCase()) {
			case "javascript":
				output = jsVm.runJavaScript(config.getScript(), input, config.getTimeoutMs());
				break;
			case "python":
				output = pythonVm.runPython(config.getRequirements(), config.getScript(), input, config.getTimeoutMs());
				break;
			case "shell":
				output = shell.runShellScript(config.getScript(), input, config.getTimeoutMs());
				break;
			default:
				logger.error("{} Script {} runtime not supported {}", ref.getOrigin(), scriptTag, config.getLanguage());
				tagger.attachError(ref.getOrigin(), ref, "Script " + scriptTag + " runtime not supported: " + config.getLanguage());
				return;
			}
		} catch (ScriptException e) {
			logger.error("{} Error running script {} on {} ({}): {}", ref.getOrigin(), scriptTag, ref.getTitle(), ref.getUrl(), getMessage(e));
			tagger.attachError(ref.getUrl(), ref.getOrigin(), getMessage(e), e.getLogs());
			return;
		} catch (Exception e) {
			logger.error("{} Error running script {} on {} ({}): {}", ref.getOrigin(), scriptTag, ref.getTitle(), ref.getUrl(), getMessage(e));
			tagger.attachError(ref.getUrl(), ref.getOrigin(), getMessage(e));
			return;
		}
		if (isBlank(output)) return;
		Bundle bundle;
		try {
			switch (config.getFormat().toLowerCase()) {
			case "json":
				bundle = objectMapper.readValue(output, new TypeReference<Bundle>() {});
				break;
			case "yaml":
				bundle = yamlMapper.readValue(output, new TypeReference<Bundle>() {});
				break;
			default:
				// Unreachable
				throw new IllegalStateException();
			}
		} catch (Exception e) {
			logger.error("{} Error parsing script {} return value", ref.getOrigin(), scriptTag, e);
			tagger.attachError(ref.getUrl(), ref.getOrigin(), "Error parsing script " + scriptTag + " output", output);
			return;
		}
		ingest.createOrUpdate(bundle, ref.getOrigin(), ref.getUrl());
	}

}
