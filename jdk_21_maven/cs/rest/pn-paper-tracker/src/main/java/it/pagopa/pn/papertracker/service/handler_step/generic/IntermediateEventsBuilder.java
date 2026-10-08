package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.mapper.SendEventMapper;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class IntermediateEventsBuilder implements HandlerStep {

    /**
     * Step che gestisce la logica di creazione degli eventi intermedi in base agli eventi validati presenti nel tracciamento.
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono(Void)
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing IntermediateEventsBuilder step for trackingId: {}", context.getTrackingId());

        return SendEventMapper.createSendEventsFromPaperProgressStatusEvent(context.getPaperProgressStatusEvent())
                .doOnNext(sendEvent -> context.getEventsToSend().add(sendEvent))
                .then();
    }

}