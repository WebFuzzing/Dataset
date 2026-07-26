package jasper.component.cron;

import jakarta.annotation.PostConstruct;
import jasper.component.Replicator;
import jasper.domain.Ref;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static jasper.domain.proj.HasOrigin.formatOrigin;
import static jasper.domain.proj.HasOrigin.subOrigin;
import static jasper.plugin.Origin.getOrigin;

@Component("cronPull")
public class Pull implements Cron.CronRunner  {
	private static final Logger logger = LoggerFactory.getLogger(Pull.class);

	@Autowired
	Cron cron;

	@Autowired
	Replicator replicator;

	@PostConstruct
	void init() {
		cron.addCronTag("+plugin/origin/pull", this);
	}

	public void run(Ref remote) {
		var config = getOrigin(remote);
		var localOrigin = subOrigin(remote.getOrigin(), config.getLocal());
		logger.info("{} Pulling origin ({}) {}: {}", remote.getOrigin(), formatOrigin(localOrigin), remote.getTitle(), remote.getUrl());
		replicator.pull(remote);
		logger.info("{} Finished pulling origin ({}) {}: {}", remote.getOrigin(), formatOrigin(localOrigin), remote.getTitle(), remote.getUrl());
	}

}
