package it.pagopa.pn.mandate.springbootcfg;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.commons.abstractions.impl.AbstractCachedSsmParameterConsumer;
import it.pagopa.pn.commons.utils.qr.QrUrlCodecService;
import org.springframework.context.annotation.Configuration;

@Configuration
public class QrUrlCodecConsumerActivation extends QrUrlCodecService {
    public QrUrlCodecConsumerActivation(AbstractCachedSsmParameterConsumer abstractCachedSsmParameterConsumer, ObjectMapper objectMapper) {
        super(abstractCachedSsmParameterConsumer,objectMapper);
    }
}
