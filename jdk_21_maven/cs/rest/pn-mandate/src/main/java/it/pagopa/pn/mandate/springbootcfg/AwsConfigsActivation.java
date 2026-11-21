package it.pagopa.pn.mandate.springbootcfg;

import it.pagopa.pn.commons.configs.aws.AwsConfigs;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties("aws")
public class AwsConfigsActivation extends AwsConfigs {
}
