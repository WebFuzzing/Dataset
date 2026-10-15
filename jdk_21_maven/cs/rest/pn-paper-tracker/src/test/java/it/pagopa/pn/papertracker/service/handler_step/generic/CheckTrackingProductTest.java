package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Event;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.model.HandlerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.List;


@ExtendWith(MockitoExtension.class)
public class CheckTrackingProductTest {

    @InjectMocks
    private CheckTrackingProduct checkTrackingProduct;

    private HandlerContext context;

    @BeforeEach
    void setUp() {
        PaperProgressStatusEvent paperProgressStatusEvent = new PaperProgressStatusEvent();
        PaperTrackings paperTrackings = new PaperTrackings();
        context = new HandlerContext();
        context.setPaperProgressStatusEvent(paperProgressStatusEvent);
        context.setPaperTrackings(paperTrackings);
    }

    @Test
    void execute_shouldCompleteWhenProductTypeMatch() {

        context.getPaperTrackings().setProductType("AR");
        context.getPaperProgressStatusEvent().setStatusCode("RECRN006");
        context.getPaperProgressStatusEvent().setProductType("AR");

        StepVerifier.create(checkTrackingProduct.execute(context))
                .verifyComplete();
    }

    @Test
    void execute_shouldCompleteWhenProductTypeAll() {

        context.getPaperTrackings().setProductType("AR");
        context.getPaperProgressStatusEvent().setStatusCode("CON998");
        context.getPaperProgressStatusEvent().setProductType("AR");

        StepVerifier.create(checkTrackingProduct.execute(context))
                .verifyComplete();
    }

    @Test
    void execute_shouldReturnErrorWhenProductTypeDontMatchWithStatusCodeALL() {

        Event event = new Event();
        event.setId("eventId");
        event.setStatusCode("CON020");
        OffsetDateTime offsetDateTime = OffsetDateTime.now();

        context.setEventId("eventId");
        context.getPaperTrackings().setProductType("AR");
        context.getPaperTrackings().setEvents(List.of(event));
        context.getPaperProgressStatusEvent().setStatusCode("CON020");
        context.getPaperProgressStatusEvent().setProductType("RIR");
        context.getPaperProgressStatusEvent().setStatusDateTime(offsetDateTime);

        StepVerifier.create(checkTrackingProduct.execute(context))
                .verifyComplete();
    }

    @Test
    void execute_shouldReturnErrorWhenEnumProductTypeDontMatch() {

        Event event = new Event();
        event.setId("eventId");
        event.setStatusCode("RECAG005C");
        OffsetDateTime offsetDateTime = OffsetDateTime.now();

        context.setEventId("eventId");
        context.getPaperTrackings().setProductType("AR");
        context.getPaperTrackings().setEvents(List.of(event));
        context.getPaperProgressStatusEvent().setStatusCode("RECAG005C");
        context.getPaperProgressStatusEvent().setProductType("AR");
        context.getPaperProgressStatusEvent().setStatusDateTime(offsetDateTime);

        StepVerifier.create(checkTrackingProduct.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().contains("Product type mismatch for trackingId null: expected AR, but got 890"))
                .verify();
    }

    @Test
    void execute_shouldReturnErrorWhenPayloadProductTypeDontMatch() {

        Event event = new Event();
        event.setId("eventId");
        event.setStatusCode("RECAG005C");
        OffsetDateTime offsetDateTime = OffsetDateTime.now();

        context.setEventId("eventId");
        context.getPaperTrackings().setProductType("AR");
        context.getPaperTrackings().setEvents(List.of(event));
        context.getPaperProgressStatusEvent().setStatusCode("RECAG005C");
        context.getPaperProgressStatusEvent().setProductType("RIR");
        context.getPaperProgressStatusEvent().setStatusDateTime(offsetDateTime);

        StepVerifier.create(checkTrackingProduct.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().contains("Product type mismatch for trackingId null: expected AR, but got RIR"))
                .verify();
    }
}
