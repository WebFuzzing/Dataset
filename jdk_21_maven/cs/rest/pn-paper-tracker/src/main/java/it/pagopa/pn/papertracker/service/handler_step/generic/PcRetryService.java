package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PaperTrackerExceptionHandler;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PcRetryResponse;
import it.pagopa.pn.papertracker.mapper.PaperTrackingsErrorsMapper;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class PcRetryService {

    private final PaperTrackerExceptionHandler paperTrackerExceptionHandler;
    /**
     * Gestione della risposta del Paper Channel alla richiesta di retry.
     * Se il retry è stato trovato, viene creata la nuova entità di tracking con il nuovo pcRetry e
     * aggiornato lo stato dell'entità relativa al pcRetry precedente con lo stato DONE e il nextRequestIdPcretry.
     * Se il retry non è stato trovato, viene gestito l'errore in base al tipo di evento (CON996 o generico).
     * @param pcRetryResponse risposta del Paper Channel alla richiesta di retry
     * @param isCON996 booleano che indica se l'evento è un CON996
     * @param context contesto dell'handler
     * @return Mono(Void)
     */
    public Mono<Void> handlePcRetryResponse(PcRetryResponse pcRetryResponse, Boolean isCON996, HandlerContext context) {
        if (Boolean.TRUE.equals(pcRetryResponse.getRetryFound())) {
            context.setNextRequestIdPcRetry(pcRetryResponse.getRequestId());
            return Mono.empty();
        } else {
            PaperTrackingsErrors paperTrackingsErrors = Boolean.TRUE.equals(isCON996) ? buildErrorForCON996(context) : buildErrorForGeneric(context);
            return paperTrackerExceptionHandler.handleRetryError(paperTrackingsErrors);
        }
    }

    private PaperTrackingsErrors buildErrorForGeneric(HandlerContext context) {
        return PaperTrackingsErrorsMapper.buildPaperTrackingsError(context.getPaperTrackings(),
                context.getPaperProgressStatusEvent().getStatusCode(),
                ErrorCategory.MAX_RETRY_REACHED_ERROR,
                null,
                "Retry not found for trackingId: " + context.getPaperTrackings().getTrackingId(),
                null,
                FlowThrow.RETRY_PHASE,
                ErrorType.ERROR,
                context.getEventId());
    }

    private PaperTrackingsErrors buildErrorForCON996(HandlerContext context) {
        String statusCode = context.getPaperProgressStatusEvent().getStatusCode();
        return PaperTrackingsErrorsMapper.buildPaperTrackingsError(context.getPaperTrackings(),
                statusCode,
                ErrorCategory.NOT_RETRYABLE_EVENT_ERROR,
                null,
                EventStatusCodeEnum.fromKey(statusCode).getStatusCodeDescription(),
                null,
                FlowThrow.NOT_RETRYABLE_EVENT_HANDLER,
                ErrorType.WARNING,
                context.getEventId());
    }
}
