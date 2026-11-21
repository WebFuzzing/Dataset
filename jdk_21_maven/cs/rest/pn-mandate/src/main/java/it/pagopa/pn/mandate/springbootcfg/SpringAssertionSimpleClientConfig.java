package it.pagopa.pn.mandate.springbootcfg;

import it.pagopa.tech.lollipop.consumer.assertion.client.simple.AssertionSimpleClientConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(
        prefix = "lollipop.assertion.rest.config"
)
@ConfigurationPropertiesScan
@Configuration
public class SpringAssertionSimpleClientConfig extends AssertionSimpleClientConfig {}
