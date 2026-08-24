package jasper.component.channel;

import jasper.domain.Ref_;
import jasper.domain.proj.HasTags;
import jasper.repository.RefRepository;
import jasper.repository.filter.RefFilter;
import jasper.service.dto.RefDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static jasper.domain.proj.HasOrigin.origin;
import static jasper.domain.proj.HasTags.hasMatchingTag;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.data.domain.Sort.by;

@Component()
public class Watch {
	private static final Logger logger = LoggerFactory.getLogger(Watch.class);

	@Autowired
	RefRepository refRepository;

	Map<String, Map<String, Watcher>> watchers = new ConcurrentHashMap<>();
	Map<String, Map<String, Set<String>>> watching = new ConcurrentHashMap<>();

	/**
	 * Register a watcher to watch all Refs in an origin.
	 */
	public void addWatch(String origin, Watcher w) {
		watchers.computeIfAbsent(origin, o -> new ConcurrentHashMap<>()).put("", w);
	}

	/**
	 * Register a watcher for all tagged Refs in an origin.
	 */
	public void addWatch(String origin, String tag, Watcher w) {
		watchers.computeIfAbsent(origin, o -> new ConcurrentHashMap<>()).put(tag, w);
	}

	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		try {
			for (var origin : watchers.keySet()) {
				for (var tag : watchers.get(origin).keySet()) {
					if (isBlank(tag)) continue;
					Instant lastModified = null;
					while (true) {
						var maybeRef = refRepository.findAll(RefFilter.builder()
							.origin(origin)
							.query(tag)
							.modifiedAfter(lastModified)
							.build().spec(), PageRequest.of(0, 1, by(Ref_.MODIFIED)));
						if (maybeRef.isEmpty()) break;
						var ref = maybeRef.getContent().getFirst();
						lastModified = ref.getModified();
						try {
							watchers.get(origin).get(tag).notify(ref);
						} catch (Exception e) {
							logger.warn("Error starting watcher", e);
						}
					}
				}
			}
		} catch (Exception e) {
			logger.error("Error serializing watchers", e);
		}
	}

	@ServiceActivator(inputChannel = "refRxChannel")
	public void handleRefUpdate(Message<RefDto> message) {
		var origin = origin(message.getHeaders().get("origin").toString());
		if (!watchers.containsKey(origin)) return;
		var ref = message.getPayload();
		for (var tag : watchers.get(origin).keySet()) {
			var set = watching
				.computeIfAbsent(origin, o -> new ConcurrentHashMap<>())
				.computeIfAbsent(tag, t -> ConcurrentHashMap.newKeySet());
			if (isNotBlank(tag)) {
				if (!set.contains(ref.getUrl())) {
					if (!hasMatchingTag(ref, tag)) continue;
					set.add(ref.getUrl());
				}
			}
			try {
				watchers.get(origin).get(tag).notify(ref);
			} finally {
				if (isNotBlank(tag) && !hasMatchingTag(ref, tag)) {
					set.remove(ref.getUrl());
				}
			}
		}
	}

	public interface Watcher {
		void notify(HasTags ref);
	}

}
