package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.BusinessState;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Event;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsState;
import it.pagopa.pn.papertracker.model.HandlerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;

@ExtendWith(MockitoExtension.class)
class CheckTrackingStateTest {

    @InjectMocks
    private CheckTrackingState checkTrackingState;

    private HandlerContext context;

    @BeforeEach
    void setUp() {
        PaperProgressStatusEvent paperProgressStatusEvent = new PaperProgressStatusEvent();
        paperProgressStatusEvent.setStatusDateTime(OffsetDateTime.now());
        PaperTrackings paperTrackings = new PaperTrackings();
        Event event = new Event();
        event.setId("id");
        paperTrackings.setEvents(java.util.List.of(event));
        context = new HandlerContext();
        context.setPaperProgressStatusEvent(paperProgressStatusEvent);
        context.setPaperTrackings(paperTrackings);
        context.setEventId("id");
    }

    @Test
    void execute_shouldCompleteWhenStatusCodeStartsWithCON() {
        // Arrange
        context.getPaperTrackings().getEvents().getFirst().setStatusCode("CON998");

        // Act & Assert
        StepVerifier.create(checkTrackingState.execute(context))
                .verifyComplete();
    }

    @Test
    void execute_shouldThrowExceptionWhenStateIsDone() {
        // Arrange
        context.getPaperTrackings().getEvents().getFirst().setStatusCode("RECRN002A");
        context.getPaperTrackings().setBusinessState(BusinessState.AWAITING_FINAL_STATUS_CODE);
        context.getPaperTrackings().setState(PaperTrackingsState.DONE);

        // Act & Assert
        StepVerifier.create(checkTrackingState.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().contains("Tracking in state DONE"))
                .verify();
    }

    @Test
    void execute_shouldThrowExceptionWhenStateIsDoneForRecAg012() {
        // Arrange
        context.getPaperTrackings().getEvents().getFirst().setStatusCode("RECAG012");
        context.getPaperTrackings().setBusinessState(BusinessState.AWAITING_FINAL_STATUS_CODE);
        context.getPaperTrackings().setState(PaperTrackingsState.DONE);

        // Act & Assert
        StepVerifier.create(checkTrackingState.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().contains("Tracking in state DONE"))
                .verify();
    }

    @Test
    void execute_shouldThrowExceptionWhenStateIsAwaitingOcr() {
        // Arrange
        context.getPaperTrackings().getEvents().getFirst().setStatusCode("RECRN004C");
        context.getPaperTrackings().setBusinessState(BusinessState.AWAITING_FINAL_STATUS_CODE);
        context.getPaperTrackings().setState(PaperTrackingsState.AWAITING_OCR);

        // Act & Assert
        StepVerifier.create(checkTrackingState.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().contains("Tracking in state AWAITING_OCR"))
                .verify();
    }

    @Test
    void execute_shouldThrowExceptionWhenStateIsAwaitingOcrForRecAg012() {
        // Arrange
        context.getPaperTrackings().getEvents().getFirst().setStatusCode("RECAG012");
        context.getPaperTrackings().setBusinessState(BusinessState.AWAITING_FINAL_STATUS_CODE);
        context.getPaperTrackings().setState(PaperTrackingsState.AWAITING_OCR);

        // Act & Assert
        StepVerifier.create(checkTrackingState.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().contains("Tracking in state AWAITING_OCR"))
                .verify();

    }

    @Test
    void execute_shouldCompleteWhenStateIsNotDoneOrAwaitingOcr() {
        // Arrange
        context.getPaperTrackings().getEvents().getFirst().setStatusCode("RECRN006");
        context.getPaperTrackings().setBusinessState(BusinessState.DONE);
        context.getPaperTrackings().setState(PaperTrackingsState.AWAITING_REFINEMENT);

        // Act & Assert
        StepVerifier.create(checkTrackingState.execute(context))
                .verifyComplete();

    }

    @Test
    void execute_shouldCompleteWhenStateIsNotDoneOrAwaitingOcrfRecAG012() {
        // Arrange
        context.getPaperTrackings().getEvents().getFirst().setStatusCode("RECAG012");
        context.getPaperTrackings().setBusinessState(BusinessState.DONE);
        context.getPaperTrackings().setState(PaperTrackingsState.AWAITING_REFINEMENT);

        // Act & Assert
        StepVerifier.create(checkTrackingState.execute(context))
                .verifyComplete();
    }

    @Test
    void execute_shouldCompleteWhenStatusCodeIsFinalDemat() {
        // Arrange
        context.getPaperTrackings().getEvents().getFirst().setStatusCode("RECRS002B");

        // Act & Assert
        StepVerifier.create(checkTrackingState.execute(context))
                .verifyComplete();
    }
}