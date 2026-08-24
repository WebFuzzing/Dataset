package jasper.config;

import jasper.component.HttpClientFactory;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.cloud.openfeign.FeignClientsConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableFeignClients(basePackages = "jasper")
@Import(FeignClientsConfiguration.class)
public class FeignConfiguration {

    /**
     * Set the Feign specific log level to log client REST requests.
     */
    @Bean
    feign.Logger.Level feignLoggerLevel() {
        return feign.Logger.Level.BASIC;
    }

	@Bean
	public feign.Contract feignContract() {
		return new feign.Contract.Default();
	}

	@Bean
	public feign.httpclient.ApacheHttpClient feignHttpClient(HttpClientFactory factory) {
		return new feign.httpclient.ApacheHttpClient(factory.getSerialClient());
	}
}
