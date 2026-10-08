package it.pagopa.pn.papertracker.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.papertracker.middleware.queue.model.CustomEventHeader;
import it.pagopa.pn.papertracker.middleware.queue.model.ExternalChannelEvent;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;

@Slf4j
public class ExternalChannelToPaperChannelDryRunMomProducer extends AbstractSqsMomProducer<ExternalChannelEvent> {

    public ExternalChannelToPaperChannelDryRunMomProducer(SqsClient sqsClient, String topic, ObjectMapper objectMapper, Class<ExternalChannelEvent> msgClass) {
        super(sqsClient, topic, objectMapper, msgClass);
    }

    @Override
    protected Map<String, MessageAttributeValue> getSqSHeader(GenericEventHeader header) {
        Map<String, MessageAttributeValue> map = super.getSqSHeader(header);
        if (!(header instanceof CustomEventHeader headerCustom)) {
            return map;
        }
        map.putAll(headerCustom.getMessageAttributes());
        return map;
    }
}