package it.pagopa.pn.papertracker.service.handler_step.RIS;

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
public class DematValidatorRisTest {


    @Mock
    private OcrUtility ocrUtility;

    @Mock
    private PaperTrackerErrorService paperTrackerErrorService;

    @Mock
    private HandlerContext context;

    @Mock
    private PaperProgressStatusEvent paperProgressStatusEvent;

    private DematValidatorRis DematValidatorRis;

    @BeforeEach
    void setUp() {
        DematValidatorRis = spy(new DematValidatorRis(ocrUtility, paperTrackerErrorService));
        when(context.getPaperProgressStatusEvent()).thenReturn(paperProgressStatusEvent);
    }

    @Test
    void validateDematRs_whenStatusCodeIsRECRS001C_shouldReturnEmptyMono() {

        when(paperProgressStatusEvent.getStatusCode()).thenReturn(RECRSI003C.name());

        Mono<Void> result = DematValidatorRis.execute(context);
        StepVerifier.create(result)
                .verifyComplete();
        verify(DematValidatorRis, never()).validateDemat(any());
    }

    @Test
    void validateDematRs_whenStatusCodeIsNotExcluded_shouldCallValidateDemat() {

        String otherStatusCode = RECRS005C.name();
        when(paperProgressStatusEvent.getStatusCode()).thenReturn(otherStatusCode);
        doReturn(Mono.empty()).when(DematValidatorRis).validateDemat(context);

        Mono<Void> result = DematValidatorRis.execute(context);
        StepVerifier.create(result)
                .verifyComplete();
        verify(DematValidatorRis, times(1)).validateDemat(context);
    }
}
