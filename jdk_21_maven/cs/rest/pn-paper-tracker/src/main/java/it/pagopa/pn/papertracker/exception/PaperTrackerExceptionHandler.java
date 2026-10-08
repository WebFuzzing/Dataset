package it.pagopa.pn.papertracker.exception;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.service.PaperTrackerTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static it.pagopa.pn.papertracker.utils.TrackerUtility.setDematValidationTimestamp;
import static it.pagopa.pn.papertracker.utils.TrackerUtility.setNewStatus;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaperTrackerExceptionHandler {

    private final PaperTrackerErrorService paperTrackerErrorService;
    private final PaperTrackerTrackingService paperTrackerTrackingService;

    /**
     * Intercetta le eccezioni di tipo {@link PnPaperTrackerValidationException}.
     * <p>
     * Per ogni eccezione intercettata, effettua una putItem sulla tabella PaperTrackingsError
     */
    public Mono<Void> handleInternalException(final PnPaperTrackerValidationException ex, Long messageReceiveCount) {
        return handleError(ex.getError(), messageReceiveCount, ex);
    }

    public Mono<Void> handleRetryError(PaperTrackingsErrors paperTrackingsErrors) {
        return handleError(paperTrackingsErrors, null, null);
    }

    public Mono<Void> handleError(PaperTrackingsErrors paperTrackingsErrors, Long messageReceiveCount, PnPaperTrackerValidationException ex) {
        boolean isStatusCodeError = Objects.nonNull(paperTrackingsErrors.getDetails()) && ErrorCause.VALUES_NOT_FOUND.equals(paperTrackingsErrors.getDetails().getCause());

        if (isStatusCodeError) {
            if (messageReceiveCount < 5) {
                // Retry SQS
                return Mono.error(ex);
            }
            log.error("Max retries reached for status code error, inserting error and updating PaperTrackings state to KO for trackingId: {}", paperTrackingsErrors.getTrackingId());
        }

        // Salva l'errore
        return insertErrorAndUpdateTrackingsEntity(paperTrackingsErrors);
    }

    private Mono<Void> insertErrorAndUpdateTrackingsEntity(PaperTrackingsErrors paperTrackingsErrors){
        return paperTrackerErrorService.insertPaperTrackingsError(paperTrackingsErrors)
                .filter(errors -> paperTrackingsErrors.getType().equals(ErrorType.ERROR))
                .doOnDiscard(PaperTrackingsErrors.class, errors -> log.info("Skipped updating PaperTrackings entity for error with type Warning"))
                .map(unused -> {
                    PaperTrackings paperTrackingsToUpdate = new PaperTrackings();
                    String statusCode = paperTrackingsErrors.getEventThrow();
                    setNewStatus(paperTrackingsToUpdate,statusCode, BusinessState.KO, PaperTrackingsState.KO);
                    setDematValidationTimestampIfNeeded(paperTrackingsErrors, paperTrackingsToUpdate,statusCode);
                    return paperTrackingsToUpdate;
                })
                .flatMap(paperTrackingsToUpdate -> paperTrackerTrackingService.updatePaperTrackingsStatus(paperTrackingsErrors.getTrackingId(), paperTrackingsToUpdate))
                .doOnError(throwable -> log.error("Error inserting entity into PaperTrackingsErrors: {}", throwable.getMessage(), throwable));
    }

    private static void setDematValidationTimestampIfNeeded(PaperTrackingsErrors paperTrackingsErrors, PaperTrackings paperTrackingsToUpdate, String statusCode) {
        if (Objects.nonNull(paperTrackingsErrors.getDetails().getCause()) && paperTrackingsErrors.getDetails().getCause().equals(ErrorCause.OCR_KO)) {
           setDematValidationTimestamp(paperTrackingsToUpdate, statusCode);
        }
    }

}
