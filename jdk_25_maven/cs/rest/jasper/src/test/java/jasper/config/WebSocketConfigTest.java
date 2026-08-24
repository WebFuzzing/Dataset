package jasper.config;

import jasper.component.ConfigCache;
import jasper.repository.RefRepository;
import jasper.security.Auth;
import jasper.security.AuthFactory;
import jasper.security.jwt.TokenProvider;
import jasper.security.jwt.TokenProviderImplDefault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class WebSocketConfigTest {
	private WebSocketConfig config;
	private MessageChannel channel;

	@BeforeEach
	void setUp() {
		config = new WebSocketConfig();
		config.props = new Props();
		config.configs = mock(ConfigCache.class);
		config.tokenProvider = mock(TokenProvider.class);
		config.defaultTokenProvider = mock(TokenProviderImplDefault.class);
		config.authFactory = mock(AuthFactory.class);
		channel = mock(MessageChannel.class);
		when(config.configs.root()).thenReturn(new Config.ServerConfig());
	}

	@Test
	void authFactoryCreatesFreshAuthInstances() {
		var factory = new AuthFactory(new Props(), mock(RoleHierarchy.class), mock(ConfigCache.class), mock(RefRepository.class));

		assertThat(factory.create()).isNotSameAs(factory.create());
	}

	@Test
	void concurrentSessionsUseTheirOwnAuthentication() throws Exception {
		var barrier = new CyclicBarrier(2);
		var auths = new CopyOnWriteArrayList<TrackingAuth>();
		when(config.authFactory.create()).thenAnswer(invocation -> {
			var auth = new TrackingAuth(barrier);
			auths.add(auth);
			return auth;
		});
		var firstAuthentication = authentication("first");
		var secondAuthentication = authentication("second");
		var first = subscribe("/topic/first", firstAuthentication, null);
		var second = subscribe("/topic/second", secondAuthentication, null);

		assertThat(authorizeConcurrently(first, second)).containsExactly(first, second);
		assertThat(auths).hasSize(2).allSatisfy(auth -> assertThat(auth.authentications).hasSize(1));
		assertThat(auths.stream()
			.flatMap(auth -> auth.authentications.stream())
			.map(Authentication::getPrincipal))
			.containsExactlyInAnyOrder("first", "second");
	}

	@Test
	void concurrentSubscriptionsInOneSessionUseTheirOwnJwt() throws Exception {
		var barrier = new CyclicBarrier(2);
		var auths = new CopyOnWriteArrayList<TrackingAuth>();
		when(config.authFactory.create()).thenAnswer(invocation -> {
			var auth = new TrackingAuth(barrier);
			auths.add(auth);
			return auth;
		});
		var firstJwtAuthentication = authentication("first-jwt");
		var secondJwtAuthentication = authentication("second-jwt");
		when(config.tokenProvider.validateToken(anyString(), anyString())).thenReturn(true);
		when(config.tokenProvider.getAuthentication("first-token", "")).thenReturn(firstJwtAuthentication);
		when(config.tokenProvider.getAuthentication("second-token", "")).thenReturn(secondJwtAuthentication);
		var sessionAttributes = sessionAttributes();
		var first = subscribe("/topic/first-jwt", authentication("session"), "first-token", sessionAttributes);
		var second = subscribe("/topic/second-jwt", authentication("session"), "second-token", sessionAttributes);

		assertThat(authorizeConcurrently(first, second)).containsExactly(first, second);
		assertThat(auths).hasSize(2).allSatisfy(auth -> assertThat(auth.authentications).hasSize(2));
		assertThat(auths.stream().map(TrackingAuth::principals))
			.containsExactlyInAnyOrder(
				List.of("session", "first-jwt"),
				List.of("session", "second-jwt")
			);
	}

	@Test
	void subscriptionJwtIsIgnoredWithoutSessionAuthentication() {
		var auth = new TrackingAuth(new CyclicBarrier(1));
		when(config.authFactory.create()).thenReturn(auth);
		when(config.defaultTokenProvider.getAuthentication(null, "")).thenReturn(authentication("default"));
		var message = subscribe("/topic/default", null, "token");

		assertThat(config.new JwtChannelInterceptor().preSend(message, channel)).isSameAs(message);
		assertThat(auth.principals()).containsExactly("default");
		verifyNoInteractions(config.tokenProvider);
		verify(config.defaultTokenProvider).getAuthentication(null, "");
	}

	private List<Message<?>> authorizeConcurrently(Message<?> first, Message<?> second) throws Exception {
		var interceptor = config.new JwtChannelInterceptor();
		try (var executor = Executors.newFixedThreadPool(2)) {
			var firstResult = executor.submit(() -> interceptor.preSend(first, channel));
			var secondResult = executor.submit(() -> interceptor.preSend(second, channel));
			return List.of(firstResult.get(5, TimeUnit.SECONDS), secondResult.get(5, TimeUnit.SECONDS));
		}
	}

	private Message<byte[]> subscribe(String destination, Authentication authentication, String jwt) {
		return subscribe(destination, authentication, jwt, sessionAttributes());
	}

	private Message<byte[]> subscribe(String destination, Authentication authentication, String jwt, HashMap<String, Object> sessionAttributes) {
		var accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
		accessor.setDestination(destination);
		accessor.setUser(authentication);
		accessor.setSessionAttributes(sessionAttributes);
		if (jwt != null) accessor.setNativeHeader("jwt", jwt);
		return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
	}

	private HashMap<String, Object> sessionAttributes() {
		var sessionAttributes = new HashMap<String, Object>();
		sessionAttributes.put("wsAttributes", new WebSocketConfig.WebSocketRequestAttributes());
		return sessionAttributes;
	}

	private Authentication authentication(String principal) {
		return new UsernamePasswordAuthenticationToken(principal, null, List.of());
	}

	private static class TrackingAuth extends Auth {
		private final CyclicBarrier barrier;
		private final List<Authentication> authentications = new ArrayList<>();

		TrackingAuth(CyclicBarrier barrier) {
			super(null, null, null, null);
			this.barrier = barrier;
		}

		@Override
		public void clear(Authentication authentication) {
			authentications.add(authentication);
		}

		@Override
		public String getOrigin() {
			return "";
		}

		@Override
		public boolean canSubscribeTo(String destination) {
			try {
				barrier.await(5, TimeUnit.SECONDS);
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
			return destination.equals("/topic/" + authentications.getLast().getPrincipal());
		}

		List<Object> principals() {
			return authentications.stream().map(Authentication::getPrincipal).toList();
		}
	}
}
