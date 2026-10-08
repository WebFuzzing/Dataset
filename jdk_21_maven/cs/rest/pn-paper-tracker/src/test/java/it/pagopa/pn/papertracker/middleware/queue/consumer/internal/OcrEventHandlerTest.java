package it.pagopa.pn.papertracker.middleware.queue.consumer.internal;

import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataResultPayload;
import it.pagopa.pn.papertracker.exception.PaperTrackerExceptionHandler;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsErrors;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.model.EventTypeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.generic.HandlersRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrEventHandlerTest {

    @Mock
    private PaperTrackingsDAO paperTrackingsDAO;

    @Mock
    private PaperTrackerExceptionHandler paperTrackerExceptionHandler;

    @Mock
    private HandlersRegistry handlersRegistry;

    @InjectMocks
    private OcrEventHandler ocrEventHandler;

    @Test
    void handleOcrMessageThrowsExceptionWhenPayloadIsNull() {
        assertThrows(IllegalArgumentException.class, () -> ocrEventHandler.handleOcrMessage(null));
    }

    @Test
    void handleOcrMessageThrowsExceptionWhenCommandIdIsNull() {
        OcrDataResultPayload payload = mock(OcrDataResultPayload.class);
        when(payload.getCommandId()).thenReturn(null);

        assertThrows(IllegalArgumentException.class, () -> ocrEventHandler.handleOcrMessage(payload));
    }

    @Test
    void handleOcrMessageProcessesValidPayloadSuccessfully() {
        OcrDataResultPayload payload = mock(OcrDataResultPayload.class);
        PaperTrackings paperTrackings = mock(PaperTrackings.class);
        when(payload.getCommandId()).thenReturn("validCommandId");
        when(paperTrackings.getProductType()).thenReturn(ProductType.AR.getValue());
        when(paperTrackingsDAO.retrieveEntityByTrackingId(any())).thenReturn(Mono.just(paperTrackings));
        when(handlersRegistry.handleEvent(any(), any(), any())).thenReturn(Mono.empty());

        ocrEventHandler.handleOcrMessage(payload);

        verify(paperTrackingsDAO).retrieveEntityByTrackingId(any());
        verify(handlersRegistry).handleEvent(any(ProductType.class), eq(EventTypeEnum.OCR_RESPONSE_EVENT), any(HandlerContext.class));
    }

    @Test
    void handleOcrMessageHandlesValidationExceptionGracefully() {
        OcrDataResultPayload payload = mock(OcrDataResultPayload.class);
        when(payload.getCommandId()).thenReturn("validCommandId");
        when(paperTrackingsDAO.retrieveEntityByTrackingId(any())).thenReturn(Mono.error(new PnPaperTrackerValidationException("Validation error", new PaperTrackingsErrors())));
        when(paperTrackerExceptionHandler.handleInternalException(any(), any())).thenReturn(Mono.empty());

        ocrEventHandler.handleOcrMessage(payload);

        verify(paperTrackerExceptionHandler).handleInternalException(any(PnPaperTrackerValidationException.class), any());
    }
}
