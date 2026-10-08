package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperStatus;
import it.pagopa.pn.papertracker.middleware.msclient.PaperChannelClient;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PcRetryResponse;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.SendEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class M10RetryTriggerTest {

    @Mock
    private PaperChannelClient paperChannelClient;

    @Mock
    private PcRetryService pcRetryService;

    @InjectMocks
    private M10RetryTrigger m10RetryTrigger;

    @ParameterizedTest
    @ValueSource(strings = {"RECRS002C", "RECRN002C", "RECAG003C", "RECRI004C", "RECRSI004C"})
    void execute_m10FailureCauseWithSupportedFinalStatusCode_convertsFeedbackEventsToProgressAndExecutesRetrySender(String finalStatusCode) {
        // Arrange
        HandlerContext context = contextWithFailureCause("M10");
        context.setFinalStatusCode(finalStatusCode);
        context.setEventsToSend(List.of(
                sendEventWithStatus(StatusCodeEnum.OK, "PNRN012"),
                sendEventWithStatus(StatusCodeEnum.OK, finalStatusCode),
                sendEventWithStatus(StatusCodeEnum.KO, finalStatusCode),
                sendEventWithStatus(StatusCodeEnum.PROGRESS, finalStatusCode)
        ));
        PcRetryResponse pcRetryResponse = new PcRetryResponse();
        when(paperChannelClient.getPcRetry(context, Boolean.FALSE)).thenReturn(Mono.just(pcRetryResponse));
        when(pcRetryService.handlePcRetryResponse(pcRetryResponse, Boolean.FALSE, context)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(m10RetryTrigger.execute(context))
                .verifyComplete();

        // Assert
        assertEquals(
                List.of(StatusCodeEnum.OK, StatusCodeEnum.PROGRESS, StatusCodeEnum.PROGRESS, StatusCodeEnum.PROGRESS),
                context.getEventsToSend().stream().map(SendEvent::getStatusCode).toList()
        );
        verify(paperChannelClient).getPcRetry(context, Boolean.FALSE);
        verify(pcRetryService).handlePcRetryResponse(pcRetryResponse, Boolean.FALSE, context);
        verifyNoMoreInteractions(paperChannelClient, pcRetryService);
    }

    @Test
    void execute_m10FailureCauseWithUnsupportedFinalStatusCode_doesNotExecuteRetrySender() {
        // Arrange
        HandlerContext context = contextWithFailureCause("M10");
        context.setFinalStatusCode("RECRN002A");
        context.setEventsToSend(List.of(sendEventWithStatus(StatusCodeEnum.OK, "RECRN002A")));

        // Act
        StepVerifier.create(m10RetryTrigger.execute(context))
                .verifyComplete();

        // Assert
        assertEquals(StatusCodeEnum.OK, context.getEventsToSend().getFirst().getStatusCode());
        verifyNoInteractions(paperChannelClient, pcRetryService);
    }

    @Test
    void execute_m10FailureCauseAbsent_doesNotExecuteRetrySender() {
        // Arrange
        HandlerContext context = contextWithFailureCause("M09");
        context.setEventsToSend(List.of(sendEventWithStatus(StatusCodeEnum.OK)));

        // Act
        StepVerifier.create(m10RetryTrigger.execute(context))
                .verifyComplete();

        // Assert
        assertEquals(StatusCodeEnum.OK, context.getEventsToSend().get(0).getStatusCode());
        verifyNoInteractions(paperChannelClient, pcRetryService);
    }

    @Test
    void execute_missingFailureCause_doesNotExecuteRetrySender() {
        // Arrange
        HandlerContext context = contextWithFailureCause(null);
        context.setEventsToSend(List.of(sendEventWithStatus(StatusCodeEnum.KO)));

        // Act
        StepVerifier.create(m10RetryTrigger.execute(context))
                .verifyComplete();

        // Assert
        assertEquals(StatusCodeEnum.KO, context.getEventsToSend().get(0).getStatusCode());
        verifyNoInteractions(paperChannelClient, pcRetryService);
    }

    private HandlerContext contextWithFailureCause(String deliveryFailureCause) {
        PaperTrackings paperTrackings = new PaperTrackings();
        paperTrackings.setTrackingId("tracking-id");
        PaperStatus paperStatus = new PaperStatus();
        paperStatus.setDeliveryFailureCause(deliveryFailureCause);
        paperTrackings.setPaperStatus(paperStatus);

        HandlerContext context = new HandlerContext();
        context.setTrackingId("tracking-id");
        context.setPaperTrackings(paperTrackings);
        PaperProgressStatusEvent paperProgressStatusEvent = new PaperProgressStatusEvent();
        paperProgressStatusEvent.setStatusCode("RECRN002A");
        context.setPaperProgressStatusEvent(paperProgressStatusEvent);
        return context;
    }

    private SendEvent sendEventWithStatus(StatusCodeEnum statusCode) {
        return sendEventWithStatus(statusCode, "RECRN002C");
    }

    private SendEvent sendEventWithStatus(StatusCodeEnum statusCode, String statusDetail) {
        SendEvent sendEvent = new SendEvent();
        sendEvent.setStatusCode(statusCode);
        sendEvent.setStatusDetail(statusDetail);
        return sendEvent;
    }
}
