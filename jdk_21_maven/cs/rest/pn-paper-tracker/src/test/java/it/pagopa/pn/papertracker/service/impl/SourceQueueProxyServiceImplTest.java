package it.pagopa.pn.papertracker.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.papertracker.exception.PaperTrackerException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProcessingMode;
import it.pagopa.pn.papertracker.middleware.queue.model.ExternalChannelEvent;
import it.pagopa.pn.papertracker.middleware.queue.producer.ExternalChannelToPaperChannelDryRunMomProducer;
import it.pagopa.pn.papertracker.middleware.queue.producer.ExternalChannelToPaperTrackerMomProducer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SourceQueueProxyServiceImplTest {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ExternalChannelToPaperTrackerMomProducer paperTrackerProducer;

    @Mock
    private ExternalChannelToPaperChannelDryRunMomProducer paperChannelDryRunProducer;

    @Mock
    private PaperTrackingsDAO paperTrackingsDAO;

    @InjectMocks
    private SourceQueueProxyServiceImpl service;

    @Captor
    private ArgumentCaptor<ExternalChannelEvent> eventCaptor;

    private SingleStatusUpdate message;
    private Map<String, MessageAttributeValue> attributes;
    private PaperProgressStatusEvent analogMail;
    private static final String REQUEST_ID = "test-request-id-123";

    @BeforeEach
    void setUp() {
        analogMail = new PaperProgressStatusEvent();
        analogMail.setRequestId(REQUEST_ID);

        message = new SingleStatusUpdate();
        message.setAnalogMail(analogMail);

        attributes = Map.of();
    }

    @Test
    void handleExternalChannelMessage_trackingNotFound_shouldSendToPaperChannelOnly() {
        // Arrange
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.empty());

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(paperChannelDryRunProducer, times(1)).push(any(ExternalChannelEvent.class));
        verify(paperTrackerProducer, never()).push(any(ExternalChannelEvent.class));
        verify(paperTrackingsDAO, times(1)).retrieveEntityByTrackingId(REQUEST_ID);
    }

    @Test
    void handleExternalChannelMessage_nullProcessingMode_shouldSendToBothQueues() {
        // Arrange
        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(null);
        tracking.setTrackingId(REQUEST_ID);

        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(paperChannelDryRunProducer, times(1)).push(eventCaptor.capture());
        verify(paperTrackerProducer, times(1)).push(eventCaptor.capture());
        verify(paperTrackingsDAO, times(1)).retrieveEntityByTrackingId(REQUEST_ID);

        // Verifica che l'evento abbia il flag dryRun impostato a true
        var capturedEvents = eventCaptor.getAllValues();
        assertEquals(2, capturedEvents.size());
        capturedEvents.forEach(event -> {
            assertNotNull(event.getHeader());
            assertNotNull(event.getPayload());
            assertEquals(message, event.getPayload());
        });
    }

    @Test
    void handleExternalChannelMessage_dryMode_shouldSendToBothQueues() {
        // Arrange
        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(ProcessingMode.DRY);
        tracking.setTrackingId(REQUEST_ID);

        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(paperChannelDryRunProducer, times(1)).push(eventCaptor.capture());
        verify(paperTrackerProducer, times(1)).push(eventCaptor.capture());
        verify(paperTrackingsDAO, times(1)).retrieveEntityByTrackingId(REQUEST_ID);

        // Verifica che l'evento abbia il flag dryRun impostato a true
        var capturedEvents = eventCaptor.getAllValues();
        assertEquals(2, capturedEvents.size());
        capturedEvents.forEach(event -> {
            assertNotNull(event.getHeader());
            assertNotNull(event.getPayload());
            assertEquals(message, event.getPayload());
        });
    }

    @Test
    void handleExternalChannelMessage_runMode_shouldSendToPaperTrackerOnly() {
        // Arrange
        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(ProcessingMode.RUN);
        tracking.setTrackingId(REQUEST_ID);

        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();

        verify(paperTrackerProducer, times(1)).push(eventCaptor.capture());
        verify(paperChannelDryRunProducer, never()).push(any(ExternalChannelEvent.class));
        verify(paperTrackingsDAO, times(1)).retrieveEntityByTrackingId(REQUEST_ID);

        // Verifica l'evento catturato
        ExternalChannelEvent capturedEvent = eventCaptor.getValue();
        assertNotNull(capturedEvent.getHeader());
        assertEquals(message, capturedEvent.getPayload());
    }

    @Test
    void handleExternalChannelMessage_nullAnalogMail_shouldReturnError() {
        // Arrange
        SingleStatusUpdate invalidMessage = new SingleStatusUpdate();
        invalidMessage.setAnalogMail(null);

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(invalidMessage, attributes);

        // Assert
        StepVerifier.create(result)
                .expectError(PaperTrackerException.class)
                .verify();

        verify(paperTrackingsDAO, never()).retrieveEntityByTrackingId(any());
        verify(paperTrackerProducer, never()).push((ExternalChannelEvent) any());
        verify(paperChannelDryRunProducer, never()).push((ExternalChannelEvent) any());
    }

    @Test
    void handleExternalChannelMessage_nullRequestId_shouldReturnError() {
        // Arrange
        PaperProgressStatusEvent mailWithoutRequestId = new PaperProgressStatusEvent();
        mailWithoutRequestId.setRequestId(null);

        SingleStatusUpdate messageWithoutRequestId = new SingleStatusUpdate();
        messageWithoutRequestId.setAnalogMail(mailWithoutRequestId);

        when(paperTrackingsDAO.retrieveEntityByTrackingId(null))
                .thenReturn(Mono.empty());

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(messageWithoutRequestId, attributes);

        // Assert
        StepVerifier.create(result)
                .verifyComplete();
    }

    @Test
    void handleExternalChannelMessage_daoError_shouldPropagateError() {
        // Arrange
        RuntimeException daoException = new RuntimeException("DAO error");
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.error(daoException));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result)
                .expectError(RuntimeException.class)
                .verify();

        verify(paperTrackerProducer, never()).push((ExternalChannelEvent) any());
        verify(paperChannelDryRunProducer, never()).push((ExternalChannelEvent) any());
    }

    @Test
    void buildOutputMessage_shouldPropagateOriginalMessageHeaders() {
        // Arrange
        Map<String, MessageAttributeValue> messageAttributes = Map.of(
                "customHeader1", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("customValue1")
                        .build(),
                "customHeader2", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("12345")
                        .build(),
                "customHeader3", MessageAttributeValue.builder()
                        .dataType("String")
                        .stringValue("true")
                        .build()
        );

        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(ProcessingMode.RUN);
        tracking.setTrackingId(REQUEST_ID);

        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, messageAttributes);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(paperTrackerProducer).push(eventCaptor.capture());

        ExternalChannelEvent event = eventCaptor.getValue();
        assertNotNull(event);
        assertNotNull(event.getHeader());
        assertNotNull(event.getHeader().getMessageAttributes());

        // Verifica che gli header originali siano stati propagati
        var messageAttributesOut = event.getHeader().getMessageAttributes();
        assertTrue(messageAttributesOut.containsKey("customHeader1"));
        assertTrue(messageAttributesOut.containsKey("customHeader2"));
        assertTrue(messageAttributesOut.containsKey("customHeader3"));

        assertEquals("customValue1", messageAttributesOut.get("customHeader1").stringValue());
        assertEquals("12345", messageAttributesOut.get("customHeader2").stringValue());
        assertEquals("true", messageAttributesOut.get("customHeader3").stringValue());

        // Verifica che sia stato aggiunto anche il dryRun header
        assertTrue(messageAttributesOut.containsKey("dryRun"));
        assertEquals("false", messageAttributesOut.get("dryRun").stringValue());
    }


    @Test
    void handleExternalChannelMessage_dryMode_shouldSetDryRunHeaderToTrue() {
        // Arrange
        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(ProcessingMode.DRY);
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(paperChannelDryRunProducer).push(eventCaptor.capture());

        ExternalChannelEvent event = eventCaptor.getValue();
        assertNotNull(event.getHeader().getMessageAttributes());
    }

    @Test
    void handleExternalChannelMessage_runMode_shouldSetDryRunHeaderToFalse() {
        // Arrange
        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(ProcessingMode.RUN);
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(paperTrackerProducer).push(eventCaptor.capture());

        ExternalChannelEvent event = eventCaptor.getValue();
        assertNotNull(event.getHeader().getMessageAttributes());
    }

    // ===== Test filtro eventi duplicati (isDuplicate) =====

    @Test
    void handleExternalChannelMessage_trackingNotFound_duplicate_shouldNotSend() {
        // Arrange
        analogMail.setIsDuplicate(true);
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.empty());

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(paperChannelDryRunProducer, never()).push(any(ExternalChannelEvent.class));
        verify(paperTrackerProducer, never()).push(any(ExternalChannelEvent.class));
    }

    @Test
    void handleExternalChannelMessage_trackingNotFound_notDuplicate_shouldSendToPaperChannelOnly() {
        // Arrange
        analogMail.setIsDuplicate(false);
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.empty());

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(paperChannelDryRunProducer, times(1)).push(any(ExternalChannelEvent.class));
        verify(paperTrackerProducer, never()).push(any(ExternalChannelEvent.class));
    }

    @Test
    void handleExternalChannelMessage_dryMode_duplicate_shouldSendToPaperTrackerOnly() {
        // Arrange
        analogMail.setIsDuplicate(true);
        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(ProcessingMode.DRY);
        tracking.setTrackingId(REQUEST_ID);
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(paperChannelDryRunProducer, never()).push(any(ExternalChannelEvent.class));
        verify(paperTrackerProducer, times(1)).push(any(ExternalChannelEvent.class));
    }

    @Test
    void handleExternalChannelMessage_dryMode_notDuplicate_shouldSendToBothQueues() {
        // Arrange
        analogMail.setIsDuplicate(false);
        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(ProcessingMode.DRY);
        tracking.setTrackingId(REQUEST_ID);
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(paperChannelDryRunProducer, times(1)).push(any(ExternalChannelEvent.class));
        verify(paperTrackerProducer, times(1)).push(any(ExternalChannelEvent.class));
    }

    @Test
    void handleExternalChannelMessage_runMode_notDuplicate_shouldSendToPaperTrackerOnly() {
        // Arrange
        analogMail.setIsDuplicate(false);
        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(ProcessingMode.RUN);
        tracking.setTrackingId(REQUEST_ID);
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(paperTrackerProducer, times(1)).push(any(ExternalChannelEvent.class));
        verify(paperChannelDryRunProducer, never()).push(any(ExternalChannelEvent.class));
    }

    @Test
    void handleExternalChannelMessage_runMode_duplicate_shouldSendToPaperTrackerOnly() {
        // Arrange
        analogMail.setIsDuplicate(true);
        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(ProcessingMode.RUN);
        tracking.setTrackingId(REQUEST_ID);
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(paperTrackerProducer, times(1)).push(any(ExternalChannelEvent.class));
        verify(paperChannelDryRunProducer, never()).push(any(ExternalChannelEvent.class));
    }

    @Test
    void handleExternalChannelMessage_nullProcessingMode_duplicate_shouldSendToPaperTrackerOnly() {
        // Arrange
        analogMail.setIsDuplicate(true);
        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(null);
        tracking.setTrackingId(REQUEST_ID);
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(paperChannelDryRunProducer, never()).push(any(ExternalChannelEvent.class));
        verify(paperTrackerProducer, times(1)).push(any(ExternalChannelEvent.class));
    }

    @Test
    void handleExternalChannelMessage_nullProcessingMode_notDuplicate_shouldSendToBothQueues() {
        // Arrange
        analogMail.setIsDuplicate(false);
        PaperTrackings tracking = new PaperTrackings();
        tracking.setProcessingMode(null);
        tracking.setTrackingId(REQUEST_ID);
        when(paperTrackingsDAO.retrieveEntityByTrackingId(REQUEST_ID))
                .thenReturn(Mono.just(tracking));

        // Act
        Mono<Void> result = service.handleExternalChannelMessage(message, attributes);

        // Assert
        StepVerifier.create(result).verifyComplete();

        verify(paperChannelDryRunProducer, times(1)).push(any(ExternalChannelEvent.class));
        verify(paperTrackerProducer, times(1)).push(any(ExternalChannelEvent.class));
    }
}