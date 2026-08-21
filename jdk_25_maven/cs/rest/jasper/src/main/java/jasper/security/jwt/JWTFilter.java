package jasper.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jasper.component.ConfigCache;
import jasper.config.Props;
import jasper.domain.proj.HasOrigin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;

import static jasper.domain.proj.HasOrigin.isSubOrigin;
import static jasper.security.Auth.LOCAL_ORIGIN_HEADER;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

/**
 * Filters incoming requests and installs a Spring Security principal if a header corresponding to a valid user is
 * found.
 */
public class JWTFilter extends GenericFilterBean {
	private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

	public static final String AUTHORIZATION_HEADER = "Authorization";

	private final Props props;
	private final TokenProvider tokenProvider;
	private final TokenProviderImplDefault defaultTokenProvider;
	private final ConfigCache configs;

	public JWTFilter(Props props, TokenProvider tokenProvider, TokenProviderImplDefault defaultTokenProvider, ConfigCache configs) {
		this.props = props;
		this.tokenProvider = tokenProvider;
		this.defaultTokenProvider = defaultTokenProvider;
		this.configs = configs;
	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		var httpServletRequest = (HttpServletRequest) servletRequest;
		if (!"OPTIONS".equalsIgnoreCase(httpServletRequest.getMethod())) {
			var route = httpServletRequest.getRequestURI();
			if (route.startsWith("/api/") || route.startsWith("/pub/api/")) {
				var origin = resolveOrigin(httpServletRequest);
				var jwt = resolveToken(httpServletRequest);
				if (configs.root().web(origin)) {
					if (tokenProvider.validateToken(jwt, origin)) {
						SecurityContextHolder.getContext().setAuthentication(tokenProvider.getAuthentication(jwt, origin));
					} else {
						SecurityContextHolder.getContext().setAuthentication(defaultTokenProvider.getAuthentication(null, origin));
					}
				} else {
					logger.error("{} No web access for origin ({}): {} ", props.getOrigin(), origin, route);
				}
			}
		}
		filterChain.doFilter(servletRequest, servletResponse);
	}

	private String resolveToken(HttpServletRequest request) {
		var bearerToken = request.getHeader(AUTHORIZATION_HEADER);
		if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
			return bearerToken.substring(7);
		}
		return null;
	}

	private String resolveOrigin(HttpServletRequest request) {
		var origin = props.getOrigin();
		var originHeader = request.getHeader(LOCAL_ORIGIN_HEADER);
		if (isNotBlank(originHeader)) {
			originHeader = originHeader.toLowerCase();
			logger.trace("Origin set by header ({})", originHeader);
			if ("default".equals(originHeader)) return origin;
			if (originHeader.matches(HasOrigin.REGEX_NOT_BLANK) && isSubOrigin(props.getLocalOrigin(), originHeader)) return originHeader;
		} else {
			logger.trace("Origin header not allowed to be blank");
		}
		logger.trace("Default origin used ({})", origin);
		return origin;
	}
}
