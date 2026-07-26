package jasper.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.MessageChannel;

@Configuration
@EnableIntegration
public class IntegrationConfig {

	@Bean
	public MessageChannel cursorTxChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel cursorRxChannel() {
		return new PublishSubscribeChannel();
	}

	@Bean
	public MessageChannel refTxChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel refRxChannel() {
		return new PublishSubscribeChannel();
	}

	@Bean
	public MessageChannel tagTxChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel tagRxChannel() {
		return new PublishSubscribeChannel();
	}

	@Bean
	public MessageChannel responseTxChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel responseRxChannel() {
		return new PublishSubscribeChannel();
	}

	@Bean
	public MessageChannel userTxChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel userRxChannel() {
		return new PublishSubscribeChannel();
	}

	@Bean
	public MessageChannel extTxChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel extRxChannel() {
		return new PublishSubscribeChannel();
	}

	@Bean
	public MessageChannel pluginTxChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel pluginRxChannel() {
		return new PublishSubscribeChannel();
	}

	@Bean
	public MessageChannel templateTxChannel() {
		return new DirectChannel();
	}

	@Bean
	public MessageChannel templateRxChannel() {
		return new PublishSubscribeChannel();
	}
}
