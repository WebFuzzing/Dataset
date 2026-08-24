package jasper.component.channel;

import jasper.component.ConfigCache;
import jasper.config.Props;
import jasper.service.dto.PluginDto;
import jasper.service.dto.TemplateDto;
import jasper.service.dto.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class ClearConfigCache {

	@Autowired
	Props props;

	@Autowired
	TaskScheduler taskScheduler;

	@Autowired
	ConfigCache configs;

	private AtomicBoolean clearingConfig = new AtomicBoolean(false);
	private AtomicBoolean clearConfigAgain = new AtomicBoolean(false);

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@ServiceActivator(inputChannel = "tagRxChannel")
	public void handleTagUpdate(Message<String> message) {
		if (!configs.isConfigTag((String) message.getHeaders().get("tag"))) return;
		if (clearingConfig.compareAndSet(false, true)) {
			clearConfig();
		} else {
			clearConfigAgain.set(true);
		}
	}

	private void checkIfClearingAgain() {
		if (!clearingConfig.get()) return;
		var next = clearConfigAgain.getAndSet(false);
		clearingConfig.set(next);
		if (next) clearConfig();
	}

	private void clearConfig() {
		configs.clearConfigCache();
		taskScheduler.schedule(this::checkIfClearingAgain, Instant.now().plusMillis(props.getClearCacheCooldownSec() * 1000L));
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@ServiceActivator(inputChannel = "userRxChannel")
	public void handleUserUpdate(Message<UserDto> message) {
		// TODO: clear caches by origin
		configs.clearUserCache();
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@ServiceActivator(inputChannel = "pluginRxChannel")
	public void handlePluginUpdate(Message<PluginDto> message) {
		// TODO: clear caches by origin
		configs.clearPluginCache();
	}

	@Order(Ordered.HIGHEST_PRECEDENCE)
	@ServiceActivator(inputChannel = "templateRxChannel")
	public void handleTemplateUpdate(Message<TemplateDto> message) {
		// TODO: clear caches by origin
		configs.clearTemplateCache();
	}
}
