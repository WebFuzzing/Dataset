package it.pagopa.pn.papertracker.middleware.queue.consumer.internal;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.papertracker.service.SourceQueueProxyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExternalChannelSourceEventsHandlerTest {

    @Mock
    private SourceQueueProxyService sourceQueueProxyService;

    private ExternalChannelSourceEventsHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ExternalChannelSourceEventsHandler(sourceQueueProxyService);
    }

    @Test
    void handleExternalChannelMessage_ok() {
        // Arrange
        SingleStatusUpdate message = getSingleStatusUpdate("REQ-TEST");
        Map<String, MessageAttributeValue> attributes = new HashMap<>();

        when(sourceQueueProxyService.handleExternalChannelMessage(message, attributes))
                .thenReturn(Mono.empty());

        // Act
        assertDoesNotThrow(() -> handler.handleExternalChannelMessage(message, attributes));

        // Assert
        verify(sourceQueueProxyService, times(1))
                .handleExternalChannelMessage(message, attributes);
    }

    @Test
    void handleExternalChannelMessage_nullMessage_throwsException() {
        // Arrange
        Map<String, MessageAttributeValue> attributes = new HashMap<>();

        // Act Assert
        assertThrows(
                IllegalArgumentException.class,
                () -> handler.handleExternalChannelMessage(null, attributes)
        );

        verifyNoInteractions(sourceQueueProxyService);
    }

    @Test
    void handleExternalChannelMessage_nullAnalogMail_throwsException() {
        // Arrange
        SingleStatusUpdate message = new SingleStatusUpdate();
        message.setAnalogMail(null);
        Map<String, MessageAttributeValue> attributes = new HashMap<>();

        // Act Assert
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> handler.handleExternalChannelMessage(message, attributes)
        );

        assertEquals("Payload or analogMail cannot be null", ex.getMessage());
        verifyNoInteractions(sourceQueueProxyService);
    }

    @Test
    void handleExternalChannelMessage_serviceReturnsError_propagatesException() {
        // Arrange
        SingleStatusUpdate message = getSingleStatusUpdate("REQ-ERR");
        Map<String, MessageAttributeValue> attributes = new HashMap<>();

        RuntimeException serviceException = new RuntimeException("Boom");

        when(sourceQueueProxyService.handleExternalChannelMessage(message, attributes))
                .thenReturn(Mono.error(serviceException));

        // Act / Assert
        RuntimeException ex = assertThrows(
                RuntimeException.class,
                () -> handler.handleExternalChannelMessage(message, attributes)
        );

        assertEquals("Boom", ex.getMessage());
        verify(sourceQueueProxyService).handleExternalChannelMessage(message, attributes);
    }

    private SingleStatusUpdate getSingleStatusUpdate(String requestId) {
        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        PaperProgressStatusEvent analogMail = new PaperProgressStatusEvent();
        analogMail.setRequestId(requestId);
        singleStatusUpdate.setAnalogMail(analogMail);
        return singleStatusUpdate;
    }
}
