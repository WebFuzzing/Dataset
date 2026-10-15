package it.pagopa.pn.papertracker.service.handler_step.RIR;

import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Event;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.generic.GenericFinalEventBuilder;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECRI004C;


@Component
@Slf4j
public class FinalEventBuilderRir extends GenericFinalEventBuilder implements HandlerStep {

    /**
     * Step che elabora l'evento finale per una raccomandata RIR in base alla logica di business definita.
     * Se l'evento che ha innescato il flusso è un evento finale, viene estratto l'evento dal contesto,
     * viene valutato lo status code e determinato lo stato dell'evento secondo le regole di business.
     * Infine, l'evento viene aggiunto alla lista degli eventi da inviare.
     * Viene utilizzato il metodo di utility evaluateStatusCodeAndRetrieveStatus perché tutti i casi non gestiti esplicitamente (M01, M03, M04)
     * e il caso della deliveryFailureCause = null ricadono comunque nel ramo di default, che restituisce il valore KO previsto dall’enum.
     *
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono<Void>
     */

    private final PaperTrackingsDAO paperTrackingsDAO;

    public FinalEventBuilderRir(DataVaultClient dataVaultClient, PaperTrackingsDAO paperTrackingsDAO) {
        super(dataVaultClient, paperTrackingsDAO);
        this.paperTrackingsDAO = paperTrackingsDAO;
    }

    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing FinalEventBuilderRir for trackingId: {}", context.getTrackingId());

        return Mono.just(TrackerUtility.extractEventFromContext(context))
                .doOnNext(event -> context.setFinalStatusCode(event.getStatusCode()))
                .flatMap(event -> handleFinalEvent(context, event))
                .thenReturn(context)
                .map(ctx -> paperTrackingsDAO.updateItem(ctx.getPaperTrackings().getTrackingId(), getPaperTrackingsToUpdate()))
                .then();
    }

    private Mono<Void> handleFinalEvent(HandlerContext context, Event finalEvent) {
        String statusCode = finalEvent.getStatusCode();
        if (RECRI004C.name().equals(statusCode)) {
            String eventStatus = TrackerUtility.evaluateStatusCodeAndRetrieveStatus(RECRI004C.name(), statusCode, context.getPaperTrackings()).name();
            return addEventToSend(context, finalEvent, eventStatus);
        }
        return addEventToSend(context, finalEvent, EventStatusCodeEnum.fromKey(statusCode).getStatus().name());
    }
}
