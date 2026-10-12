package it.pagopa.pn.papertracker.service;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;

public interface SourceQueueProxyService {

    Mono<Void> handleExternalChannelMessage(SingleStatusUpdate message, Map<String, MessageAttributeValue> messageAttributes);

}
