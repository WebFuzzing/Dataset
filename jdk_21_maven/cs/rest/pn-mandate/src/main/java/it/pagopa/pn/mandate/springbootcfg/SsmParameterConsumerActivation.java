package it.pagopa.pn.mandate.springbootcfg;

import it.pagopa.pn.commons.abstractions.impl.AbstractCachedSsmParameterConsumer;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.ssm.SsmClient;

@Configuration
public class SsmParameterConsumerActivation extends AbstractCachedSsmParameterConsumer {
    public SsmParameterConsumerActivation(SsmClient ssmClient) {
        super(ssmClient);
    }
}
