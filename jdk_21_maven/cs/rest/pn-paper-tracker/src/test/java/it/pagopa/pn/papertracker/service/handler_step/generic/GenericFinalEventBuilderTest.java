package it.pagopa.pn.papertracker.service.handler_step.generic;

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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GenericFinalEventBuilderTest {

    @Mock
    DataVaultClient dataVaultClient;

    @Mock
    PaperTrackingsDAO paperTrackingsDAO;

    GenericFinalEventBuilder genericFinalEventBuilder;

    HandlerContext handlerContext;

    PaperTrackings paperTrackings;

    private static final String EVENT_ID = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        handlerContext = new HandlerContext();
        handlerContext.setTrackingId("req-123");
        genericFinalEventBuilder = new GenericFinalEventBuilder(dataVaultClient, paperTrackingsDAO) { };
        paperTrackings = new PaperTrackings();
        paperTrackings.setTrackingId("req-123");
        paperTrackings.setProductType(ProductType.AR.getValue());
        paperTrackings.setUnifiedDeliveryDriver("POSTE");
        paperTrackings.setPaperStatus(new PaperStatus());
        paperTrackings.getPaperStatus().setRegisteredLetterCode("RL123");
        paperTrackings.getPaperStatus().setDeliveryFailureCause("F01");
        ValidationFlow validationFlow = new ValidationFlow();
        validationFlow.setSequencesValidationTimestamp(Instant.now());
        paperTrackings.setValidationFlow(validationFlow);
        Event event = new Event();
        event.setStatusCode("RECRI004A");
        event.setStatusTimestamp(now);
        event.setRequestTimestamp(now);
        event.setId(EVENT_ID + "1");

        Event event1 = new Event();
        event1.setStatusCode("RECRI004C");
        event1.setStatusTimestamp(now);
        event1.setRequestTimestamp(now);
        event1.setId(EVENT_ID + "2");

        Event event2 = new Event();
        event2.setStatusCode("RECRI003C");
        event2.setStatusTimestamp(now);
        event2.setRequestTimestamp(now);
        event2.setId(EVENT_ID);

        Event event3 = new Event();
        event3.setStatusCode("RECRI003A");
        event3.setStatusTimestamp(now);
        event3.setRequestTimestamp(now);
        event3.setId(EVENT_ID);

        paperTrackings.setEvents(List.of(event, event1, event2, event3));
        paperTrackings.getPaperStatus().setValidatedEvents(List.of(EVENT_ID + "1", EVENT_ID + "2", EVENT_ID));
        handlerContext.setPaperTrackings(paperTrackings);

        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));
    }

    @Test
    void genericFinalEvent_RECRI003C() {
        // Arrange
        handlerContext.setPaperProgressStatusEvent(getFinalEvent("RECRI003C"));
        handlerContext.setEventId(EVENT_ID);

        // Act
        StepVerifier.create(genericFinalEventBuilder.execute(handlerContext))
                .verifyComplete();

        // Assert
        Assertions.assertEquals(1, handlerContext.getEventsToSend().size());
        Assertions.assertEquals(RECRI003C.name(), handlerContext.getEventsToSend().getFirst().getStatusDetail());
        Assertions.assertEquals(StatusCodeEnum.OK, handlerContext.getEventsToSend().getFirst().getStatusCode());
    }

    @Test
    void genericFinalEvent_RECRI004C() {
        // Arrange
        handlerContext.setPaperProgressStatusEvent(getFinalEvent("RECRI004C"));
        handlerContext.setEventId(EVENT_ID + "2");

        // Act
        StepVerifier.create(genericFinalEventBuilder.execute(handlerContext))
                .verifyComplete();

        // Assert
        Assertions.assertEquals(1, handlerContext.getEventsToSend().size());
        Assertions.assertEquals(RECRI004C.name(), handlerContext.getEventsToSend().getFirst().getStatusDetail());
        Assertions.assertEquals(StatusCodeEnum.KO, handlerContext.getEventsToSend().getFirst().getStatusCode());
    }

    private PaperProgressStatusEvent getFinalEvent(String statusCode) {
        PaperProgressStatusEvent finalEvent = new PaperProgressStatusEvent();
        finalEvent.setStatusCode(statusCode);
        finalEvent.setRequestId("req-123");
        finalEvent.setClientRequestTimeStamp(OffsetDateTime.now());
        finalEvent.setRegisteredLetterCode("RL123");
        finalEvent.setAttachments(new ArrayList<>());
        finalEvent.setStatusDateTime(OffsetDateTime.now());
        return finalEvent;
    }
}
