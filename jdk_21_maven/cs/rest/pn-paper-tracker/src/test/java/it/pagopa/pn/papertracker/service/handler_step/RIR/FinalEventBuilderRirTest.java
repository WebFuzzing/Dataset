package it.pagopa.pn.papertracker.service.handler_step.RIR;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalEventBuilderRirTest {

    @Mock
    DataVaultClient dataVaultClient;

    @Mock
    PaperTrackingsDAO paperTrackingsDAO;

    FinalEventBuilderRir finalEventBuilder;

    HandlerContext handlerContext;

    PaperTrackings paperTrackings;

    private static final String EVENT_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        handlerContext = new HandlerContext();
        handlerContext.setTrackingId("req-123");
        finalEventBuilder = new FinalEventBuilderRir(dataVaultClient, paperTrackingsDAO);
        paperTrackings = new PaperTrackings();
        paperTrackings.setTrackingId("req-123");
        paperTrackings.setProductType(ProductType.AR.getValue());
        paperTrackings.setUnifiedDeliveryDriver("POSTE");
        paperTrackings.setPaperStatus(new PaperStatus());
        paperTrackings.getPaperStatus().setRegisteredLetterCode("RL123");
        paperTrackings.getPaperStatus().setDeliveryFailureCause("M02");
        ValidationFlow validationFlow = new ValidationFlow();
        validationFlow.setSequencesValidationTimestamp(Instant.now());
        paperTrackings.setValidationFlow(validationFlow);
        Event event = new Event();
        event.setStatusCode("RECRI003A");
        event.setDeliveryFailureCause("M02");
        event.setStatusTimestamp(now);
        event.setRequestTimestamp(now);
        event.setId(EVENT_ID + "1");

        Event event1 = new Event();
        event1.setStatusCode("RECRN003B");
        event1.setStatusTimestamp(now);
        event1.setRequestTimestamp(now);
        event1.setId(EVENT_ID + "2");

        Event event2 = new Event();
        event2.setStatusCode("RECRI003C");
        event2.setStatusTimestamp(now);
        event2.setRequestTimestamp(now);
        event2.setId(EVENT_ID);

        paperTrackings.setEvents(List.of(event, event1, event2));
        paperTrackings.getPaperStatus().setValidatedEvents(List.of(EVENT_ID + "1", EVENT_ID + "2", EVENT_ID));
        handlerContext.setPaperTrackings(paperTrackings);
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

    }

    @Test
    void buildRIRFinalEvent_withDiscoveredAddress() {
        // Arrange
        PaperProgressStatusEvent finalEvent = getFinalEvent(RECRI003C.name());
        handlerContext.setPaperProgressStatusEvent(finalEvent);
        handlerContext.setEventId(EVENT_ID);

        // Act
        StepVerifier.create(finalEventBuilder.execute(handlerContext))
                .verifyComplete();

        // Assert
        Assertions.assertEquals(1, handlerContext.getEventsToSend().size());
        Assertions.assertNull(handlerContext.getEventsToSend().getFirst().getDiscoveredAddress());
        Assertions.assertEquals(StatusCodeEnum.OK, handlerContext.getEventsToSend().getFirst().getStatusCode());
        Assertions.assertEquals("M02", handlerContext.getEventsToSend().getFirst().getDeliveryFailureCause());
    }

    @Test
    void buildRIRFinalEvent_withoutDiscoveredAddress() {
        // Arrange
        PaperProgressStatusEvent finalEvent = getFinalEvent(RECRI003C.name());
        handlerContext.setEventId(EVENT_ID);
        handlerContext.setPaperProgressStatusEvent(finalEvent);

        // Act
        StepVerifier.create(finalEventBuilder.execute(handlerContext))
                .verifyComplete();

        // Assert
        Assertions.assertEquals(1, handlerContext.getEventsToSend().size());
        Assertions.assertNull(handlerContext.getEventsToSend().getFirst().getDiscoveredAddress());
        Assertions.assertEquals(StatusCodeEnum.OK, handlerContext.getEventsToSend().getLast().getStatusCode());
        Assertions.assertEquals("M02", handlerContext.getEventsToSend().getFirst().getDeliveryFailureCause());
    }

    @Test
    void buildRIRFinalEvent_withRECRI004C_statusCode() {
        // Arrange
        PaperProgressStatusEvent finalEvent = getFinalEvent(RECRI004C.name());
        handlerContext.setPaperProgressStatusEvent(finalEvent);
        handlerContext.setEventId(EVENT_ID);
        Event event = new Event();
        event.setStatusCode(RECRI004C.name());
        event.setStatusTimestamp(Instant.now());
        event.setRequestTimestamp(Instant.now());
        event.setId(EVENT_ID);
        handlerContext.getPaperTrackings().setEvents(List.of(event));

        // Act
        StepVerifier.create(finalEventBuilder.execute(handlerContext))
                .verifyComplete();

        // Assert
        Assertions.assertEquals(1, handlerContext.getEventsToSend().size());
        String expectedStatus = TrackerUtility.evaluateStatusCodeAndRetrieveStatus(RECRI004C.name(), RECRI004C.name(), handlerContext.getPaperTrackings()).name();
        Assertions.assertNotNull(handlerContext.getEventsToSend().getFirst().getStatusCode());
        Assertions.assertEquals(expectedStatus, handlerContext.getEventsToSend().getFirst().getStatusCode().name());
    }

    @Test
    void buildRIRFinalEvent_withRECRI004C_andM02_shouldSetOkStatus() {
        // Arrange
        PaperProgressStatusEvent finalEvent = getFinalEvent(RECRI004C.name());
        handlerContext.setPaperProgressStatusEvent(finalEvent);
        handlerContext.setEventId(EVENT_ID);
        Event event = new Event();
        event.setStatusCode(RECRI004C.name());
        event.setStatusTimestamp(Instant.now());
        event.setRequestTimestamp(Instant.now());
        event.setId(EVENT_ID);
        handlerContext.getPaperTrackings().setEvents(List.of(event));
        handlerContext.getPaperTrackings().getPaperStatus().setDeliveryFailureCause("M02");
        // Act
        StepVerifier.create(finalEventBuilder.execute(handlerContext))
                .verifyComplete();
        // Assert
        Assertions.assertEquals(1, handlerContext.getEventsToSend().size());
        Assertions.assertEquals(StatusCodeEnum.OK, handlerContext.getEventsToSend().getFirst().getStatusCode());
        String expectedStatus = TrackerUtility.evaluateStatusCodeAndRetrieveStatus(RECRI004C.name(), RECRI004C.name(), handlerContext.getPaperTrackings()).name();
        Assertions.assertEquals(expectedStatus, handlerContext.getEventsToSend().getFirst().getStatusCode().name());
    }

    @Test
    void buildRIRFinalEvent_withRECRI004C_andM06_shouldSetKoStatus() {
        // Arrange
        PaperProgressStatusEvent finalEvent = getFinalEvent(RECRI004C.name());
        handlerContext.setPaperProgressStatusEvent(finalEvent);
        handlerContext.setEventId(EVENT_ID);
        Event event = new Event();
        event.setStatusCode(RECRI004C.name());
        event.setStatusTimestamp(Instant.now());
        event.setRequestTimestamp(Instant.now());
        event.setId(EVENT_ID);
        handlerContext.getPaperTrackings().setEvents(List.of(event));
        handlerContext.getPaperTrackings().getPaperStatus().setDeliveryFailureCause("M06");
        // Act
        StepVerifier.create(finalEventBuilder.execute(handlerContext))
                .verifyComplete();
        // Assert
        Assertions.assertEquals(1, handlerContext.getEventsToSend().size());
        Assertions.assertEquals(StatusCodeEnum.KO, handlerContext.getEventsToSend().getFirst().getStatusCode());
        String expectedStatus = TrackerUtility.evaluateStatusCodeAndRetrieveStatus(RECRI004C.name(), RECRI004C.name(), handlerContext.getPaperTrackings()).name();
        Assertions.assertEquals(expectedStatus, handlerContext.getEventsToSend().getFirst().getStatusCode().name());
    }

    private PaperProgressStatusEvent getFinalEvent(String statusCode) {
        PaperProgressStatusEvent finalEvent = new PaperProgressStatusEvent();
        finalEvent.setStatusCode(statusCode);
        finalEvent.setRequestId("req-123");
        finalEvent.setClientRequestTimeStamp(OffsetDateTime.now());
        finalEvent.setRegisteredLetterCode("RL123");
        finalEvent.setAttachments(new ArrayList<>());
        finalEvent.setStatusDateTime(OffsetDateTime.now());
        finalEvent.setProductType("RIR");
        return finalEvent;
    }
}
