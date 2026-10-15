package it.pagopa.pn.papertracker.service.handler_step.AR;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.model.HandlerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalEventBuilderArTest {

    @Mock
    PnPaperTrackerConfigs cfg;

    @Mock
    DataVaultClient dataVaultClient;

    @Mock
    PaperTrackingsDAO paperTrackingsDAO;

    FinalEventBuilderAr finalEventBuilder;

    HandlerContext handlerContext;

    PaperTrackings paperTrackings;

    private static final String EVENT_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        handlerContext = new HandlerContext();
        handlerContext.setTrackingId("req-123");
        finalEventBuilder = new FinalEventBuilderAr(cfg, dataVaultClient, paperTrackingsDAO);
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
        event.setRequestTimestamp(now);
        event.setStatusTimestamp(Instant.now());
        event.setId(EVENT_ID + "1");

        Event event1 = new Event();
        event1.setStatusCode("RECRN005B");
        event1.setStatusTimestamp(now);
        event1.setRequestTimestamp(now);
        event1.setId(EVENT_ID + "2");

        Event event2 = new Event();
        event2.setStatusCode("RECRN005C");
        event2.setStatusTimestamp(now);
        event2.setRequestTimestamp(now);
        event2.setId(EVENT_ID);

        Event event3 = new Event();
        event3.setStatusCode("RECRN004C");
        event3.setStatusTimestamp(now);
        event3.setRequestTimestamp(now);
        event3.setId(EVENT_ID + "5");

        Event event4 = new Event();
        event4.setStatusCode("RECRN003C");
        event4.setStatusTimestamp(now);
        event4.setRequestTimestamp(now);
        event4.setId(EVENT_ID + "6");

        Event event5 = new Event();
        event5.setStatusCode("RECRN002D");
        event5.setStatusTimestamp(Instant.now());
        event5.setDeliveryFailureCause("M02");
        event5.setRequestTimestamp(Instant.now());
        event5.setId(EVENT_ID + "7");

        Event event6 = new Event();
        event6.setStatusCode("RECRN002F");
        event6.setStatusTimestamp(Instant.now());
        event6.setRequestTimestamp(Instant.now());
        event6.setId(EVENT_ID + "4");

        Event event7 = new Event();
        event7.setStatusCode("RECRN010");
        event7.setRequestTimestamp(Instant.now());
        event7.setId(EVENT_ID + "3");

        paperTrackings.setEvents(List.of(event, event1, event2, event3, event4, event5, event6, event7));
        handlerContext.setPaperTrackings(paperTrackings);
    }

    @Test
    void buildFinalEvent_stockStatus_differenceGreater_RECRN005C() {
        // Arrange
        handlerContext.getPaperTrackings().getEvents().getFirst().setStatusCode(RECRN005A.name());
        handlerContext.getPaperTrackings().getEvents().getLast().setStatusTimestamp(Instant.now().minus(40, ChronoUnit.DAYS));
        setValidatedEvents(EVENT_ID + "1");
        PaperProgressStatusEvent finalEvent = getFinalEvent(RECRN005C.name());
        handlerContext.setPaperProgressStatusEvent(finalEvent);
        handlerContext.setEventId(EVENT_ID);
        when(cfg.getCompiutaGiacenzaArDuration()).thenReturn(Duration.ofDays(30));

        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(finalEventBuilder.execute(handlerContext))
                .verifyComplete();

        // Assert
        Assertions.assertEquals(2, handlerContext.getEventsToSend().size());
        Assertions.assertEquals(PNRN012.name(), handlerContext.getEventsToSend().getFirst().getStatusDetail());
        Assertions.assertNull(handlerContext.getEventsToSend().getFirst().getDeliveryFailureCause());
        Assertions.assertEquals(RECRN005C.name(), handlerContext.getEventsToSend().getLast().getStatusDetail());
        Assertions.assertEquals(StatusCodeEnum.PROGRESS, handlerContext.getEventsToSend().getLast().getStatusCode());
    }

    @Test
    void buildFinalEvent_stockStatus_differenceMinor_RECRN005C() {
        // Arrange
        handlerContext.getPaperTrackings().getEvents().getFirst().setStatusCode(RECRN005A.name());
        handlerContext.getPaperTrackings().getEvents().getLast().setStatusTimestamp(Instant.now().minus(25, ChronoUnit.DAYS));
        setValidatedEvents(EVENT_ID + "1");
        PaperProgressStatusEvent finalEvent = getFinalEvent(RECRN005C.name());
        handlerContext.setEventId(EVENT_ID);
        handlerContext.setPaperProgressStatusEvent(finalEvent);

        when(cfg.getCompiutaGiacenzaArDuration()).thenReturn(Duration.ofDays(30));

        // Act
        StepVerifier.create(finalEventBuilder.execute(handlerContext))
                .expectErrorSatisfies(throwable -> {
                    Assertions.assertInstanceOf(PnPaperTrackerValidationException.class, throwable);
                    PnPaperTrackerValidationException ex = (PnPaperTrackerValidationException) throwable;
                    Assertions.assertNotNull(ex.getError());
                    Assertions.assertEquals(paperTrackings.getTrackingId(), ex.getError().getTrackingId());
                    Assertions.assertEquals(finalEvent.getStatusCode(), ex.getError().getEventThrow());
                    Assertions.assertEquals(finalEvent.getProductType(), ex.getError().getProductType());
                    Assertions.assertEquals(ErrorCause.GIACENZA_DATE_ERROR, ex.getError().getDetails().getCause());
                })
                .verify();

        // Assert
        Assertions.assertTrue(handlerContext.getEventsToSend().isEmpty());
    }

    @Test
    void buildFinalEvent_stockStatus_differenceMinor_RECRN003C() {
        // Arrange
        handlerContext.getPaperTrackings().getEvents().getFirst().setStatusCode(RECRN003A.name());
        handlerContext.getPaperTrackings().getEvents().getLast().setStatusTimestamp(Instant.now().minus(5, ChronoUnit.DAYS));
        setValidatedEvents(EVENT_ID + "1");
        PaperProgressStatusEvent finalEvent = getFinalEvent(RECRN003C.name());
        handlerContext.setEventId(EVENT_ID + "6");
        handlerContext.setPaperProgressStatusEvent(finalEvent);
        when(cfg.getRefinementDuration()).thenReturn(Duration.ofDays(10));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(finalEventBuilder.execute(handlerContext))
                .verifyComplete();

        // Assert
        Assertions.assertEquals(1, handlerContext.getEventsToSend().size());
        Assertions.assertEquals(RECRN003C.name(), handlerContext.getEventsToSend().getFirst().getStatusDetail());
        Assertions.assertEquals(StatusCodeEnum.valueOf(RECRN003C.getStatus().name()), handlerContext.getEventsToSend().getFirst().getStatusCode());
    }

    @Test
    void buildFinalEvent_stockStatus_differenceGreater_enableTruncatedDate_RECRN004C() {
        // Arrange
        handlerContext.getPaperTrackings().getEvents().getFirst().setStatusCode(RECRN004A.name());
        handlerContext.getPaperTrackings().getEvents().getLast().setStatusTimestamp(Instant.now().minus(13, ChronoUnit.DAYS));
        setValidatedEvents(EVENT_ID + "1");
        PaperProgressStatusEvent finalEvent = getFinalEvent(RECRN004C.name());
        handlerContext.setEventId(EVENT_ID + "5");
        handlerContext.setPaperProgressStatusEvent(finalEvent);
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

        when(cfg.getRefinementDuration()).thenReturn(Duration.ofDays(10));
        when(cfg.isEnableTruncatedDateForRefinementCheck()).thenReturn(true);

        // Act
        StepVerifier.create(finalEventBuilder.execute(handlerContext))
                .verifyComplete();

        // Assert
        Assertions.assertEquals(2, handlerContext.getEventsToSend().size());
        Assertions.assertEquals(PNRN012.name(), handlerContext.getEventsToSend().getFirst().getStatusDetail());
        Assertions.assertNotNull(handlerContext.getEventsToSend().getFirst().getStatusDateTime());
        Assertions.assertEquals(RECRN004C.name(), handlerContext.getEventsToSend().getLast().getStatusDetail());
        Assertions.assertEquals(StatusCodeEnum.PROGRESS, handlerContext.getEventsToSend().getLast().getStatusCode());
    }

    @Test
    void buildFinalEvent_stockStatusFalse() {
        // Arrange
        handlerContext.getPaperTrackings().getEvents().getFirst().setStatusCode(RECRN002D.name());
        handlerContext.getPaperTrackings().getEvents().getLast().setStatusTimestamp(Instant.now());
        setValidatedEvents(EVENT_ID + "1");
        PaperProgressStatusEvent finalEvent = getFinalEvent(RECRN002F.name());
        handlerContext.setPaperProgressStatusEvent(finalEvent);
        handlerContext.setEventId(EVENT_ID + "4");
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(finalEventBuilder.execute(handlerContext))
                .verifyComplete();

        // Assert
        Assertions.assertEquals(1, handlerContext.getEventsToSend().size());
        Assertions.assertEquals(RECRN002F.name(), handlerContext.getEventsToSend().getFirst().getStatusDetail());
        Assertions.assertEquals("M02", handlerContext.getEventsToSend().getFirst().getDeliveryFailureCause());
    }

    private void setValidatedEvents(String eventId) {
        paperTrackings.getPaperStatus().setValidatedEvents(List.of(eventId, EVENT_ID + 3));
    }

    private PaperProgressStatusEvent getFinalEvent(String statusCode) {
        PaperProgressStatusEvent finalEvent = new PaperProgressStatusEvent();
        finalEvent.setStatusCode(statusCode);
        finalEvent.setRequestId("req-123");
        finalEvent.setClientRequestTimeStamp(OffsetDateTime.now());
        finalEvent.setRegisteredLetterCode("RL123");
        finalEvent.setDeliveryFailureCause("M02");
        finalEvent.setAttachments(new ArrayList<>());
        finalEvent.setStatusDateTime(OffsetDateTime.now());
        finalEvent.setProductType("AR");
        return finalEvent;
    }
}
