package jasper.config;

import jakarta.servlet.http.HttpServletRequest;
import jasper.component.ConfigCache;
import jasper.domain.proj.HasOrigin;
import jasper.security.Auth;
import jasper.security.jwt.TokenProvider;
import jasper.security.jwt.TokenProviderImplDefault;
import jasper.service.dto.UserDto;
import org.apache.tomcat.websocket.server.WsSci;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.tomcat.TomcatContextCustomizer;
import org.springframework.boot.tomcat.servlet.TomcatServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.handler.WebSocketHandlerDecorator;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.io.IOException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static jasper.domain.proj.HasOrigin.isSubOrigin;
import static jasper.security.Auth.LOCAL_ORIGIN_HEADER;
import static jasper.security.Auth.READ_ACCESS_HEADER;
import static jasper.security.Auth.TAG_READ_ACCESS_HEADER;
import static jasper.security.Auth.TAG_WRITE_ACCESS_HEADER;
import static jasper.security.Auth.USER_ROLE_HEADER;
import static jasper.security.Auth.USER_TAG_HEADER;
import static jasper.security.Auth.WRITE_ACCESS_HEADER;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

@Profile("!no-websocket")
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
	private static final Logger logger = LoggerFactory.getLogger(WebSocketConfig.class);

	public static final String AUTHORIZATION_HEADER = "Authorization";

	@Autowired
	Props props;

	@Autowired
	ConfigCache configs;

	@Autowired
	TokenProvider tokenProvider;

	@Autowired
	TokenProviderImplDefault defaultTokenProvider;

	@Autowired
	@Qualifier("authSingleton")
	Auth auth;

	private Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();

	@Bean
	public TomcatServletWebServerFactory tomcatContainerFactory() {
		var factory = new TomcatServletWebServerFactory();
		factory.addContextCustomizers(tomcatContextCustomizer());
		return factory;
	}

	@Bean
	public TomcatContextCustomizer tomcatContextCustomizer() {
		return context -> context.addServletContainerInitializer(new WsSci(), null);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry config) {
		config.enableSimpleBroker("/topic");
		config.setApplicationDestinationPrefixes("/app");
	}

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry
			.addEndpoint("/api/stomp/")
				.setHandshakeHandler(new StompDefaultHandshakeHandler())
				.addInterceptors(new StompHandshakeInterceptor())
				.setAllowedOriginPatterns("*")
			.withSockJS()
				.setSuppressCors(true);
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
		registration.interceptors(new JwtChannelInterceptor());
	}

	@Override
	public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
		registration.addDecoratorFactory(handler -> new WebSocketHandlerDecorator(handler) {
			@Override
			public void afterConnectionEstablished(WebSocketSession session) throws Exception {
				sessions.add(session);
				super.afterConnectionEstablished(session);
			}
			@Override
			public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
				sessions.remove(session);
				super.afterConnectionClosed(session, closeStatus);
			}

		});
	}

	@ServiceActivator(inputChannel = "userRxChannel")
	public void handleUserUpdate(Message<UserDto> message) {
		// Just drop all sessions for now
		sessions.forEach(s -> {
            try {
                s.close(CloseStatus.SERVICE_RESTARTED);
            } catch (IOException e) {
                logger.warn("Could not close websocket session.", e);
            }
        });
		sessions.clear();
	}

	class StompHandshakeInterceptor implements HandshakeInterceptor {

		private String resolveOrigin(HttpServletRequest request) {
			var originHeader = request.getHeader(LOCAL_ORIGIN_HEADER);
			logger.debug("STOMP Local Origin Header: {}", originHeader);
			if (isNotBlank(originHeader)) {
				originHeader = originHeader.toLowerCase();
				if ("default".equals(originHeader)) return props.getLocalOrigin();
				if (originHeader.matches(HasOrigin.REGEX) && isSubOrigin(props.getLocalOrigin(), originHeader)) {
					return originHeader;
				}
			}
			return props.getOrigin();
		}

		@Override
		public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Map<String, Object> attributes) {
			logger.debug("STOMP Handshake");
			if (request instanceof ServletServerHttpRequest servletRequest) {
				var httpServletRequest = servletRequest.getServletRequest();
				var token = httpServletRequest.getHeader(AUTHORIZATION_HEADER);
				if (isNotBlank(token) && token.startsWith("Bearer ")) {
					attributes.put("jwt", token.substring("Bearer ".length()));
				}
				attributes.put("origin", resolveOrigin(httpServletRequest));
				// Create WebSocket request attributes for proxy-controlled headers
				var wsAttributes = new WebSocketRequestAttributes();
				wsAttributes.setHeader(LOCAL_ORIGIN_HEADER, httpServletRequest.getHeader(LOCAL_ORIGIN_HEADER));
				wsAttributes.setHeader(USER_ROLE_HEADER, httpServletRequest.getHeader(USER_ROLE_HEADER));
				wsAttributes.setHeader(USER_TAG_HEADER, httpServletRequest.getHeader(USER_TAG_HEADER));
				wsAttributes.setHeader(READ_ACCESS_HEADER, httpServletRequest.getHeader(READ_ACCESS_HEADER));
				wsAttributes.setHeader(WRITE_ACCESS_HEADER, httpServletRequest.getHeader(WRITE_ACCESS_HEADER));
				wsAttributes.setHeader(TAG_READ_ACCESS_HEADER, httpServletRequest.getHeader(TAG_READ_ACCESS_HEADER));
				wsAttributes.setHeader(TAG_WRITE_ACCESS_HEADER, httpServletRequest.getHeader(TAG_WRITE_ACCESS_HEADER));
				attributes.put("wsAttributes", wsAttributes);
			}
			return true;
		}

		@Override
		public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) { }
	}

	class StompDefaultHandshakeHandler extends DefaultHandshakeHandler {
		@Override
		public Principal determineUser(ServerHttpRequest request, WebSocketHandler handler, Map<String, Object> attributes) {
			var origin = (String) attributes.get("origin");
			WebSocketConfig.logger.debug("{} STOMP Determine User", origin);
			if (!configs.root().web(origin)) {
				WebSocketConfig.logger.error("{} No web access for origin", origin);
				return null;
			}
			try {
				var wsAttributes = (WebSocketRequestAttributes) attributes.get("wsAttributes");
				RequestContextHolder.setRequestAttributes(wsAttributes);
				if (!attributes.containsKey("jwt")) return defaultTokenProvider.getAuthentication(null, origin);
				var token = (String) attributes.get("jwt");
				return tokenProvider.validateToken(token, origin) ? tokenProvider.getAuthentication(token, origin) : defaultTokenProvider.getAuthentication(null, origin);
			} finally {
				RequestContextHolder.resetRequestAttributes();
			}
		}
	}

	class JwtChannelInterceptor implements ChannelInterceptor {
		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			try {
				var accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
				if (accessor.getCommand() == StompCommand.BEGIN) return null; // No Transactions
				if (accessor.getCommand() == StompCommand.SEND) return null; // No Client Messages
				if (accessor.getCommand() != StompCommand.SUBSCRIBE) return message;
				var wsAttributes = (WebSocketRequestAttributes) accessor.getSessionAttributes().get("wsAttributes");
				RequestContextHolder.setRequestAttributes(wsAttributes);
				if (accessor.getUser() instanceof Authentication authentication) {
					auth.clear(authentication);
					logger.debug("{} STOMP User Set {}", auth.getOrigin(), auth.getUserTag());
					@SuppressWarnings("unchecked")
					var headers = (Map<String, ArrayList<String>>) message.getHeaders().get("nativeHeaders", Map.class);
					if (headers != null && headers.get("jwt") != null && headers.get("jwt").size() > 0) {
						var token = headers.get("jwt").get(0);
						var origin = auth.getOrigin();
						if  (tokenProvider.validateToken(token, origin)) {
							auth.clear(tokenProvider.getAuthentication(token, origin));
							logger.debug("{} STOMP SUBSCRIBE Credentials Header {}", auth.getOrigin(), auth.getUserTag());
						}
					}
				} else {
					auth.clear(defaultTokenProvider.getAuthentication(null, props.getOrigin()));
					logger.debug("{} STOMP Default auth {}", auth.getOrigin(), auth.getUserTag());
				}
				if (!configs.root().web(auth.getOrigin())) {
					logger.error("{} No web access for origin", auth.getOrigin());
					return null;
				}
				if (auth.canSubscribeTo(accessor.getDestination())) return message;
				logger.warn("{} {} can't subscribe to {}", auth.getOrigin(), auth.getUserTag(), accessor.getDestination());
			} catch (Exception e) {
				logger.error("{} Cannot authorize websocket subscription.", auth.getOrigin(), e);
			} finally {
				RequestContextHolder.resetRequestAttributes();
			}
			return null;
		}
	}

	public static class WebSocketRequestAttributes implements RequestAttributes {
		private final Map<String, String> headers = new HashMap<>();

		public void setHeader(String name, String value) {
			headers.put(name, value);
		}

		public String getHeader(String name) {
			return headers.get(name);
		}

		@Override
		public Object getAttribute(String name, int scope) {
			return headers.get(name);
		}

		@Override
		public void setAttribute(String name, Object value, int scope) {
			headers.put(name, (String)value);
		}

		@Override
		public void removeAttribute(String name, int scope) {

		}

		@Override
		public String[] getAttributeNames(int scope) {
			return new String[0];
		}

		@Override
		public void registerDestructionCallback(String name, Runnable callback, int scope) {}

		@Override
		public Object resolveReference(String key) { return null; }

		@Override
		public String getSessionId() { return null; }

		@Override
		public Object getSessionMutex() { return null; }
	}
}
