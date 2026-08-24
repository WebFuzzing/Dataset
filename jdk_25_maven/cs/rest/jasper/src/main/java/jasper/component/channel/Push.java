package jasper.component.channel;

import jasper.component.ConfigCache;
import jasper.component.Replicator;
import jasper.component.Tagger;
import jasper.config.Props;
import jasper.domain.proj.HasTags;
import jasper.repository.RefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static jasper.domain.proj.HasOrigin.formatOrigin;
import static jasper.domain.proj.HasOrigin.origin;
import static jasper.domain.proj.HasOrigin.subOrigin;
import static jasper.plugin.Origin.getOrigin;
import static jasper.plugin.Push.getPush;
import static jasper.util.Logging.getMessage;

@Component
public class Push {
	private static final Logger logger = LoggerFactory.getLogger(Push.class);

	@Autowired
	Props props;

	@Autowired
	TaskScheduler taskScheduler;

	@Autowired
	ConfigCache configs;

	@Autowired
	RefRepository refRepository;

	@Autowired
	Replicator replicator;

	@Autowired
	Tagger tagger;

	@Autowired
	Watch watch;

	record Remote(String url, String origin) {}
	private Map<String, Instant> lastSent = new ConcurrentHashMap<>();
	private Map<String, Instant> queued = new ConcurrentHashMap<>();
	private Map<String, Set<Remote>> pushes = new ConcurrentHashMap<>();

	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		for (var origin : configs.root().scriptOrigins("+plugin/origin/push")) {
			// TODO: redo on template change
			watch.addWatch(origin, "+plugin/origin/push", this::watch);
		}
	}

	private void watch(HasTags update) {
		if (!configs.root().script("+plugin/origin/push", update.getOrigin())) return;
		var remote = refRepository.findOneByUrlAndOrigin(update.getUrl(), update.getOrigin())
			.orElseThrow();
		var config = getOrigin(remote);
		var localOrigin = subOrigin(remote.getOrigin(), config.getLocal());
		var push = getPush(remote);
		var target = new Remote(remote.getUrl(), remote.getOrigin());
		pushes.values().forEach(set -> set.remove(target));
		if (remote.hasTag("+plugin/origin/push") && !remote.hasTag("+plugin/error") && remote.hasTag("+plugin/cron") && push.isPushOnChange()) {
			pushes
				.computeIfAbsent(localOrigin, o -> ConcurrentHashMap.newKeySet())
				.add(target);
		}
	}

	@ServiceActivator(inputChannel = "cursorRxChannel")
	public void handleCursorUpdate(Message<Instant> message) {
		var root = configs.root();
		var origin = origin(message.getHeaders().get("origin").toString());
		if (!root.script("+plugin/origin/push", origin)) return;
		var cursor = message.getPayload();
		if (cursor.equals(lastSent.computeIfAbsent(origin, o -> cursor))) {
			taskScheduler.schedule(() -> push(origin), Instant.now());
		} else {
			queued.put(origin, cursor);
		}
	}

	private void push(String origin) {
		try {
			if (pushes.containsKey(origin)) {
				var deleted = new HashSet<Remote>();
				pushes.get(origin).forEach(target -> {
					var remote = refRepository.findOneByUrlAndOrigin(target.url, target.origin).orElse(null);
					if (remote != null && !remote.hasTag("+plugin/error")) {
						logger.info("{} Pushing origin ({}) on change {}: {}", remote.getOrigin(), formatOrigin(origin), remote.getTitle(), remote.getUrl());
						try  {
							replicator.push(remote);
							logger.info("{} Finished pushing origin ({}) on change {}: {}", remote.getOrigin(), formatOrigin(origin), remote.getTitle(), remote.getUrl());
						} catch (Exception e) {
							logger.error("{} Error pushing origin ({}) on change {}: {}", remote.getOrigin(), formatOrigin(origin), remote.getTitle(), remote.getUrl());
							tagger.attachError(remote.getUrl(), origin, "Error pushing", getMessage(e));
						}
					} else {
						deleted.add(target);
					}
				});
				deleted.forEach(remote -> pushes.values().forEach(set -> set.remove(remote)));
			}
		} finally {
			taskScheduler.schedule(() -> checkIfQueued(origin), Instant.now().plus(props.getPushCooldownSec(), ChronoUnit.SECONDS));
		}
	}

	private void checkIfQueued(String origin) {
		if (!lastSent.containsKey(origin)) return;
		var next = queued.remove(origin);
		if (next != null) {
			lastSent.put(origin, next);
			push(origin);
		} else {
			lastSent.remove(origin);
		}
	}

}
