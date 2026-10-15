package it.pagopa.pn.papertracker.service.handler_step._890;

import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Event;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.service.handler_step.generic.GenericFinalEventBuilder;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECAG003C;
import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECAG012;

@Component
@Slf4j
public class FinalEventBuilder890 extends GenericFinalEventBuilder implements HandlerStep {

    private final PaperTrackingsDAO paperTrackingsDAO;

    public FinalEventBuilder890(DataVaultClient dataVaultClient, PaperTrackingsDAO paperTrackingsDAO) {
        super(dataVaultClient, paperTrackingsDAO);
        this.paperTrackingsDAO = paperTrackingsDAO;
    }

    /**
     * Step che elabora l'evento finale (890) in base alla logica di business definita.
     * Se l'evento che ha innescato il flusso è il RECAG012, non viene eseguita alcuna azione e si passa allo step successivo.
     * Questo scenario può presentarsi solo nell'handler dedicato alle response dell OCR
     * Se l'evento che ha innescato il flusso è un evento finale, viene estratto l'evento finale dal contesto,
     * viene valutato lo status code e viene determinato lo stato dell'evento.
     * Infine, l'evento viene aggiunto alla lista degli eventi da inviare.
     * @return Mono(Void)
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("FinalEventBuilder890 execute for trackingId: {}", context.getTrackingId());
        Event finalEvent = TrackerUtility.extractEventFromContext(context);
        if(finalEvent.getStatusCode().equalsIgnoreCase(RECAG012.name())){
            return Mono.empty();
        }
        context.setFinalStatusCode(finalEvent.getStatusCode());
        String statusCode = finalEvent.getStatusCode();
        String eventStatus = TrackerUtility.evaluateStatusCodeAndRetrieveStatus(RECAG003C.name(), statusCode, context.getPaperTrackings()).name();
        return addEventToSend(context, finalEvent, eventStatus)
                .thenReturn(finalEvent)
                .map(sendEvent -> paperTrackingsDAO.updateItem(context.getPaperTrackings().getTrackingId(), getPaperTrackingsToUpdate()))
                .then();
    }
}
