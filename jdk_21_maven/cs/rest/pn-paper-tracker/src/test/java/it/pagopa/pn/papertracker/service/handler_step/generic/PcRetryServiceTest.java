package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PaperTrackerExceptionHandler;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PcRetryResponse;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.HandlerContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.CON996;
import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECRN006;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PcRetryServiceTest {

    @InjectMocks
    private PcRetryService pcRetryService;

    @Mock
    private PaperTrackerExceptionHandler paperTrackerExceptionHandler;

    @Test
    void handlePcRetryResponseCon996RetryFound() {
        //ARRANGE
        PcRetryResponse response = getPcRetryResponse(true);
        HandlerContext context = getHandlerContext(CON996.name());

        //ACT
        StepVerifier.create(pcRetryService.handlePcRetryResponse(response, Boolean.TRUE, context))
                .verifyComplete();

        //ASSERT
        Assertions.assertEquals(response.getRequestId(), context.getNextRequestIdPcRetry());
        verify(paperTrackerExceptionHandler, never()).handleRetryError(any());
    }

    @Test
    void handlePcRetryResponseCon996RetryNotFound() {
        //ARRANGE
        PcRetryResponse response = getPcRetryResponse(false);
        HandlerContext context = getHandlerContext(CON996.name());
        ArgumentCaptor<PaperTrackingsErrors> captor = ArgumentCaptor.forClass(PaperTrackingsErrors.class);
        when(paperTrackerExceptionHandler.handleRetryError(captor.capture())).thenReturn(Mono.empty());

        //ACT
        StepVerifier.create(pcRetryService.handlePcRetryResponse(response, Boolean.TRUE, context))
                .verifyComplete();

        //ASSERT
        PaperTrackingsErrors paperTrackingsErrors = captor.getValue();
        Assertions.assertEquals(ErrorCategory.NOT_RETRYABLE_EVENT_ERROR, paperTrackingsErrors.getErrorCategory());
        verify(paperTrackerExceptionHandler, times(1)).handleRetryError(any());
    }

    @Test
    void handlePcRetryResponseRetryFound() {
        //ARRANGE
        PcRetryResponse response = getPcRetryResponse(true);
        HandlerContext context = getHandlerContext(RECRN006.name());

        //ACT
        StepVerifier.create(pcRetryService.handlePcRetryResponse(response, Boolean.FALSE, context))
                .verifyComplete();

        //ASSERT
        Assertions.assertEquals(response.getRequestId(), context.getNextRequestIdPcRetry());
        verify(paperTrackerExceptionHandler, never()).handleRetryError(any());
    }

    @Test
    void handlePcRetryResponseRetryNotFound() {
        //ARRANGE
        PcRetryResponse response = getPcRetryResponse(false);
        HandlerContext context = getHandlerContext(RECRN006.name());
        ArgumentCaptor<PaperTrackingsErrors> captor = ArgumentCaptor.forClass(PaperTrackingsErrors.class);
        when(paperTrackerExceptionHandler.handleRetryError(captor.capture())).thenReturn(Mono.empty());

        //ACT
        StepVerifier.create(pcRetryService.handlePcRetryResponse(response, Boolean.FALSE, context))
                .verifyComplete();

        //ASSERT
        PaperTrackingsErrors paperTrackingsErrors = captor.getValue();
        Assertions.assertEquals(ErrorCategory.MAX_RETRY_REACHED_ERROR, paperTrackingsErrors.getErrorCategory());
        verify(paperTrackerExceptionHandler, times(1)).handleRetryError(captor.capture());
    }


    private HandlerContext getHandlerContext(String statusCode) {
        HandlerContext context = new HandlerContext();
        PaperProgressStatusEvent paperProgressStatusEvent = new PaperProgressStatusEvent();
        paperProgressStatusEvent.setStatusCode(statusCode);
        context.setPaperProgressStatusEvent(paperProgressStatusEvent);
        PaperTrackings paperTrackings = new PaperTrackings();
        paperTrackings.setProductType(ProductType.AR.getValue());
        paperTrackings.setAttemptId("requestId");
        context.setPaperTrackings(paperTrackings);
        return context;
    }

    private PcRetryResponse getPcRetryResponse(boolean found) {
        PcRetryResponse response = new PcRetryResponse();
        response.setRetryFound(found);
        response.setParentRequestId("parentRequestId");
        if (found) {
            response.setRequestId("requestId");
            response.setPcRetry("1");
            response.setDeliveryDriverId("unifiedDeliveryDriverId");
        }
        return response;
    }

}
