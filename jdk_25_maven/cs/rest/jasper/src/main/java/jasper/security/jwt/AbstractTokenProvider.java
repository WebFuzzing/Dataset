package jasper.security.jwt;

import jasper.component.ConfigCache;
import jasper.config.Props;
import jasper.domain.User;
import jasper.errors.UserTagInUseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static jasper.domain.User.ROLES;
import static jasper.security.Auth.USER_ROLE_HEADER;
import static jasper.security.Auth.getHeader;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public abstract class AbstractTokenProvider implements TokenProvider {
	private final Logger logger = LoggerFactory.getLogger(AbstractTokenProvider.class);

	public Props props;

	public ConfigCache configs;

	AbstractTokenProvider(Props props, ConfigCache configs) {
		this.props = props;
		this.configs = configs;
	}

	User getUser(String userTag, Claims claims, String origin) {
		var user = configs.getUser(userTag + origin);
		var security = configs.security(origin);
		if (security.isExternalId()) {
			var email = claims.getString(security.getUsernameClaim());
			if (user == null) return configs.createUser(userTag, origin, email);
			if (user.hasExternalId() && configs.getUserByExternalId(origin, email).isEmpty()) {
				// There is no explicit mapping for `email`, but `user` has an explicit mapping,
				// Therefore, the current `userTag` cannot be implicitly mapped to `user`
				logger.warn("{} External ID {} already mapped to user {}", origin, email, userTag);
				throw new UserTagInUseException();
			}
			if (!user.hasExternalId(email)) {
				// After a user is implicitly mapped for the first time, save the external ID to make it explicit
				configs.setExternalId(userTag, origin, email);
			}
		}
		return user;
	}

	Collection<? extends GrantedAuthority> getAuthorities(User user, String origin) {
		var auth = getPartialAuthorities(origin);
		if (user != null && user.getRole() != null) {
			logger.debug("{} User Roles: {}", origin, user.getRole());
			if (ROLES.contains(user.getRole().trim())) {
				auth.add(new SimpleGrantedAuthority(user.getRole().trim()));
			}
		} else {
			logger.debug("No User");
		}
		return auth;
	}

	List<SimpleGrantedAuthority> getPartialAuthorities(String origin) {
		var auth = new ArrayList<SimpleGrantedAuthority>();
		auth.add(new SimpleGrantedAuthority(props.getDefaultRole()));
		auth.add(new SimpleGrantedAuthority(configs.security(origin).getDefaultRole()));
		var roleHeader = getHeader(USER_ROLE_HEADER);
		if (props.isAllowUserRoleHeader() && isNotBlank(roleHeader)) {
			logger.debug("{} Header Roles: {}", origin, roleHeader);
			for (var role : roleHeader.trim().trim().split(",")) {
				if (ROLES.contains(role.trim())) {
					auth.add(new SimpleGrantedAuthority(role.trim()));
				}
			}
		}
		return auth;
	}
}
