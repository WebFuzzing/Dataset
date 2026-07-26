package jasper.config;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import jasper.component.ConfigCache;
import jasper.service.dto.TemplateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;

import static java.time.Duration.ofMinutes;
import static java.time.Duration.ofSeconds;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Configuration
public class BulkheadConfiguration {
	private static final Logger logger = LoggerFactory.getLogger(BulkheadConfiguration.class);

	@Autowired
	ConfigCache configs;

	@Autowired
	BulkheadRegistry registry;

	@Bean
	public Bulkhead httpBulkhead() {
		return registry.bulkhead("http", BulkheadConfig.custom()
			.maxConcurrentCalls(configs.root().getMaxConcurrentRequests())
			.maxWaitDuration(ofSeconds(0))
			.build());
	}

	@Bean
	public Bulkhead scriptBulkhead() {
		return registry.bulkhead("script", BulkheadConfig.custom()
			.maxConcurrentCalls(configs.root().getMaxConcurrentScripts())
			.maxWaitDuration(ofSeconds(60))
			.build());
	}

	@Bean
	public Bulkhead replBulkhead() {
		return registry.bulkhead("repl", BulkheadConfig.custom()
			.maxConcurrentCalls(configs.root().getMaxConcurrentReplication())
			.maxWaitDuration(ofMinutes(15))
			.build());
	}

	@Bean
	public Bulkhead fetchBulkhead() {
		return registry.bulkhead("fetch", BulkheadConfig.custom()
			.maxConcurrentCalls(configs.root().getMaxConcurrentFetch())
			.maxWaitDuration(ofMinutes(5))
			.build());
	}

	@Bean
	public Bulkhead recyclerBulkhead() {
		return registry.bulkhead("recycler", BulkheadConfig.custom()
			.maxConcurrentCalls(1)
			.maxWaitDuration(ofMinutes(0))
			.build());
	}

	@ServiceActivator(inputChannel = "templateRxChannel")
	public void handleTemplateUpdate(Message<TemplateDto> message) {
		var template = message.getPayload();
		if (isBlank(template.getTag())) return;
		if (template.getTag().startsWith("_config/server")) {
			logger.debug("Server config template updated, updating bulkhead configurations");
			updateBulkheadConfig(httpBulkhead(), configs.root().getMaxConcurrentRequests());
			updateBulkheadConfig(scriptBulkhead(), configs.root().getMaxConcurrentScripts());
			updateBulkheadConfig(replBulkhead(), configs.root().getMaxConcurrentReplication());
			updateBulkheadConfig(fetchBulkhead(), configs.root().getMaxConcurrentFetch());
		}
	}

	public static void updateBulkheadConfig(Bulkhead bulkhead, int limit) {
		bulkhead.changeConfig(BulkheadConfig
			.from(bulkhead.getBulkheadConfig())
			.maxConcurrentCalls(limit)
			.build());
	}
}
