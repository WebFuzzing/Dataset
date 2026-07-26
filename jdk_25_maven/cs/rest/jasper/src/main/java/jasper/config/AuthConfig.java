package jasper.config;

import jasper.component.ConfigCache;
import jasper.management.SecurityMetersService;
import jasper.repository.RefRepository;
import jasper.security.Auth;
import jasper.security.jwt.TokenProvider;
import jasper.security.jwt.TokenProviderImpl;
import jasper.security.jwt.TokenProviderImplDefault;
import jasper.security.jwt.TokenProviderImplNop;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.ssl.SSLContexts;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.annotation.ApplicationScope;

import javax.net.ssl.HostnameVerifier;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

@Configuration
public class AuthConfig {

	@Bean("authSingleton")
	@ApplicationScope
	public Auth authSingleton(Props props, RoleHierarchy roleHierarchy, ConfigCache configs, RefRepository refRepository) {
		return new Auth(props, roleHierarchy, configs, refRepository);
	}

	@Bean
	@Profile("!no-ssl")
	RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	@Profile("no-ssl")
	RestTemplate restTemplateBypassSSL() throws KeyStoreException, NoSuchAlgorithmException, KeyManagementException {
		TrustStrategy acceptingTrustStrategy = (X509Certificate[] chain, String authType) -> true;
		HostnameVerifier hostnameVerifier = (s, sslSession) -> true;
		var sslContext = SSLContexts.custom().loadTrustMaterial(null, acceptingTrustStrategy).build();
		var csf = new SSLConnectionSocketFactory(sslContext, hostnameVerifier);
		var cm = PoolingHttpClientConnectionManagerBuilder.create()
				.setSSLSocketFactory(csf)
				.build();
		var httpClient = HttpClients.custom().setConnectionManager(cm).build();
		var requestFactory = new HttpComponentsClientHttpRequestFactory();
		requestFactory.setHttpClient(httpClient);
		return new RestTemplate(requestFactory);
	}

	@Primary
	@Bean
	@Profile("jwt")
	TokenProvider tokenProvider(Props props, ConfigCache configs, SecurityMetersService securityMetersService, RestTemplate restTemplate) {
		return new TokenProviderImpl(props, configs, securityMetersService, restTemplate);
	}

	@Primary
	@Bean
	@ConditionalOnMissingBean
	TokenProvider fallbackTokenProvider(Props props, ConfigCache configs) {
		return new TokenProviderImplNop(props, configs);
	}

	@Bean
	TokenProviderImplDefault defaultTokenProvider(Props props, ConfigCache configs) {
		return new TokenProviderImplDefault(props, configs);
	}
}
