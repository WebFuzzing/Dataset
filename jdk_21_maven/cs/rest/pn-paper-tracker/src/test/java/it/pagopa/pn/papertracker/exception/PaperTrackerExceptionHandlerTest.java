package it.pagopa.pn.papertracker.exception;

import it.pagopa.pn.papertracker.mapper.PaperTrackingsErrorsMapper;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.service.PaperTrackerTrackingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaperTrackerExceptionHandlerTest {

    @Mock
    private PaperTrackerErrorService paperTrackerErrorService;

    @Mock
    private PaperTrackerTrackingService paperTrackerEventService;

    private PaperTrackerExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new PaperTrackerExceptionHandler(paperTrackerErrorService, paperTrackerEventService);
    }

    @Test
    void handleInternalExceptionSuccessfully() {
        // ARRANGE
        PnPaperTrackerValidationException exception = new PnPaperTrackerValidationException("Validation error", new PaperTrackingsErrors());
        when(paperTrackerErrorService.insertPaperTrackingsError(exception.getError())).thenReturn(Mono.empty());

        // ACT
        Mono<Void> response = exceptionHandler.handleInternalException(exception, 1L);

        // ASSERT
        StepVerifier.create(response)
                .verifyComplete();
        verify(paperTrackerErrorService, times(1)).insertPaperTrackingsError(exception.getError());
    }

    @Test
    void handleStatusCodeErrorFirstREtry(){
        //Arrange
        PnPaperTrackerValidationException exception = new PnPaperTrackerValidationException("Validation error", PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                new PaperTrackings(),
                "RECRN001C",
                ErrorCategory.INCONSISTENT_STATE,
                ErrorCause.VALUES_NOT_FOUND,
                "message",
                null,
                FlowThrow.SEQUENCE_VALIDATION,
                ErrorType.ERROR,
                "eventId"));

        // ACT
        Mono<Void> response = exceptionHandler.handleInternalException(exception, 1L);
        // ASSERT
        StepVerifier.create(response)
                .verifyError(PnPaperTrackerValidationException.class);
        verify(paperTrackerErrorService, times(0)).insertPaperTrackingsError(exception.getError());

    }


    @Test
    void handleStatusCodeErrorSixthRetry(){
        //Arrange
        PnPaperTrackerValidationException exception = new PnPaperTrackerValidationException("Validation error", PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                new PaperTrackings(),
                "RECRN001C",
                ErrorCategory.INCONSISTENT_STATE,
                ErrorCause.VALUES_NOT_FOUND,
                "message",
                null,
                FlowThrow.SEQUENCE_VALIDATION,
                ErrorType.ERROR,
                "eventId"));

        when(paperTrackerErrorService.insertPaperTrackingsError(exception.getError())).thenReturn(Mono.empty());

        // ACT
        Mono<Void> response = exceptionHandler.handleInternalException(exception, 5L);
        // ASSERT
        StepVerifier.create(response)
                .verifyComplete();
        verify(paperTrackerErrorService, times(1)).insertPaperTrackingsError(exception.getError());
    }

    @Test
    void handleOtherError(){
        //Arrange
        PnPaperTrackerValidationException exception = new PnPaperTrackerValidationException("Validation error", PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                new PaperTrackings(),
                "RECRN001C",
                ErrorCategory.ATTACHMENTS_ERROR,
                null,
                "message",
                null,
                FlowThrow.SEQUENCE_VALIDATION,
                ErrorType.ERROR,
                "eventId"));

        when(paperTrackerErrorService.insertPaperTrackingsError(exception.getError())).thenReturn(Mono.empty());

        // ACT
        Mono<Void> response = exceptionHandler.handleInternalException(exception, 1L);
        // ASSERT
        StepVerifier.create(response)
                .verifyComplete();
        verify(paperTrackerErrorService, times(1)).insertPaperTrackingsError(exception.getError());
    }


}