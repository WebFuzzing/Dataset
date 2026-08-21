package jasper.component;

import jakarta.annotation.PreDestroy;
import jasper.config.Props;
import jasper.security.Auth;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.ScopeNotActiveException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import static jasper.domain.proj.HasOrigin.formatOrigin;

@Component
public class HttpClientFactory {
	private static final Logger logger = LoggerFactory.getLogger(HttpClientFactory.class);

	@Autowired
	Props props;

	@Autowired
	Auth auth;

	record PoolKey(String tenantId, boolean serial) {}
	private final Map<PoolKey, PoolingHttpClientConnectionManager> managers = new ConcurrentHashMap<>();
	private final Map<PoolKey, CloseableHttpClient> clients = new ConcurrentHashMap<>();

	@Scheduled(fixedDelay = 30, initialDelay = 1, timeUnit = TimeUnit.MINUTES)
	public void logStats() {
		for (var entry : managers.entrySet()) {
			var manager = entry.getValue();
			var stats = manager.getTotalStats();
			logger.info("HTTP Connection Pool: {} ({} {}): Leased={}, Available={}, Pending={}, Max={}",
				formatOrigin(entry.getKey().tenantId),
				entry.getKey().serial() ? "serial" : "parallel",
				manager.getDefaultMaxPerRoute(),
				stats.getLeased(),
				stats.getAvailable(),
				stats.getPending(),
				stats.getMax());
			manager.getRoutes().forEach(route -> {
				var routeStats = manager.getStats(route);
				if (routeStats.getLeased() > 0) {
					logger.debug("{}  Route {}: Leased={}, Available={}",
						formatOrigin(entry.getKey().tenantId),
						route,
						routeStats.getLeased(),
						routeStats.getAvailable());
				}
			});
		}
	}

	@Scheduled(fixedDelay = 30, timeUnit = TimeUnit.SECONDS)
	public void evictIdleConnections() {
		managers.values().forEach(manager -> {
			manager.closeExpiredConnections();
			manager.closeIdleConnections(30, TimeUnit.SECONDS);
		});
	}

	public CloseableHttpClient getSerialClient() {
		return getOrCreateClientForTenant(getCurrentTenant(), true);
	}

	public CloseableHttpClient getClient() {
		return getOrCreateClientForTenant(getCurrentTenant(), false);
	}

	private String getCurrentTenant() {
		try {
			return auth.getOrigin();
		} catch (ScopeNotActiveException e) {
			return props.getOrigin();
		}
	}

	private CloseableHttpClient getOrCreateClientForTenant(String tenantId, boolean serial) {
		var key = new PoolKey(tenantId, serial);
		return clients.computeIfAbsent(key, id -> {
			var cm = managers.computeIfAbsent(id, tid -> {
				var manager = new PoolingHttpClientConnectionManager();
				manager.setMaxTotal(100);
				manager.setDefaultMaxPerRoute(serial ? 1 : 4);
				return manager;
			});

			return HttpClients.custom()
				.setConnectionManager(cm)
				.setConnectionManagerShared(true)
				.setDefaultRequestConfig(RequestConfig.custom()
					.setConnectTimeout(5 * 60 * 1000)
					.setConnectionRequestTimeout(30 * 1000)
					.setSocketTimeout(5 * 60 * 1000)
					.build())
				.disableCookieManagement()
				.disableConnectionState()
				.build();
		});
	}

	@PreDestroy
	public void cleanup() {
		clients.values().forEach(IOUtils::closeQuietly);
		managers.values().forEach(PoolingHttpClientConnectionManager::close);
	}
}
