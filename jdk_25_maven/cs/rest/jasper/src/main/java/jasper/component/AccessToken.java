package jasper.component;

import jasper.client.TokenClient;
import jasper.security.Auth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import java.net.URI;
import java.net.URISyntaxException;

@Component
@RequestScope
public class AccessToken {

	@Autowired
	Auth auth;

	@Autowired
	TokenClient tokenClient;

	private String adminToken;

	public String getAdminToken() {
		if (adminToken == null) {
			adminToken = tokenClient.tokenService(baseUri(), getClientId(), getSecret(), "admin").getAccess_token();
		}
		return adminToken;
	}

	private URI baseUri() {
		try {
			return new URI(auth.security().getTokenEndpoint());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private String getSecret() {
		return auth.security().getSecret();
	}

	private String getClientId() {
		return auth.security().getClientId();
	}
}
