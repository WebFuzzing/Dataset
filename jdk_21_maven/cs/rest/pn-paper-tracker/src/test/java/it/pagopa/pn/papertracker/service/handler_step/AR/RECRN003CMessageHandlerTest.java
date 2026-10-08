package it.pagopa.pn.papertracker.service.handler_step.AR;

import it.pagopa.pn.papertracker.BaseTest;
import it.pagopa.pn.papertracker.exception.PaperTrackerExceptionHandler;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PaperChannelUpdate;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.SendEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackerDryRunOutputsDAO;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsErrorsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Attachment;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Event;
import it.pagopa.pn.papertracker.middleware.eventBridge.EventBridgePublisher;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.middleware.msclient.PaperChannelClient;
import it.pagopa.pn.papertracker.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.papertracker.middleware.queue.consumer.internal.ExternalChannelHandler;
import it.pagopa.pn.papertracker.service.handler_step.RIR.HandlersFactoryRir;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static it.pagopa.pn.papertracker.service.handler_step.TestUtils.getPaperTrackings;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@Slf4j
@TestPropertySource(properties = {
        "pn.paper-tracker.send-output-to-delivery-push=true"
})
class RECRN003CMessageHandlerTest extends BaseTest.WithLocalStack {
    private static final String STATUS_RECRN010 = "RECRN010";
    private static final String STATUS_RECRN011 = "RECRN011";
    private static final String STATUS_RECRN003A = "RECRN003A";
    private static final String STATUS_RECRN003B = "RECRN003B";
    private static final String STATUS_RECRN003C = "RECRN003C";
    private static final String STATUS_PNRN012 = "PNRN012";

    private static final int DAYS_REFINEMENT = 10;

    @Autowired
    private ExternalChannelHandler externalChannelHandler;
    @Autowired
    private PaperTrackingsDAO paperTrackingsDAO;
    @Autowired
    private PaperTrackingsErrorsDAO paperTrackingsErrorsDAO;
    @Autowired
    private PaperTrackerExceptionHandler paperTrackerExceptionHandler;
    @Autowired
    private PaperTrackerDryRunOutputsDAO paperTrackerDryRunOutputsDAO;
    @Autowired
    private HandlersFactoryAr handlersFactoryAr;
    @Autowired
    private HandlersFactoryRir handlersFactoryRir;

    @MockitoBean
    private SafeStorageClient safeStorageClient;
    @MockitoBean
    private PaperChannelClient paperChannelClient;
    @MockitoBean
    private DataVaultClient dataVaultClient;
    @MockitoBean
    private EventBridgePublisher eventBridgePublisher;


    @Test
    void when_RECRN003AGreaterThanRECRN010Of10Days_then_pushPNRN012Status() {
        // Arrange
        var now = Instant.now();
        String eventId = "eventId";
        Event eventMetaRECRN010 = getEventMeta(STATUS_RECRN010, now.minus(DAYS_REFINEMENT + 1, ChronoUnit.DAYS));
        Event eventMetaRECRN011 = getEventMeta(STATUS_RECRN011, now);
        Event eventMetaRECRN003A = getEventMeta(STATUS_RECRN003A, now);
        Event eventMetaRECRN003B = getEventMeta(STATUS_RECRN003B, now);

        when(safeStorageClient.getSafeStoragePresignedUrl(any())).thenReturn(Mono.just("url"));
        when(eventBridgePublisher.publish(any(PaperChannelUpdate.class))).thenReturn(Mono.just(PutEventsResponse.builder().build()));

        String iun = UUID.randomUUID().toString();
        String requestId = "PREPARE_ANALOG_DOMICILE.IUN_" + iun + ".RECINDEX_0.ATTEMPT_0.PCRETRY_0";
        paperTrackingsDAO.putIfAbsent(getPaperTrackings(requestId, List.of(eventMetaRECRN010, eventMetaRECRN011, eventMetaRECRN003A, eventMetaRECRN003B))).block();

        PaperProgressStatusEvent paperRequest = new PaperProgressStatusEvent()
                .requestId(requestId)
                .statusCode(STATUS_RECRN003C)
                .statusDateTime(now.atOffset(ZoneOffset.UTC))
                .clientRequestTimeStamp(OffsetDateTime.now())
                .registeredLetterCode("registeredLetterCode")
                .productType("AR");

        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setAnalogMail(paperRequest);

        ArgumentCaptor<PaperChannelUpdate> capturedSendEvent = ArgumentCaptor.forClass(PaperChannelUpdate.class);

        // Act
        externalChannelHandler.handleExternalChannelMessage(singleStatusUpdate, false, null, eventId, null);

        // Assert
        verify(eventBridgePublisher, times(2)).publish(capturedSendEvent.capture());
        assertNotNull(capturedSendEvent.getAllValues());
        assertEquals(2, capturedSendEvent.getAllValues().size());
        assertEquals(STATUS_PNRN012, Objects.requireNonNull(capturedSendEvent.getAllValues().get(0).getSendEvent()).getStatusDetail());
        assertEquals(StatusCodeEnum.OK, Objects.requireNonNull(capturedSendEvent.getAllValues().get(0).getSendEvent()).getStatusCode());
        assertEquals(STATUS_RECRN003C, Objects.requireNonNull(capturedSendEvent.getAllValues().get(1).getSendEvent()).getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, Objects.requireNonNull(capturedSendEvent.getAllValues().get(1).getSendEvent()).getStatusCode());
    }

    @Test
    void when_RECRN003ALessThanOrEqualToRECRN010Of10Days_then_pushOnQueue() {
        // Arrange
        var now = Instant.now();
        String eventId = "eventId";
        Event eventMetaRECRN010 = getEventMeta(STATUS_RECRN010, now.minus(DAYS_REFINEMENT, ChronoUnit.DAYS));
        Event eventMetaRECRN011 = getEventMeta(STATUS_RECRN011, now);
        Event eventMetaRECRN003A = getEventMeta(STATUS_RECRN003A, now);
        Event eventMetaRECRN003B = getEventMeta(STATUS_RECRN003B, now);

        when(safeStorageClient.getSafeStoragePresignedUrl(any())).thenReturn(Mono.just("url"));
        when(eventBridgePublisher.publish(any(PaperChannelUpdate.class))).thenReturn(Mono.just(PutEventsResponse.builder().build()));

        String iun = UUID.randomUUID().toString();
        String requestId = "PREPARE_ANALOG_DOMICILE.IUN_" + iun + ".RECINDEX_0.ATTEMPT_0.PCRETRY_0";
        paperTrackingsDAO.putIfAbsent(getPaperTrackings(requestId, List.of(eventMetaRECRN010, eventMetaRECRN011, eventMetaRECRN003A, eventMetaRECRN003B))).block();

        PaperProgressStatusEvent paperRequest = new PaperProgressStatusEvent()
                .requestId(requestId)
                .statusCode(STATUS_RECRN003C)
                .statusDateTime(now.atOffset(ZoneOffset.UTC))
                .clientRequestTimeStamp(OffsetDateTime.now())
                .registeredLetterCode("registeredLetterCode")
                .productType("AR");

        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setAnalogMail(paperRequest);

        ArgumentCaptor<PaperChannelUpdate> capturedSendEvent = ArgumentCaptor.forClass(PaperChannelUpdate.class);

        // Act
        externalChannelHandler.handleExternalChannelMessage(singleStatusUpdate, false, null, eventId, null);

        // Assert
        verify(eventBridgePublisher).publish(capturedSendEvent.capture());
        log.info(capturedSendEvent.getAllValues().toString());
        assertNotNull(capturedSendEvent.getAllValues());
        SendEvent sendEvent = capturedSendEvent.getValue().getSendEvent();
        assertNotNull(sendEvent);
        Assertions.assertEquals(StatusCodeEnum.OK, sendEvent.getStatusCode());
        Assertions.assertEquals(STATUS_RECRN003C, sendEvent.getStatusDetail());

    }

    private Event getEventMeta(String statusCode, Instant time) {
        var eventMeta = new Event();
        eventMeta.setId(UUID.randomUUID().toString());
        eventMeta.setStatusCode(statusCode);
        eventMeta.setStatusTimestamp(time);
        eventMeta.setRegisteredLetterCode("registeredLetterCode");
        eventMeta.setRequestTimestamp(time); //aggiunto
        if(statusCode.equalsIgnoreCase("RECRN003B")){
            Attachment attachment = new Attachment();
            attachment.setDocumentType("AR");
            attachment.setUri("test.pdf");
            eventMeta.setAttachments(List.of(attachment));
        }
        return eventMeta;
    }
}
