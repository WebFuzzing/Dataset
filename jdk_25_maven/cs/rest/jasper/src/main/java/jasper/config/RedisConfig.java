package jasper.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jasper.service.dto.ExtDto;
import jasper.service.dto.PluginDto;
import jasper.service.dto.RefDto;
import jasper.service.dto.TemplateDto;
import jasper.service.dto.UserDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static jasper.component.Messages.originHeaders;
import static jasper.component.Messages.refHeaders;
import static jasper.component.Messages.responseHeaders;
import static jasper.component.Messages.tagHeaders;
import static jasper.domain.proj.HasOrigin.formatOrigin;
import static jasper.domain.proj.HasTags.formatTag;
import static java.util.Arrays.copyOfRange;
import static org.springframework.data.redis.listener.PatternTopic.of;

@Profile("redis")
@Import(DataRedisAutoConfiguration.class)
@Configuration
public class RedisConfig {
	private static final Logger logger = LoggerFactory.getLogger(RedisConfig.class);

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	RedisConnectionFactory redisConnectionFactory;

	@Autowired
	MessageChannel cursorTxChannel;

	@Autowired
	MessageChannel cursorRxChannel;

	@Autowired
	MessageChannel refTxChannel;

	@Autowired
	MessageChannel refRxChannel;

	@Autowired
	MessageChannel tagTxChannel;

	@Autowired
	MessageChannel tagRxChannel;

	@Autowired
	MessageChannel responseTxChannel;

	@Autowired
	MessageChannel responseRxChannel;

	@Autowired
	MessageChannel userTxChannel;

	@Autowired
	MessageChannel userRxChannel;

	@Autowired
	MessageChannel extTxChannel;

	@Autowired
	MessageChannel extRxChannel;

	@Autowired
	MessageChannel pluginTxChannel;

	@Autowired
	MessageChannel pluginRxChannel;

	@Autowired
	MessageChannel templateTxChannel;

	@Autowired
	MessageChannel templateRxChannel;

	@Bean
	public MessageChannel cursorRedisChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel refRedisChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel tagRedisChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel responseRedisChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel userRedisChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel extRedisChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel pluginRedisChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel templateRedisChannel() {
		return new DirectChannel();
	}

	@Bean
	public IntegrationFlow redisPublishCursorFlow() {
		return IntegrationFlow
			.from(cursorTxChannel)
			.handle(new CustomPublishingMessageHandler<Instant>() {
				@Override
				protected String getTopic(Message<Instant> message) {
					return "cursor/" + formatOrigin(message.getHeaders().get("origin"));
				}

				@Override
				protected byte[] getMessage(Message<Instant> message) {
					return message.getPayload().toString().getBytes();
				}
			})
			.get();
	}

	@Bean
	public IntegrationFlow redisSubscribeCursorFlow() {
		return IntegrationFlow
			.from(cursorRedisChannel())
			.channel(cursorRxChannel)
			.get();
	}

	@Bean
	public RedisMessageListenerContainer redisCursorRxAdapter(RedisConnectionFactory redisConnectionFactory) {
		var container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.addMessageListener((message, pattern) -> {
			var cursor = Instant.parse(new String(message.getBody()));
			var parts = new String(message.getChannel(), StandardCharsets.UTF_8).split("/");
			var origin = parts[1];
			cursorRedisChannel().send(MessageBuilder.createMessage(cursor, originHeaders(origin)));
		}, of("cursor/*"));
		return container;
	}

	@Bean
	public IntegrationFlow redisPublishRefFlow() {
		return IntegrationFlow
			.from(refTxChannel)
			.handle(new CustomPublishingMessageHandler<RefDto>() {
				@Override
				protected String getTopic(Message<RefDto> message) {
					return "ref/" + formatOrigin(message.getHeaders().get("origin")) + "/" + message.getHeaders().get("url");
				}

				@Override
				protected byte[] getMessage(Message<RefDto> message) {
					try {
						return objectMapper.writeValueAsBytes(message.getPayload());
					} catch (JsonProcessingException e) {
						logger.error("Cannot serialize RefDto.");
						throw new RuntimeException(e);
					}
				}
			})
			.get();
	}

	@Bean
	public IntegrationFlow redisSubscribeRefFlow() {
		return IntegrationFlow
			.from(refRedisChannel())
			.channel(refRxChannel)
			.get();
	}

	@Bean
	public RedisMessageListenerContainer redisRefRxAdapter(RedisConnectionFactory redisConnectionFactory) {
		var container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.addMessageListener((message, pattern) -> {
			try {
				var ref = objectMapper.readValue(message.getBody(), RefDto.class);
				var parts = new String(message.getChannel(), StandardCharsets.UTF_8).split("/");
				var origin = parts[1];
				refRedisChannel().send(MessageBuilder.createMessage(ref, refHeaders(origin, ref)));
			} catch (IOException e) {
				logger.error("Error parsing RefDto from redis.");
			}
		}, of("ref/*"));
		return container;
	}

	@Bean
	public IntegrationFlow redisPublishTagFlow() {
		return IntegrationFlow
			.from(tagTxChannel)
			.handle(new CustomPublishingMessageHandler<String>() {
				@Override
				protected String getTopic(Message<String> message) {
					return "tag/" + formatOrigin(message.getHeaders().get("origin")) + "/" + formatTag(message.getHeaders().get("tag"));
				}

				@Override
				protected byte[] getMessage(Message<String> message) {
					return message.getPayload().getBytes();
				}
			})
			.get();
	}

	@Bean
	public IntegrationFlow redisSubscribeTagFlow() {
		return IntegrationFlow
			.from(tagRedisChannel())
			.channel(tagRxChannel)
			.get();
	}

	@Bean
	public RedisMessageListenerContainer redisTagRxAdapter(RedisConnectionFactory redisConnectionFactory) {
		var container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.addMessageListener((message, pattern) -> {
			var fullTag = new String(message.getBody(), StandardCharsets.UTF_8);
			var parts = new String(message.getChannel(), StandardCharsets.UTF_8).split("/");
			var origin = parts[1];
			var tag = String.join("/", copyOfRange(parts, 2, parts.length));
			tagRedisChannel().send(MessageBuilder.createMessage(fullTag, tagHeaders(origin, tag)));
		}, of("tag/*"));
		return container;
	}

	@Bean
	public IntegrationFlow redisPublishResponseFlow() {
		return IntegrationFlow
			.from(responseTxChannel)
			.handle(new CustomPublishingMessageHandler<String>() {
				@Override
				protected String getTopic(Message<String> message) {
					return "response/" + formatOrigin(message.getHeaders().get("origin")) + "/" + message.getHeaders().get("response");
				}

				@Override
				protected byte[] getMessage(Message<String> message) {
					return message.getPayload().getBytes();
				}
			})
			.get();
	}

	@Bean
	public IntegrationFlow redisSubscribeResponseFlow() {
		return IntegrationFlow
			.from(responseRedisChannel())
			.channel(responseRxChannel)
			.get();
	}

	@Bean
	public RedisMessageListenerContainer redisResponseRxAdapter(RedisConnectionFactory redisConnectionFactory) {
		var container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.addMessageListener((message, pattern) -> {
			var response = new String(message.getBody(), StandardCharsets.UTF_8);
			var parts = new String(message.getChannel(), StandardCharsets.UTF_8).split("/");
			var origin = parts[1];
			var source = String.join("/", copyOfRange(parts, 2, parts.length));
			responseRedisChannel().send(MessageBuilder.createMessage(response, responseHeaders(origin, source)));
		}, of("response/*"));
		return container;
	}

	@Bean
	public IntegrationFlow redisPublishUserFlow() {
		return IntegrationFlow
			.from(userTxChannel)
			.handle(new CustomPublishingMessageHandler<UserDto>() {
				@Override
				protected String getTopic(Message<UserDto> message) {
					return "user/" + formatOrigin(message.getHeaders().get("origin")) + "/" + message.getHeaders().get("tag");
				}

				@Override
				protected byte[] getMessage(Message<UserDto> message) {
					try {
						return objectMapper.writeValueAsBytes(message.getPayload());
					} catch (JsonProcessingException e) {
						logger.error("Cannot serialize UserDto.");
						throw new RuntimeException(e);
					}
				}
			})
			.get();
	}

	@Bean
	public IntegrationFlow redisSubscribeUserFlow() {
		return IntegrationFlow
			.from(userRedisChannel())
			.channel(userRxChannel)
			.get();
	}

	@Bean
	public RedisMessageListenerContainer redisUserRxAdapter(RedisConnectionFactory redisConnectionFactory) {
		var container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.addMessageListener((message, pattern) -> {
			try {
				var user = objectMapper.readValue(message.getBody(), UserDto.class);
				var parts = new String(message.getChannel(), StandardCharsets.UTF_8).split("/");
				var origin = parts[1];
				var tag = String.join("/", copyOfRange(parts, 2, parts.length));
				userRedisChannel().send(MessageBuilder.createMessage(user, tagHeaders(origin, tag)));
			} catch (IOException e) {
				logger.error("Error parsing UserDto from redis.");
			}
		}, of("user/*"));
		return container;
	}

	@Bean
	public IntegrationFlow redisPublishExtFlow() {
		return IntegrationFlow
			.from(extTxChannel)
			.handle(new CustomPublishingMessageHandler<ExtDto>() {
				@Override
				protected String getTopic(Message<ExtDto> message) {
					return "ext/" + formatOrigin(message.getHeaders().get("origin")) + "/" + formatTag(message.getHeaders().get("tag"));
				}

				@Override
				protected byte[] getMessage(Message<ExtDto> message) {
					try {
						return objectMapper.writeValueAsBytes(message.getPayload());
					} catch (JsonProcessingException e) {
						logger.error("Cannot serialize ExtDto.");
						throw new RuntimeException(e);
					}
				}
			})
			.get();
	}

	@Bean
	public IntegrationFlow redisSubscribeExtFlow() {
		return IntegrationFlow
			.from(extRedisChannel())
			.channel(extRxChannel)
			.get();
	}

	@Bean
	public RedisMessageListenerContainer redisExtRxAdapter(RedisConnectionFactory redisConnectionFactory) {
		var container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.addMessageListener((message, pattern) -> {
			try {
				var ext = objectMapper.readValue(message.getBody(), ExtDto.class);
				var parts = new String(message.getChannel(), StandardCharsets.UTF_8).split("/");
				var origin = parts[1];
				var tag = String.join("/", copyOfRange(parts, 2, parts.length));
				extRedisChannel().send(MessageBuilder.createMessage(ext, tagHeaders(origin, tag)));
			} catch (IOException e) {
				logger.error("Error parsing ExtDto from redis.");
			}
		}, of("ext/*"));
		return container;
	}

	@Bean
	public IntegrationFlow redisPublishPluginFlow() {
		return IntegrationFlow
			.from(pluginTxChannel)
			.handle(new CustomPublishingMessageHandler<PluginDto>() {
				@Override
				protected String getTopic(Message<PluginDto> message) {
					return "plugin/" + formatOrigin(message.getHeaders().get("origin")) + "/" + formatTag(message.getHeaders().get("tag"));
				}

				@Override
				protected byte[] getMessage(Message<PluginDto> message) {
					try {
						return objectMapper.writeValueAsBytes(message.getPayload());
					} catch (JsonProcessingException e) {
						logger.error("Cannot serialize PluginDto.");
						throw new RuntimeException(e);
					}
				}
			})
			.get();
	}

	@Bean
	public IntegrationFlow redisSubscribePluginFlow() {
		return IntegrationFlow
			.from(pluginRedisChannel())
			.channel(pluginRxChannel)
			.get();
	}

	@Bean
	public RedisMessageListenerContainer redisPluginRxAdapter(RedisConnectionFactory redisConnectionFactory) {
		var container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.addMessageListener((message, pattern) -> {
			try {
				var plugin = objectMapper.readValue(message.getBody(), PluginDto.class);
				var parts = new String(message.getChannel(), StandardCharsets.UTF_8).split("/");
				var origin = parts[1];
				var tag = String.join("/", copyOfRange(parts, 2, parts.length));
				pluginRedisChannel().send(MessageBuilder.createMessage(plugin, tagHeaders(origin, tag)));
			} catch (IOException e) {
				logger.error("Error parsing PluginDto from redis.");
			}
		}, of("plugin/*"));
		return container;
	}

	@Bean
	public IntegrationFlow redisPublishTemplateFlow() {
		return IntegrationFlow
			.from(templateTxChannel)
			.handle(new CustomPublishingMessageHandler<TemplateDto>() {
				@Override
				protected String getTopic(Message<TemplateDto> message) {
					return "template/" + formatOrigin(message.getHeaders().get("origin")) + "/" + formatTag(message.getHeaders().get("tag"));
				}

				@Override
				protected byte[] getMessage(Message<TemplateDto> message) {
					try {
						return objectMapper.writeValueAsBytes(message.getPayload());
					} catch (JsonProcessingException e) {
						logger.error("Cannot serialize TemplateDto.");
						throw new RuntimeException(e);
					}
				}
			})
			.get();
	}

	@Bean
	public IntegrationFlow redisSubscribeTemplateFlow() {
		return IntegrationFlow
			.from(templateRedisChannel())
			.channel(templateRxChannel)
			.get();
	}

	@Bean
	public RedisMessageListenerContainer redisTemplateRxAdapter(RedisConnectionFactory redisConnectionFactory) {
		var container = new RedisMessageListenerContainer();
		container.setConnectionFactory(redisConnectionFactory);
		container.addMessageListener((message, pattern) -> {
			try {
				var template = objectMapper.readValue(message.getBody(), TemplateDto.class);
				var parts = new String(message.getChannel(), StandardCharsets.UTF_8).split("/");
				var origin = parts[1];
				var tag = String.join("/", copyOfRange(parts, 2, parts.length));
				templateRedisChannel().send(MessageBuilder.createMessage(template, tagHeaders(origin, tag)));
			} catch (IOException e) {
				logger.error("Error parsing TemplateDto from redis.");
			}
		}, of("template/*"));
		return container;
	}

	private abstract class CustomPublishingMessageHandler<T> extends AbstractMessageHandler {

		private final RedisTemplate<?, ?> template;

		public CustomPublishingMessageHandler() {
			template = new RedisTemplate<>();
			template.setConnectionFactory(redisConnectionFactory);
			template.setEnableDefaultSerializer(false);
			template.afterPropertiesSet();
		}

		@Override
		public String getComponentType() {
			return "redis:outbound-channel-adapter";
		}

		@Override
		@SuppressWarnings("unchecked")
		protected void handleMessageInternal(Message<?> message) {
			template.convertAndSend(getTopic((Message<T>) message), getMessage((Message<T>) message));
		}

		protected abstract String getTopic(Message<T> message);
		protected abstract byte[] getMessage(Message<T> message);
	}
}
