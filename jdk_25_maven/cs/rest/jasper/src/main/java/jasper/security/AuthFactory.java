package jasper.security;

import jasper.component.ConfigCache;
import jasper.config.Props;
import jasper.repository.RefRepository;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.stereotype.Component;

@Component
public class AuthFactory {
	private final Props props;
	private final RoleHierarchy roleHierarchy;
	private final ConfigCache configs;
	private final RefRepository refRepository;

	public AuthFactory(Props props, RoleHierarchy roleHierarchy, ConfigCache configs, RefRepository refRepository) {
		this.props = props;
		this.roleHierarchy = roleHierarchy;
		this.configs = configs;
		this.refRepository = refRepository;
	}

	public Auth create() {
		return new Auth(props, roleHierarchy, configs, refRepository);
	}
}
