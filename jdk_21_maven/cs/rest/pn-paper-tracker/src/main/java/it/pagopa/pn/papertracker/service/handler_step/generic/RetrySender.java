package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.middleware.msclient.PaperChannelClient;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

/**
 * Gestisce l'invio delle richieste di retry a pn-paper-channel e la successiva
 * elaborazione della risposta ricevuta.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RetrySender implements HandlerStep {

    private final PaperChannelClient paperChannelClient;
    private final PcRetryService pcRetryService;

    /**
     * Invia al Paper Channel la richiesta di retry associata al contesto e delega
     * l'elaborazione della risposta a {@link PcRetryService} (per tutti gli eventi di retry escluso il CON996).
     *
     * @param context contesto contenente i dati del tracking per cui effettuare il retry
     * @return mono che completa al termine dell'elaborazione della risposta
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing RetrySender step for trackingId: {}", context.getTrackingId());

        return paperChannelClient.getPcRetry(context, Boolean.FALSE)
                .doOnError(throwable -> log.error("Error retrieving retry on {} for trackingId: {}",
                        context.getPaperProgressStatusEvent().getStatusCode(),
                        context.getPaperTrackings().getTrackingId(), throwable))
                .flatMap(pcRetryResponse -> pcRetryService.handlePcRetryResponse(pcRetryResponse, Boolean.FALSE, context));
    }
}