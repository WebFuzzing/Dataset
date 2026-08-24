package jasper.component.channel;

import jasper.component.ConfigCache;
import jasper.component.Storage;
import jasper.config.Props;
import jasper.repository.UserRepository;
import jasper.service.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Profile("!kubernetes & storage")
@Component
public class TunnelServerImplFile implements TunnelServer {
	private static final Logger logger = LoggerFactory.getLogger(TunnelServerImplFile.class);
	static final String CONFIG = "config";
	static final String SECRETS = "secrets";

	@Autowired
	Props props;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ConfigCache configs;

	@Autowired
	Storage storage;

	@EventListener(ApplicationReadyEvent.class)
	public void init() {
		configs.rootUpdate(root -> {
			if (root.getSshOrigins().isEmpty()) return;
			generateHostKey();
			generateConfig();
		});
	}

	@ServiceActivator(inputChannel = "userRxChannel")
	public void handleUserUpdate(Message<UserDto> message) {
		if (configs.root().getSshOrigins().isEmpty()) return;
		var user = message.getPayload();
		if ("+user".equals(user.getTag()) && props.getLocalOrigin().equals(user.getOrigin())) {
			generateHostKey();
		}
		if (configs.root().ssh(user.getOrigin())) {
			generateConfig();
		}
	}

	public void generateHostKey() {
		logger.info("Generating new host_key");
		var hostKey = "";
		if (configs.user() == null || configs.user().getKey() != null) {
			hostKey = new String(configs.user().getKey());
		}
		try {
			if (storage.exists("", SECRETS, "host_key")) {
				storage.overwrite("", SECRETS, "host_key", hostKey.getBytes());
			} else {
				storage.storeAt("", SECRETS, "host_key", hostKey.getBytes());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void generateConfig() {
		logger.info("Generating new authorized_keys");
		var result = authorizedKeys(configs.root().getSshOrigins(), userRepository);
		try {
			if (storage.exists("", CONFIG, "authorized_keys")) {
				storage.overwrite("", CONFIG, "authorized_keys", result.toString().getBytes());
			} else {
				storage.storeAt("", CONFIG, "authorized_keys", result.toString().getBytes());
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}
