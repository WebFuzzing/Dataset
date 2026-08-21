package jasper.component.cron;

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
public class Script implements Cron.CronRunner {
	private static final Logger logger = LoggerFactory.getLogger(Script.class);

	@Autowired
	ConfigCache config;

	@Autowired
	Cron cron;

	@Autowired
	ScriptExecutorFactory scriptExecutorFactory;

	@Autowired
	ScriptRunner scriptRunner;

	@Autowired
	Tagger tagger;

	@PostConstruct
	void init() {
		cron.addCronTag("plugin/script", this);
		cron.addCronTag("+plugin/script", this);
		cron.addCronTag("_plugin/script", this);
	}

	@Override
	@Bulkhead(name = "script")
	public void run(Ref ref) throws Exception {
		logger.debug("{} Searching scripts for {} ({})", ref.getOrigin(), ref.getTitle(), ref.getUrl());
		var tags = ref.getExpandedTags().stream()
			.filter(t -> !publicTag(t).equals("plugin/script"))
			.filter(t -> matchesTag("plugin/script", publicTag(t)))
			.filter(t -> config.root().script(t, ref.getOrigin()))
			.sorted()
			.toList()
			.reversed();
		for (var scriptTag : tags) {
			scriptExecutorFactory.run(scriptTag, ref.getOrigin(), ref.getUrl(), () -> {
				try {
					logger.debug("{} Running script {} on {} ({})", ref.getOrigin(), scriptTag, ref.getTitle(), ref.getUrl());
					scriptRunner.runScripts(ref, scriptTag);
				} catch (UntrustedScriptException e) {
					logger.error("{} Script hash not whitelisted: {}", ref.getOrigin(), e.getScriptHash());
					tagger.attachError(ref.getOrigin(), ref, "Script hash not whitelisted", e.getScriptHash());
				}
			}).join();
		}
	}

}
