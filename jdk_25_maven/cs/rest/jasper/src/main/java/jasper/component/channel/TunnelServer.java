package jasper.component.channel;

import jasper.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static jasper.domain.proj.HasOrigin.nesting;
import static jasper.repository.spec.OriginSpec.isOrigin;
import static jasper.repository.spec.UserSpec.hasAuthorizedKeys;
import static org.apache.commons.lang3.StringUtils.isBlank;

public interface TunnelServer {
	Logger logger = LoggerFactory.getLogger(TunnelServer.class);

	void generateHostKey();
	void generateConfig();

	default String authorizedKeys(List<String> origins, UserRepository userRepository) {
		var result = new StringBuilder();
		for (var origin : origins) {
			result
				.append("\n# ")
				.append(isBlank(origin) ? "default" : origin)
				.append("\n");
			for (var u : userRepository.findAll(hasAuthorizedKeys().and(isOrigin(origin)))) {
				if (isBlank(u.getAuthorizedKeys())) continue;
				if (nesting(u.getOrigin()) > nesting(origin)) continue;
				logger.debug("Enabling SSH access for {}", u.getQualifiedTag());
				var lines = u.getAuthorizedKeys().split("\n");
				for (var l : lines) {
					if (isBlank(l)) continue;
					var parts = l.split("\\s+");
					result
						.append(parts[0])
						.append(" ")
						.append(parts[1])
						.append(" ")
						.append(u.getQualifiedTag())
						.append("\n");
				}
			}
		}
		return result.toString();
	}
}
