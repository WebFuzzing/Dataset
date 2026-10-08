package it.pagopa.pn.papertracker.service.handler_step.RS;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.utils.OcrUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DematValidatorRsTest {

    @Mock
    private OcrUtility ocrUtility;

    @Mock
    private PaperTrackerErrorService paperTrackerErrorService;

    @Mock
    private HandlerContext context;

    @Mock
    private PaperProgressStatusEvent paperProgressStatusEvent;

    private DematValidatorRs dematValidatorRs;

    @BeforeEach
    void setUp() {
        dematValidatorRs = spy(new DematValidatorRs(ocrUtility, paperTrackerErrorService));
        when(context.getPaperProgressStatusEvent()).thenReturn(paperProgressStatusEvent);
    }

    @Test
    void validateDematRs_whenStatusCodeIsRECRS001C_shouldReturnEmptyMono() {

        when(paperProgressStatusEvent.getStatusCode()).thenReturn(RECRS001C.name());

        Mono<Void> result = dematValidatorRs.execute(context);
        StepVerifier.create(result)
                .verifyComplete();
        verify(dematValidatorRs, never()).validateDemat(any());
    }

    @Test
    void validateDematRs_whenStatusCodeIsRECRS003C_shouldReturnEmptyMono() {

        when(paperProgressStatusEvent.getStatusCode()).thenReturn(RECRS003C.name());

        Mono<Void> result = dematValidatorRs.execute(context);
        StepVerifier.create(result)
                .verifyComplete();
        verify(dematValidatorRs, never()).validateDemat(any());
    }

    @Test
    void validateDematRs_whenStatusCodeIsNotExcluded_shouldCallValidateDemat() {

        String otherStatusCode = RECRS005C.name();
        when(paperProgressStatusEvent.getStatusCode()).thenReturn(otherStatusCode);
        doReturn(Mono.empty()).when(dematValidatorRs).validateDemat(context);

        Mono<Void> result = dematValidatorRs.execute(context);
        StepVerifier.create(result)
                .verifyComplete();
        verify(dematValidatorRs, times(1)).validateDemat(context);
    }
}