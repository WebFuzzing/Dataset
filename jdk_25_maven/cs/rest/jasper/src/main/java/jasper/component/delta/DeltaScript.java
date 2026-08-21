package jasper.component.delta;

import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import jakarta.annotation.PostConstruct;
import jasper.component.ConfigCache;
import jasper.component.ScriptExecutorFactory;
import jasper.component.ScriptRunner;
import jasper.component.Tagger;
import jasper.domain.Ref;
import jasper.errors.UntrustedScriptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import static jasper.domain.proj.Tag.matchesTag;
import static jasper.domain.proj.Tag.publicTag;

@Profile("scripts")
@Component
public class DeltaScript implements Async.AsyncRunner {
	private static final Logger logger = LoggerFactory.getLogger(DeltaScript.class);

	@Autowired
	ConfigCache config;

	@Autowired
	Async async;

	@Autowired
	ScriptExecutorFactory scriptExecutorFactory;

	@Autowired
	ScriptRunner scriptRunner;

	@Autowired
	Tagger tagger;

	@PostConstruct
	void init() {
		async.addAsyncTag("plugin/delta", this);
		async.addAsyncTag("_plugin/delta", this);
	}

	@Override
	public String signature() {
		return "+plugin/delta";
	}

	@Override
	@Bulkhead(name = "script")
	public void run(Ref ref) throws Exception {
		if (ref.hasTag("_seal/delta")) return;
		logger.debug("{} Searching for delta response scripts for {} ({})", ref.getOrigin(), ref.getTitle(), ref.getUrl());
		var tags = ref.getExpandedTags().stream()
			.filter(t -> !publicTag(t).equals("plugin/delta"))
			.filter(t -> matchesTag("plugin/delta", t) || matchesTag("_plugin/delta", t))
			.filter(t -> config.root().script(t, ref.getOrigin()))
			.sorted()
			.toList()
			.reversed();
		for (var scriptTag : tags) {
			scriptExecutorFactory.run(scriptTag, ref.getOrigin(), ref.getUrl(), () -> {
				try {
					logger.debug("{} Applying delta response {} to {} ({})", ref.getOrigin(), scriptTag, ref.getTitle(), ref.getUrl());
					scriptRunner.runScripts(ref, scriptTag);
				} catch (UntrustedScriptException e) {
					logger.error("{} Script hash not whitelisted: {}", ref.getOrigin(), e.getScriptHash());
					tagger.attachError(ref.getOrigin(), ref, "Script hash not whitelisted", e.getScriptHash());
				}
			}).join();
		}
	}

}
