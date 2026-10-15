package it.pagopa.pn.papertracker.service.handler_step.AR;

import it.pagopa.pn.papertracker.BaseTest;
import it.pagopa.pn.papertracker.exception.PaperTrackerExceptionHandler;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PaperChannelUpdate;
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
class RECRN005CMessageHandlerTest extends BaseTest.WithLocalStack {
    private static final String STATUS_RECRN010 = "RECRN010";
    private static final String STATUS_RECRN011 = "RECRN011";
    private static final String STATUS_RECRN005A = "RECRN005A";
    private static final String STATUS_RECRN005C = "RECRN005C";
    private static final String STATUS_RECRN005B = "RECRN005B";
    private static final String STATUS_PNRN012 = "PNRN012";

    private static final int DAYS_REFINEMENT = 10;
    private static final int STORAGE_DURATION_AR_DAYS = 30;

    private final String eventId = "eventId";

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
    void should_pushPNRN012_when_RECRN005AGreaterOrEqualsRECRN010By30Days (){
        // Arrange
        var now = Instant.parse("2025-04-09T09:02:10Z");
        // minus 30 because the month of now (April) has 30 days
        Event eventMetaRECRN010 = getEventMeta(STATUS_RECRN010,now.minus(STORAGE_DURATION_AR_DAYS, ChronoUnit.DAYS));
        Event eventMetaRECRN005A = getEventMeta(STATUS_RECRN005A, now);
        Event eventMetaRECRN011 = getEventMeta(STATUS_RECRN011, now);
        Event eventMetaRECRN005B = getEventMeta(STATUS_RECRN005B, now);

        when(safeStorageClient.getSafeStoragePresignedUrl(any())).thenReturn(Mono.just("url"));
        when(eventBridgePublisher.publish(any(PaperChannelUpdate.class))).thenReturn(Mono.just(PutEventsResponse.builder().build()));

        String iun = UUID.randomUUID().toString();
        String requestId = "PREPARE_ANALOG_DOMICILE.IUN_" + iun + ".RECINDEX_0.ATTEMPT_0.PCRETRY_0";
        paperTrackingsDAO.putIfAbsent(getPaperTrackings(requestId, List.of(eventMetaRECRN010, eventMetaRECRN011, eventMetaRECRN005A, eventMetaRECRN005B))).block();

        PaperProgressStatusEvent paperRequest = new PaperProgressStatusEvent()
                .requestId(requestId)
                .statusDateTime(now.atOffset(ZoneOffset.UTC))
                .clientRequestTimeStamp(OffsetDateTime.now())
                .registeredLetterCode("registeredLetterCode")
                .productType("AR")
                .statusCode(STATUS_RECRN005C);

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
        assertEquals(STATUS_RECRN005C, Objects.requireNonNull(capturedSendEvent.getAllValues().get(1).getSendEvent()).getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, Objects.requireNonNull(capturedSendEvent.getAllValues().get(1).getSendEvent()).getStatusCode());
    }

    @Test
    void should_savePnEventError_when_RECRN005ALessRECRN010By1Days() {
        // Arrange
        var now = Instant.parse("2025-04-09T09:02:10Z");
        Event eventMetaRECRN010 = getEventMeta(STATUS_RECRN010, now);
        Event eventMetaRECRN005A = getEventMeta(STATUS_RECRN005A, now.minus(1, ChronoUnit.DAYS));
        Event eventMetaRECRN011 = getEventMeta(STATUS_RECRN011, now);
        Event eventMetaRECRN005B = getEventMeta(STATUS_RECRN005B, now);

        when(safeStorageClient.getSafeStoragePresignedUrl(any())).thenReturn(Mono.just("url"));

        String iun = UUID.randomUUID().toString();
        String requestId = "PREPARE_ANALOG_DOMICILE.IUN_" + iun + ".RECINDEX_0.ATTEMPT_0.PCRETRY_0";
        paperTrackingsDAO.putIfAbsent(getPaperTrackings(requestId, List.of(eventMetaRECRN010, eventMetaRECRN011, eventMetaRECRN005A, eventMetaRECRN005B))).block();

        PaperProgressStatusEvent paperRequest = new PaperProgressStatusEvent()
                .requestId(requestId)
                .statusDateTime(now.atOffset(ZoneOffset.UTC))
                .clientRequestTimeStamp(OffsetDateTime.now())
                .registeredLetterCode("registeredLetterCode")
                .productType("AR")
                .statusCode(STATUS_RECRN005C);


        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setAnalogMail(paperRequest);

        // Act
        externalChannelHandler.handleExternalChannelMessage(singleStatusUpdate, true, null, eventId, null);

        // Assert
        verify(eventBridgePublisher, never()).publish(any(PaperChannelUpdate.class));


    }

    // Test troncamento
    @Test
    void should_pushPNRN012_when_RECRN005GreaterOrEqualsRECRN010By30Days_withRemoveTime(){
        // Arrange
        var recrn010StatusDateTime = Instant.parse("2025-01-09T09:02:10Z");
        var recrn005AStatusDateTime = Instant.parse("2025-02-08T06:55:24Z");
        Event eventMetaRECRN010 = getEventMeta(STATUS_RECRN010, recrn010StatusDateTime);
        Event eventMetaRECRN005A = getEventMeta(STATUS_RECRN005A, recrn005AStatusDateTime);
        Event eventMetaRECRN011 = getEventMeta(STATUS_RECRN011, recrn010StatusDateTime);
        Event eventMetaRECRN005B = getEventMeta(STATUS_RECRN005B, recrn005AStatusDateTime);

        when(safeStorageClient.getSafeStoragePresignedUrl(any())).thenReturn(Mono.just("url"));
        when(eventBridgePublisher.publish(any(PaperChannelUpdate.class))).thenReturn(Mono.just(PutEventsResponse.builder().build()));

        String iun = UUID.randomUUID().toString();
        String requestId = "PREPARE_ANALOG_DOMICILE.IUN_" + iun + ".RECINDEX_0.ATTEMPT_0.PCRETRY_0";
        paperTrackingsDAO.putIfAbsent(getPaperTrackings(requestId, List.of(eventMetaRECRN010, eventMetaRECRN011, eventMetaRECRN005A, eventMetaRECRN005B))).block();

        PaperProgressStatusEvent paperRequest = new PaperProgressStatusEvent()
                .requestId(requestId)
                .statusDateTime(recrn005AStatusDateTime.atOffset(ZoneOffset.UTC))
                .clientRequestTimeStamp(OffsetDateTime.now())
                .registeredLetterCode("registeredLetterCode")
                .productType("AR")
                .statusCode(STATUS_RECRN005C);

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
        assertEquals(STATUS_RECRN005C, Objects.requireNonNull(capturedSendEvent.getAllValues().get(1).getSendEvent()).getStatusDetail());
        assertEquals(StatusCodeEnum.PROGRESS, Objects.requireNonNull(capturedSendEvent.getAllValues().get(1).getSendEvent()).getStatusCode());

    }

    // test utile per capire se ci sono problemi con la timezone
    @Test
    void  should_savePnEventError_when_RECRN005LessRECRN010_testTimezone(){
        // Arrange
        var recrn010StatusDateTime = Instant.parse("2025-02-10T09:02:10Z");
        var recrn005AStatusDateTime = Instant.parse("2025-03-07T23:55:24Z");
        Event eventMetaRECRN010 = getEventMeta(STATUS_RECRN010, recrn010StatusDateTime);
        Event eventMetaRECRN005A = getEventMeta(STATUS_RECRN005A, recrn005AStatusDateTime);
        Event eventMetaRECRN011 = getEventMeta(STATUS_RECRN011, recrn010StatusDateTime);
        Event eventMetaRECRN005B = getEventMeta(STATUS_RECRN005B, recrn005AStatusDateTime);

        when(safeStorageClient.getSafeStoragePresignedUrl(any())).thenReturn(Mono.just("url"));

        String iun = UUID.randomUUID().toString();
        String requestId = "PREPARE_ANALOG_DOMICILE.IUN_" + iun + ".RECINDEX_0.ATTEMPT_0.PCRETRY_0";
        paperTrackingsDAO.putIfAbsent(getPaperTrackings(requestId, List.of(eventMetaRECRN010, eventMetaRECRN011, eventMetaRECRN005A, eventMetaRECRN005B))).block();

        PaperProgressStatusEvent paperRequest = new PaperProgressStatusEvent()
                .requestId(requestId)
                .statusDateTime(recrn005AStatusDateTime.atOffset(ZoneOffset.UTC))
                .clientRequestTimeStamp(OffsetDateTime.now())
                .registeredLetterCode("registeredLetterCode")
                .productType("AR")
                .statusCode(STATUS_RECRN005C);

        SingleStatusUpdate singleStatusUpdate = new SingleStatusUpdate();
        singleStatusUpdate.setAnalogMail(paperRequest);


        // Act
        externalChannelHandler.handleExternalChannelMessage(singleStatusUpdate, true, null, eventId, null);

        // Assert
        verify(eventBridgePublisher, never()).publish(any(PaperChannelUpdate.class));
    }

    private Event getEventMeta(String statusCode, Instant time) {
        var eventMeta = new Event();
        eventMeta.setId(UUID.randomUUID().toString());
        eventMeta.setStatusCode(statusCode);
        eventMeta.setStatusTimestamp(time);
        eventMeta.setRegisteredLetterCode("registeredLetterCode");
        eventMeta.setRequestTimestamp(time); //aggiunto
        if(statusCode.equalsIgnoreCase("RECRN005B")){
            Attachment attachment = new Attachment();
            attachment.setDocumentType("Plico");
            attachment.setUri("test.pdf");
            eventMeta.setAttachments(List.of(attachment));
        }
        return eventMeta;
    }
}
