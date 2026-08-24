package jasper.config;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jasper.component.ConfigCache;
import jasper.security.jwt.JWTConfigurer;
import jasper.security.jwt.TokenProvider;
import jasper.security.jwt.TokenProviderImplDefault;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import org.springframework.web.context.annotation.ApplicationScope;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.zalando.problem.spring.web.advice.security.SecurityProblemSupport;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static jasper.security.AuthoritiesConstants.ADMIN;
import static jasper.security.AuthoritiesConstants.ANONYMOUS;
import static jasper.security.AuthoritiesConstants.EDITOR;
import static jasper.security.AuthoritiesConstants.MOD;
import static jasper.security.AuthoritiesConstants.USER;
import static jasper.security.AuthoritiesConstants.VIEWER;
import static org.apache.commons.lang3.ArrayUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl.fromHierarchy;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Import(SecurityProblemSupport.class)
public class SecurityConfiguration {
	private final Logger logger = LoggerFactory.getLogger(SecurityConfiguration.class);

	@Autowired
	Props props;
	@Autowired
	ConfigCache configs;
	@Autowired
	SecurityProblemSupport problemSupport;
	@Autowired
	TokenProvider tokenProvider;

	@Autowired
	TokenProviderImplDefault defaultTokenProvider;

	@Value("${spring.profiles.active}")
	String[] profiles;

	@PostConstruct
	void init() {
		var unsafeSecret = profile("dev") && isNotBlank(props.getOverride().getSecurity().getSecret());
		if (!props.isDebug()) props.setDebug(unsafeSecret);
		if (props.isDebug()) {
			logger.warn("==================================================");
			logger.warn("==================================================");
			logger.warn("DEBUG MODE");
			logger.warn("==================================================");
			logger.warn("==================================================");
		}
		logger.info("LOCAL ORIGIN:             {}", props.getLocalOrigin());
		logger.info("WORKER ORIGIN:            {}", props.getWorkerOrigin());
		logger.info("DEFAULT ROLE:             {}", props.getDefaultRole());
		logger.info("DEFAULT READ ACCESS:      {}", isEmpty(props.getDefaultReadAccess()) ? "" : String.join(", ", props.getDefaultReadAccess()));
		logger.info("DEFAULT WRITE ACCESS:     {}", isEmpty(props.getDefaultWriteAccess()) ? "" : String.join(", ", props.getDefaultWriteAccess()));
		logger.info("DEFAULT TAG READ ACCESS:  {}", isEmpty(props.getDefaultTagReadAccess()) ? "" : String.join(", ", props.getDefaultTagReadAccess()));
		logger.info("DEFAULT TAG WRITE ACCESS: {}", isEmpty(props.getDefaultTagWriteAccess()) ? "" : String.join(", ", props.getDefaultTagWriteAccess()));
		logger.info("MAX ROLE:                 {}", props.getMaxRole());
		logger.info("MIN ROLE:                 {}", props.getMinRole());
		logger.info("MIN WRITE ROLE:           {}", props.getMinWriteRole());
		logger.info("MIN FETCH ROLE:           {}", props.getMinFetchRole());
		logger.info("MIN CONFIG ROLE:          {}", props.getMinConfigRole());
		logger.info("MIN READ BACKUPS ROLE:    {}", props.getMinReadBackupsRole());
		logger.info("AUTH HEADERS:             {}", props.isAllowAuthHeaders() ? "ENABLED" : "-");
		logger.info("USER HEADERS:             {}", props.isAllowUserTagHeader() ? "ENABLED" : "-");
		logger.info("ROLE HEADERS:             {}", props.isAllowUserRoleHeader() ? "ENABLED" : "-");
	}

	private boolean profile(String profile) {
		return Arrays.asList(profiles).contains(profile);
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // @formatter:off
        http
            .exceptionHandling(e -> e
                .authenticationEntryPoint(problemSupport)
                .accessDeniedHandler(problemSupport)
			)
            .headers(h -> h
				.contentSecurityPolicy(csp -> csp
					.policyDirectives(props.getSecurity().getContentSecurityPolicy()))
				.referrerPolicy(r -> r.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
				.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
				.permissionsPolicyHeader(p -> p.policy("camera=(), fullscreen=(self), geolocation=(), gyroscope=(), magnetometer=(), microphone=(), midi=(), payment=(), sync-xhr=()"))
			)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.with(securityConfigurerAdapter(), Customizer.withDefaults())
			.authorizeHttpRequests(r -> r
				.requestMatchers("/api/**").permitAll()
				.requestMatchers("/pub/api/**").permitAll()
				.requestMatchers("/management/**").permitAll()
			)
			.csrf(c -> c
				.csrfTokenRequestHandler(csrfRequestHandler())
				.csrfTokenRepository(csrfTokenRepository())
				.ignoringRequestMatchers("/pub/api/**") // Public API
			)
		; // @formatter:on
		return http.build();
	}

	@Bean
	public AuthenticationManager noopAuthenticationManager() {
		return authentication -> {
			throw new AuthenticationServiceException("AuthenticationManager is disabled");
		};
	}

	@Bean
	JWTConfigurer securityConfigurerAdapter() {
		return new JWTConfigurer(props, tokenProvider, defaultTokenProvider, configs);
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		var r = CookieCsrfTokenRepository.withHttpOnlyFalse();
		r.setCookieCustomizer(c -> c
			.secure(false) // Required when using SSL terminating gateway
			.build());
		return r;
	}

	@Bean
	CsrfTokenRequestAttributeHandler csrfRequestHandler() {
		CsrfTokenRequestAttributeHandler requestHandler = new CsrfTokenRequestAttributeHandler();
		// TODO: CSRF BREACH: https://docs.spring.io/spring-security/reference/5.8/migration/servlet/exploits.html
		// Opt out of deferred csrf token loading
		requestHandler.setCsrfRequestAttributeName(null);
		return requestHandler;
	}

	@Bean
	@ApplicationScope
	public RoleHierarchy roleHierarchy() {
		return fromHierarchy(String.join("\n", List.of(
			ADMIN + " > " + MOD,
			MOD + " > " + EDITOR,
			EDITOR + " > " + USER,
			USER + " > " + VIEWER,
			VIEWER + " > " + ANONYMOUS
		)));
	}

	@Bean
	public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler() {
		var expressionHandler = new DefaultMethodSecurityExpressionHandler();
		expressionHandler.setRoleHierarchy(roleHierarchy());
		return expressionHandler;
	}

	@Bean
	public Filter shallowEtagHeaderFilter() {
		return new ShallowEtagHeaderFilter() {
			@Override
			protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException, IOException {
				if (!shouldUseEtag(request, response)) {
					ShallowEtagHeaderFilter.disableContentCaching(request);
					filterChain.doFilter(request, response);
				} else {
					super.doFilterInternal(request, response, filterChain);
				}
			}

			@Override
			protected boolean isEligibleForEtag(
				HttpServletRequest request,
				HttpServletResponse response,
				int responseStatusCode,
				InputStream inputStream
			) {
				if (!shouldUseEtag(request,response)) return false;
				return super.isEligibleForEtag(request, response, responseStatusCode, inputStream);
			}

			private boolean shouldUseEtag(HttpServletRequest request,
										  HttpServletResponse response){
				return response.containsHeader(HttpHeaders.CONTENT_DISPOSITION);
			}
		};
	}
}
