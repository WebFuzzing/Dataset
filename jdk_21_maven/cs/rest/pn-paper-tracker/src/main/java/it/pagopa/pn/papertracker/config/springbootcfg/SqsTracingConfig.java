package it.pagopa.pn.papertracker.config.springbootcfg;

import io.awspring.cloud.sqs.config.SqsListenerConfigurer;
import io.awspring.cloud.sqs.config.SqsMessageListenerContainerFactory;
import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class SqsTracingConfig {
    private static final String DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME = "tracedMessagesListenerContainerFactory";

    @Bean
    SqsListenerConfigurer configurer() {
        return registrar -> registrar.setDefaultListenerContainerFactoryBeanName(DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME);
    }

    @Bean
    SqsTemplate sqsTemplate(SqsAsyncClient sqsAsyncClient, ObservationRegistry observationRegistry) {
        return SqsTemplate.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configure(options -> options.observationRegistry(observationRegistry))
                .build();
    }

    @Bean(name = DEFAULT_LISTENER_CONTAINER_FACTORY_BEAN_NAME)
    SqsMessageListenerContainerFactory<Object> factory(SqsAsyncClient sqsAsyncClient, ObservationRegistry observationRegistry) {
        return SqsMessageListenerContainerFactory.builder()
                .sqsAsyncClient(sqsAsyncClient)
                .configure(options -> options.observationRegistry(observationRegistry))
                .build();
    }
}
