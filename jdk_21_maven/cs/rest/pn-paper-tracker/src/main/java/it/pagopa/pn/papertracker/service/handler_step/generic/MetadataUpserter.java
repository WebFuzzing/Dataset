package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.DiscoveredAddress;
import it.pagopa.pn.papertracker.mapper.PaperProgressStatusEventMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;

@Component
@RequiredArgsConstructor
@Slf4j
public class MetadataUpserter implements HandlerStep {

    private final PaperTrackingsDAO paperTrackingsDAO;
    private final DataVaultClient dataVaultClient;
    private final PnPaperTrackerConfigs pnPaperTrackerConfigs;

    /**
     * Step utilizzato per l'upsert dei metadati relativi all'evento di avanzamento. Inoltre se è presente l'indirizzo scoperto, si occupa di anonimizzarlo.
     *  Alla fine del processo l'entità aggiornata viene settata nel context insieme al trackingId.
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono(Void)
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing MetadataUpserter step for trackingId: {}", context.getTrackingId());

        return Mono.just(context)
                .flatMap(this::discoveredAddressAnonimization)
                .map(handlerContext -> PaperProgressStatusEventMapper.toPaperTrackings(
                        context.getReworkId(),
                        context.getPaperProgressStatusEvent(),
                        context.getAnonymizedDiscoveredAddressId(),
                        context.getEventId(),
                        context.isDryRunEnabled(),
                        TrackerUtility.checkIfIsFinalDemat(context.getPaperProgressStatusEvent().getStatusCode()),
                        TrackerUtility.checkIfIsP000event(context.getPaperProgressStatusEvent().getStatusCode()),
                        TrackerUtility.checkIfIsInternalEvent(pnPaperTrackerConfigs.getInternalEvents(), context.getPaperProgressStatusEvent().getStatusCode()),
                        TrackerUtility.checkIfIsRecag012event(context.getPaperProgressStatusEvent().getStatusCode())
                ))
                .flatMap(paperTrackings -> paperTrackingsDAO.updateItem(
                        context.getPaperProgressStatusEvent().getRequestId(),
                        paperTrackings
                ))
                .doOnNext(TrackerUtility::checkValidationConfig)//aggiunto per retrocompatibilità
                .doOnNext(paperTrackings -> updateContextWithTrackingInfo(context, paperTrackings))
                .then();
    }

    private void updateContextWithTrackingInfo(HandlerContext context, PaperTrackings paperTrackings) {
        Long eventReceiveCount = paperTrackings.getEvents().stream()
                .filter(event -> event.getId().equalsIgnoreCase(context.getEventId()))
                .count();
        context.setPaperTrackings(paperTrackings);
        context.setTrackingId(paperTrackings.getTrackingId());
        context.setMessageReceiveCount(eventReceiveCount);
    }

    private Mono<HandlerContext> discoveredAddressAnonimization(HandlerContext handlerContext) {
        DiscoveredAddress discoveredAddress = handlerContext.getPaperProgressStatusEvent().getDiscoveredAddress();
        if (Objects.nonNull(discoveredAddress)) {
            return dataVaultClient.anonymizeDiscoveredAddress(handlerContext.getPaperProgressStatusEvent().getRequestId(), discoveredAddress)
                    .doOnNext(handlerContext::setAnonymizedDiscoveredAddressId)
                    .thenReturn(handlerContext);
        }
        return Mono.just(handlerContext);
    }
}