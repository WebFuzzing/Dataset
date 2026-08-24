package jasper.component.channel;

import jakarta.websocket.DeploymentException;
import jasper.component.ConfigCache;
import jasper.component.Replicator;
import jasper.component.ScriptExecutorFactory;
import jasper.component.Tagger;
import jasper.component.TunnelClient;
import jasper.domain.proj.HasTags;
import jasper.errors.RetryableTunnelException;
import jasper.repository.RefRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static jasper.domain.proj.HasOrigin.formatOrigin;
import static jasper.domain.proj.HasOrigin.origin;
import static jasper.domain.proj.HasOrigin.subOrigin;
import static jasper.domain.proj.HasTags.hasMatchingTag;
import static jasper.plugin.Origin.getOrigin;
import static jasper.plugin.Pull.getPull;
import static jasper.util.Logging.getMessage;

@Component
public class Pull {
	private static final Logger logger = LoggerFactory.getLogger(Pull.class);

	@Autowired
	TaskScheduler taskScheduler;

	@Autowired
	ScriptExecutorFactory scriptExecutorFactory;

	@Autowired
	ConfigCache configs;

	@Autowired
	RefRepository refRepository;

	@Autowired
	TunnelClient tunnelClient;

	@Autowired
	Watch watch;

	@Autowired
	Replicator replicator;

	@Autowired
	Tagger tagger;

	private final ConcurrentHashMap<String, Boolean> isPulling = new ConcurrentHashMap<>();

	record MonitorInfo(String url, String origin, WebSocketStompClient client, String proxy, AtomicBoolean connected) {}
	private Map<String, MonitorInfo> pulls = new ConcurrentHashMap<>();

	record RetryInfo(int count, boolean retrying) {}
	private Map<String, RetryInfo> retryCounts = new ConcurrentHashMap<>();
	private final int BASE_BACKOFF_SECONDS = 5;
	private final int MAX_BACKOFF_SECONDS = 300; // max backoff delay

	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		configs.rootUpdate(root -> {
			for (var origin : root.scriptOrigins("+plugin/origin/pull")) {
				watch.addWatch(origin, "+plugin/origin/pull", this::watch);
			}
		});
	}

	@Scheduled(fixedDelay = 30, initialDelay = 10, timeUnit = TimeUnit.MINUTES)
	public void log() {
		for (var e : pulls.entrySet()) {
			logger.info("Websocket Monitor[{}]: {} ({}): {}",
				e.getValue().proxy, e.getKey(), e.getValue().url, e.getValue().connected.get() ? "CONNECTED" : "DISCONNECTED");
		}
	}

	private void watch(HasTags update) {
		if (!configs.root().script("+plugin/origin/pull", update.getOrigin())) return;
		var remote = refRepository.findOneByUrlAndOrigin(update.getUrl(), update.getOrigin()).orElse(null);
		var config = getOrigin(remote);
		var pull = getPull(remote);
		if (remote == null || config == null) {
			for (var e : pulls.entrySet()) {
				if (e.getValue().url.equals(update.getUrl()) && e.getValue().origin.equals(update.getOrigin())) {
					logger.info("{} Disconnecting origin ({}) from websocket {}: {}", update.getOrigin(), e.getKey(), update.getTitle(), update.getUrl());
					pulls.remove(e.getKey());
					e.getValue().client.stop();
					return;
				}
			}
			return;
		}
		var localOrigin = subOrigin(remote.getOrigin(), config.getLocal());
		var remoteOrigin = origin(config.getRemote());
		pulls.compute(localOrigin, (o, info) -> {
			if (hasMatchingTag(update, "plugin/delete") || remote.hasTag("+plugin/error") || !remote.hasTag("+plugin/origin/pull") || !remote.hasTag("+plugin/cron") || !pull.isWebsocket()) {
				if (info != null) {
					logger.info("{} Disconnecting origin ({}) from websocket {}: {}", remote.getOrigin(), formatOrigin(localOrigin), remote.getTitle(), remote.getUrl());
					info.client.stop();
					tunnelClient.releaseProxy(remote);
				}
				return null;
			}
			if (info != null) {
				if (info.connected.get()) return info;
				logger.error("{} Restarting monitor ({}) from websocket {}: {}", remote.getOrigin(), formatOrigin(localOrigin), remote.getTitle(), remote.getUrl());
				tunnelClient.releaseProxy(remote);
			}
			URI url;
			try {
				url = tunnelClient.reserveProxy(remote);
			} catch (RetryableTunnelException e) {
				logger.info("{} Error connecting to ({}) via ssh {}: {}", remote.getOrigin(), formatOrigin(localOrigin), remote.getTitle(), remote.getUrl());
				scheduleReconnect(update, localOrigin);
				return null;
			} catch (Exception e) {
				logger.error("{} Error connecting to ({}) via ssh {}: {}", remote.getOrigin(), formatOrigin(localOrigin), remote.getTitle(), remote.getUrl());
				return null;
			}
			logger.info("{} Monitoring origin ({}) via websocket {}: {}", remote.getOrigin(), formatOrigin(localOrigin), remote.getTitle(), remote.getUrl());
			var stomp = getWebSocketStompClient();
			stomp.setDefaultHeartbeat(new long[]{10000, 10000});
			stomp.setTaskScheduler(taskScheduler);
			var handler = new StompSessionHandlerAdapter() {
				@Override
				public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
					pulls.get(localOrigin).connected.set(true);
					retryCounts.remove(localOrigin);
					handleCursorUpdate(remote.getOrigin(), localOrigin, null);
					session.subscribe("/topic/cursor/" + formatOrigin(remoteOrigin), new StompFrameHandler() {
						@Override
						public Type getPayloadType(StompHeaders headers) {
							return Instant.class;
						}

						@Override
						public void handleFrame(StompHeaders headers, Object payload) {
							handleCursorUpdate(remote.getOrigin(), localOrigin, (Instant) payload);
						}
					});
				}

				@Override
				public void handleTransportError(StompSession session, Throwable exception) {
					pulls.get(localOrigin).connected.set(false);
					logger.debug("{} Websocket Client Transport error: {}", remote.getOrigin(), exception.getMessage());
					stomp.stop();
					scheduleReconnect(update, localOrigin);
				}

				@Override
				public void handleException(StompSession session, StompCommand command,
											StompHeaders headers, byte[] payload, Throwable exception) {
					logger.error("{} Error in websocket connection", remote.getOrigin(), exception);
					// Will automatically reconnect due to SockJS
				}
			};
			try {
				var future = stomp.connectAsync(url.resolve("/api/stomp/").toString(), handler);
				scriptExecutorFactory.run("_plugin/websocket", remote.getOrigin(), () -> future.thenAcceptAsync(session -> {
					// TODO: add plugin response to origin to show connection status
					logger.info("{} Connected to ({}) via websocket {}: {}", remote.getOrigin(), formatOrigin(localOrigin), remote.getTitle(), remote.getUrl());
				}).exceptionally(e -> {
					logger.error("{} Error creating websocket session: {}", remote.getOrigin(), e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
					stomp.stop();
					if (e instanceof DeploymentException) return null;
					scheduleReconnect(update, localOrigin);
					return null;
				}));
			} catch (Exception e) {
				logger.error("{} Error creating websocket session: {}", remote.getOrigin(), e.getCause() == null ? e.getMessage() : e.getCause().getMessage());
				stomp.stop();
				tunnelClient.releaseProxy(remote);
				if (e instanceof DeploymentException) return null;
				scheduleReconnect(update, localOrigin);
				return null;
			}
			return new MonitorInfo(remote.getUrl(), remote.getOrigin(), stomp, url.toString(), new AtomicBoolean());
		});
	}

	private void handleCursorUpdate(String origin, String local, Instant cursor) {
		if (!configs.root().script("+plugin/origin/pull", origin)) return;
		pulls.compute(local, (k, info) -> {
			if (info == null) return null;
			var maybeRemote = refRepository.findOneByUrlAndOrigin(info.url, info.origin);
			if (maybeRemote.isPresent()) {
				var remote = maybeRemote.get();
				if (remote.getPluginResponses("+plugin/user/run") == 0 && isPulling.putIfAbsent(local, true) == null) {
					taskScheduler.schedule(() -> {
						var config = getOrigin(remote);
						var localOrigin = subOrigin(remote.getOrigin(), config.getLocal());
						logger.debug("{} Pulling origin from monitor ({}) {}: {}", remote.getOrigin(), formatOrigin(localOrigin), remote.getTitle(), remote.getUrl());
						try {
							replicator.pull(remote);
							logger.debug("{} Finished pulling origin from monitor ({}) {}: {}", remote.getOrigin(), formatOrigin(localOrigin), remote.getTitle(), remote.getUrl());
						} catch (Exception e) {
							logger.error("{} Error pulling origin from monitor ({}) {}: {}", remote.getOrigin(), formatOrigin(localOrigin), remote.getTitle(), remote.getUrl());
							tagger.attachError(remote.getUrl(), origin, "Error pulling", getMessage(e));
						} finally {
							isPulling.remove(local);
						}
					}, Instant.now());
				}
				return info;
			}
			return null;
		});
	}

	private void scheduleReconnect(HasTags update, String localOrigin) {
		retryCounts.compute(localOrigin, (k, info) -> {
			if (info == null) info = new RetryInfo(0, false);
			if (info.retrying) return info;
			var count = info.count + 1;
			int delaySec = Math.min(BASE_BACKOFF_SECONDS * (1 << count), MAX_BACKOFF_SECONDS);
			logger.info("{} Scheduling reconnect for {} in {} seconds (retry count: {})",
				update.getOrigin(), localOrigin, delaySec, count);
			taskScheduler.schedule(() -> {
				retryCounts.put(localOrigin, new RetryInfo(count, false));
				watch(update);
			}, Instant.now().plus(delaySec, ChronoUnit.SECONDS));
			return new RetryInfo(count, true);
		});
	}

	private WebSocketStompClient getWebSocketStompClient() {
		var socks = new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient())));
		socks.setConnectTimeoutScheduler(taskScheduler);
		var stomp = new WebSocketStompClient(socks);
		stomp.setMessageConverter(new MessageConverter() {
			@Override
			public Object fromMessage(Message<?> message, Class<?> targetClass) {
				return Instant.parse(new String((byte[]) message.getPayload(), StandardCharsets.UTF_8));
			}

			@Override
			public Message<?> toMessage(Object payload, MessageHeaders headers) {
				return MessageBuilder.createMessage(payload, headers);
			}
		});
		return stomp;
	}
}
