package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PaperTrackerExceptionHandler;
import it.pagopa.pn.papertracker.mapper.PaperTrackingsErrorsMapper;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotRetryableErrorInserting implements HandlerStep {

    private final PaperTrackerExceptionHandler paperTrackerExceptionHandler;

    /**
     * Step che costruisce un oggetto `PaperTrackingsErrors`, relativamente ad un evento notRetryable,
     * utilizzando i dati forniti nel contesto
     * e delega la gestione dell'errore al rispettivo handler.
     *
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return {@link Mono<Void>}
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing NotRetryableErrorInserting step for trackingId: {}", context.getTrackingId());

        String statusCode = context.getPaperProgressStatusEvent().getStatusCode();
        PaperTrackingsErrors paperTrackingsErrors = PaperTrackingsErrorsMapper.buildPaperTrackingsError(context.getPaperTrackings(),
                statusCode,
                ErrorCategory.NOT_RETRYABLE_EVENT_ERROR,
                null,
                EventStatusCodeEnum.fromKey(statusCode).getStatusCodeDescription(),
                null,
                FlowThrow.NOT_RETRYABLE_EVENT_HANDLER,
                ErrorType.WARNING,
                context.getEventId()
        );
        return paperTrackerExceptionHandler.handleRetryError(paperTrackingsErrors);
    }
}
