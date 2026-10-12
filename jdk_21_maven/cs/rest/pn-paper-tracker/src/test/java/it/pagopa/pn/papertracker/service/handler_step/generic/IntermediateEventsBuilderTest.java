package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.AttachmentDetails;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.model.HandlerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class IntermediateEventsBuilderTest {

    private HandlerContext context;

    @InjectMocks
    private IntermediateEventsBuilder intermediateEventsBuilder;

    @BeforeEach
    void setUp() {
        context = new HandlerContext();
    }

    @Test
    void execute_shouldAddSendEventToContext_whenFluxEmitsEvent() {
        // Arrange
        context.setPaperProgressStatusEvent(getPaperProgressStatusEvent("RECRN001C", null));

        // Act & Assert
        StepVerifier.create(intermediateEventsBuilder.execute(context))
                .expectSubscription()
                .verifyComplete();

        Assertions.assertEquals(1, context.getEventsToSend().size());
    }

    @Test
    void execute_shouldCompleteWithoutAddingEvent_whenFluxIsEmpty() {
        // Arrange
        context.setPaperProgressStatusEvent(getPaperProgressStatusEvent("RECRN001B", null));

        // Act & Assert
        StepVerifier.create(intermediateEventsBuilder.execute(context))
                .expectSubscription()
                .verifyComplete();

        Assertions.assertEquals(0, context.getEventsToSend().size());
    }

    private PaperProgressStatusEvent getPaperProgressStatusEvent(String statusCode, List<AttachmentDetails> attachments) {
        PaperProgressStatusEvent event = new PaperProgressStatusEvent();
        event.setRequestId("req_123");
        event.setStatusCode(statusCode);
        event.setAttachments(attachments);
        return event;
    }

}
