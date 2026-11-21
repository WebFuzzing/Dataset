package it.pagopa.pn.mandate.springbootcfg;

import it.pagopa.pn.commons.abstractions.ParameterConsumer;
import it.pagopa.pn.commons.configs.ValidationCFConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ValidateUtilsActivation extends ValidationCFConfiguration {
    public ValidateUtilsActivation(ParameterConsumer parameterConsumer) {
        super(parameterConsumer);
    }
}
