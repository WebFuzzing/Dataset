package it.pagopa.pn.papertracker.middleware.queue.consumer;

import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataResultPayload;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.ExternalChannelHandler;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.ExternalChannelSourceEventsHandler;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.OcrEventHandler;
import it.pagopa.pn.papertracker.utils.LogUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PnEventInboundServiceTest {

    @Mock
    private ExternalChannelSourceEventsHandler externalChannelSourceEventsHandler;

    @Mock
    private ExternalChannelHandler externalChannelHandler;

    @Mock
    private OcrEventHandler ocrEventHandler;

    @Mock
    private LogUtility logUtility;

    private PnEventInboundService service;

    @BeforeEach
    void setUp() {
        service = new PnEventInboundService(
                externalChannelSourceEventsHandler,
                externalChannelHandler,
                ocrEventHandler,
                logUtility
        );
    }

    @Test
    void externalChannelSourceConsumerProcessesMessage() {
        var payload = new SingleStatusUpdate();
        Message<SingleStatusUpdate> message = MessageBuilder.withPayload(payload).build();
        Map<String, Object> headers = new HashMap<>();
        Map<String, MessageAttributeValue> attributes = new HashMap<>();
        software.amazon.awssdk.services.sqs.model.Message sqsMessage = software.amazon.awssdk.services.sqs.model.Message.builder()
                .messageId("msg-123")
                .body("payload-ignored-in-test")
                .messageAttributes(attributes)
                .build();

        service.externalChannelSourceConsumer(message, headers, sqsMessage);

        verify(externalChannelSourceEventsHandler).handleExternalChannelMessage(payload, attributes);
    }

    @Test
    void externalChannelConsumerProcessesMessageWithAllHeaders() {
        Message<SingleStatusUpdate> message = MessageBuilder.withPayload(new SingleStatusUpdate()).build();
        Map<String, Object> headers = Map.of(
                "dryRun", true,
                "reworkId", "rework123",
                "id", "message123",
                "SenderId", "sender123"
        );

        service.externalChannelConsumer(message, true, "rework123", "message123", "sender123", headers);

        verify(externalChannelHandler).handleExternalChannelMessage(
                message.getPayload(), true, "rework123", "message123", "sender123"
        );
    }

    @Test
    void ocrOutputsConsumerProcessesMessage() {
        Message<OcrDataResultPayload> message = MessageBuilder.withPayload(new OcrDataResultPayload.OcrDataResultPayloadBuilder().build()).build();
        Map<String, Object> headers = new HashMap<>();

        service.ocrOutputsConsumer(message, headers);

        verify(ocrEventHandler).handleOcrMessage(message.getPayload());
    }
}
