package it.pagopa.pn.mandate.springbootcfg;

import it.pagopa.pn.lollipop.client.config.PNHttpVerifierConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix="lollipop", name="active", havingValue = "true")
public class LollipopActivation extends PNHttpVerifierConfiguration {
}
