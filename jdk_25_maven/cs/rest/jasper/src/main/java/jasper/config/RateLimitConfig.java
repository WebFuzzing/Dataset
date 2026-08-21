package jasper.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jasper.component.ConfigCache;
import jasper.security.Auth;
import jasper.service.dto.TemplateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import static java.lang.String.format;
import static java.time.Duration.ofNanos;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Configuration
public class RateLimitConfig {
	private static final Logger logger = LoggerFactory.getLogger(RateLimitConfig.class);

	@Autowired
	ConfigCache configs;

	@Autowired
	Auth auth;

	@Autowired
	Bulkhead httpBulkhead;

	private final ConcurrentHashMap<String, RateLimiter> originRateLimiters = new ConcurrentHashMap<>();
	private RateLimiter getOriginRateLimiter(String origin) {
		return originRateLimiters.computeIfAbsent(origin, k -> RateLimiter.of("http-" + origin, RateLimiterConfig.custom()
				.limitForPeriod(configs.security(origin).getMaxRequests())
				.limitRefreshPeriod(ofNanos(500))
				.build()));
	}

	@ServiceActivator(inputChannel = "templateRxChannel")
	public void handleTemplateUpdate(Message<TemplateDto> message) {
		var template = message.getPayload();
		if (isBlank(template.getTag())) return;
		if (template.getTag().startsWith("_config/server")) {
			httpRateLimiter().changeLimitForPeriod(configs.root().getMaxConcurrentRequests());
		}
		if (template.getTag().startsWith("_config/security")) {
			originRateLimiters.forEach((origin, r) -> r.changeLimitForPeriod(configs.security(origin).getMaxRequests()));
		}
	}

	@Bean
	RateLimiter httpRateLimiter() {
		return RateLimiter.of("http", RateLimiterConfig.custom()
				.limitForPeriod(configs.root().getMaxConcurrentRequests())
				.limitRefreshPeriod(ofNanos(500))
				.build());
	}

	@Bean
	public Filter rateLimitInterceptor(RateLimiter httpRateLimiter) {
		return new GenericFilterBean() {
			@Override
			public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
				HttpServletRequest httpRequest = (HttpServletRequest) request;
				HttpServletResponse httpResponse = (HttpServletResponse) response;
				var origin = auth.getOrigin();
				if (!getOriginRateLimiter(origin).acquirePermission()) {
					RateLimitConfig.logger.debug("{} Rate limit exceeded for origin: {}", origin, httpRequest.getRequestURI());
					httpResponse.setStatus(429);
					httpResponse.setHeader("X-RateLimit-Limit", ""+configs.security(origin).getMaxRequests());
					// Add random jitter from 3.5 to 4.5 seconds to prevent thundering herd
					httpResponse.setHeader("X-RateLimit-Retry-After", format("%.1f", ThreadLocalRandom.current().nextDouble(3.5, 4.5)));
					return;
				}
				if (!httpRateLimiter.acquirePermission()) {
					RateLimitConfig.logger.debug("HTTP rate limit exceeded for request: {}", httpRequest.getRequestURI());
					httpResponse.setStatus(429);
					// Add random jitter from 3.5 to 4.5 seconds to prevent thundering herd
					httpResponse.setHeader("X-RateLimit-Retry-After", format("%.1f", ThreadLocalRandom.current().nextDouble(3.5, 4.5)));
					return;
				}
				try {
					httpBulkhead.executeCheckedSupplier(() -> {
						chain.doFilter(request, response);
						return null;
					});
				} catch (BulkheadFullException e) {
					RateLimitConfig.logger.debug("HTTP concurrent limit exceeded for request: {}", httpRequest.getRequestURI());
					httpResponse.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE); // 503
					// Add random jitter from 3.5 to 4.5 seconds to prevent thundering herd
					httpResponse.setHeader("X-RateLimit-Retry-After", format("%.1f", ThreadLocalRandom.current().nextDouble(3.5, 4.5)));
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			}
		};
	}
}
