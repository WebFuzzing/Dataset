package it.pagopa.pn.papertracker.middleware.queue.consumer;

import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataResultPayload;
import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.ExternalChannelHandler;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.ExternalChannelSourceEventsHandler;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.OcrEventHandler;
import it.pagopa.pn.papertracker.utils.LogUtility;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Headers;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@CustomLog
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "pn.paper-tracker.",
        name = "disable-all-consumers",
        havingValue = "false"
)
public class PnEventInboundService {

    private final ExternalChannelSourceEventsHandler externalChannelSourceEventsHandler;
    private final ExternalChannelHandler externalChannelHandler;
    private final OcrEventHandler ocrEventHandler;
    private final LogUtility logUtility;

    @SqsListener("${pn.paper-tracker.topics.external-channel-to-paper-channel-queue}")
    public void externalChannelSourceConsumer(
            @Payload Message<SingleStatusUpdate> message,
            @Headers Map<String, Object> headers,
            @Header(name = SqsHeaders.SQS_SOURCE_DATA_HEADER) software.amazon.awssdk.services.sqs.model.Message sourceMessage
    ) {
        processMessage(() -> {
                var messageAttributes = sourceMessage.messageAttributes();
                externalChannelSourceEventsHandler.handleExternalChannelMessage(message.getPayload(), messageAttributes);
            }, "pn-external_channel_to_paper_channel", message, headers);
    }

    @SqsListener("${pn.paper-tracker.topics.external-channel-to-paper-tracker-queue}")
    public void externalChannelConsumer(
            @Payload Message<SingleStatusUpdate> message,
            @Header(name = "dryRun", required = false) Boolean dryRun,
            @Header(name = "reworkId", required = false) String reworkId,
            @Header(name = "id") String messageId,
            @Header(name = SqsHeaders.MessageSystemAttributes.SQS_SENDER_ID, required = false) String senderId,
            @Headers Map<String, Object> headers
    ) {
        processMessage(() -> externalChannelHandler.handleExternalChannelMessage(
                        message.getPayload(), Boolean.TRUE.equals(dryRun), reworkId, messageId, senderId),
                "pn-external_channel_to_paper_tracker", message, headers);
    }

    @SqsListener("${pn.paper-tracker.topics.pn-ocr-outputs-queue}")
    public void ocrOutputsConsumer(
            @Payload Message<OcrDataResultPayload> message,
            @Headers Map<String, Object> headers
    ) {
        processMessage(() -> ocrEventHandler.handleOcrMessage(message.getPayload()),
                "pn-ocr_outputs", message, headers);
    }

    private void processMessage(Runnable handler, String source, Message<?> message, Map<String, Object> headers) {
        String anonymizedMessage = logUtility.messageToString(message, Set.of("discoveredAddress"));
        try {
            log.info("Handle message from {} with content {}", source, anonymizedMessage);
            setMDCContext(headers);
            handler.run();
        } catch (Exception ex) {
            log.error("Error processing message from {}: {}", source, ex.getMessage(), ex);
            throw ex;
        }
    }

    private void setMDCContext(Map<String, Object> headers) {
        MDCUtils.clearMDCKeys();
        MDC.put(MDCUtils.MDC_PN_CTX_MESSAGE_ID, headers.getOrDefault("id", "").toString());
        MDC.put(MDCUtils.MDC_TRACE_ID_KEY, headers.getOrDefault("AWSTraceHeader", UUID.randomUUID().toString()).toString());
    }
}
