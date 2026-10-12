package it.pagopa.pn.papertracker.middleware.queue.producer;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.api.dto.events.AbstractSqsMomProducer;
import it.pagopa.pn.papertracker.middleware.queue.model.OcrEvent;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;

@Slf4j
public class OcrMomProducer extends AbstractSqsMomProducer<OcrEvent>  {

    public OcrMomProducer(SqsClient sqsClient, String topic, String url, ObjectMapper objectMapper, Class<OcrEvent> msgClass) {
        super(sqsClient, topic, url, objectMapper, msgClass);
    }

}