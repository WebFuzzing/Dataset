package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PaperChannelUpdate;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.SendEvent;
import it.pagopa.pn.papertracker.mapper.PaperTrackerDryRunOutputsMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackerDryRunOutputsDAO;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.BusinessState;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperStatus;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsState;
import it.pagopa.pn.papertracker.middleware.eventBridge.EventBridgePublisher;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.LogUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.SendEvent.JSON_PROPERTY_DISCOVERED_ADDRESS;
import static it.pagopa.pn.papertracker.utils.TrackerUtility.setNewStatus;

@Slf4j
@RequiredArgsConstructor
@Component
public class OutputTargetSender implements HandlerStep {

    private final String CLIENT_ID = "pn-delivery-push";
    private final PnPaperTrackerConfigs configs;
    private final PaperTrackerDryRunOutputsDAO paperTrackerDryRunOutputsDAO;
    private final PaperTrackingsDAO paperTrackingsDAO;
    private final EventBridgePublisher eventBridgePublisher;
    private final LogUtility logUtility;

    /**
     * Step che esegue l'invio degli eventi al target di output configurato, coda verso delivery-push oppure tabella di dry-run
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono(Void)
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing OutputTargetSender step for trackingId: {}", context.getTrackingId());

        List<SendEvent> filteredEvent = context.getEventsToSend().stream()
                .filter(sendEvent -> !configs.getSaveAndNotSendToDeliveryPush().contains(sendEvent.getStatusDetail()))
                .toList();

        if (CollectionUtils.isEmpty(filteredEvent)) {
            log.info("No events to send for trackingId: {}. Skipping OutputTargetSender step.", context.getTrackingId());
            return Mono.empty();
        }

        return Flux.fromIterable(filteredEvent)
                .concatMap(sendEvent -> sendToOutputTarget(sendEvent, context))
                .collectList()
                .filter(sendEvent -> StringUtils.hasText(context.getFinalStatusCode()) || StringUtils.hasText(context.getNextRequestIdPcRetry()))
                .map(sendEvent -> getPaperTrackingsDone(context.getNextRequestIdPcRetry(), context.getFinalStatusCode()))
                .flatMap(paperTrackings -> paperTrackingsDAO.updateItem(context.getTrackingId(), paperTrackings))
                .doOnNext(context::setPaperTrackings)
                .then();
    }

    /**
     * Invia i dati specificati al target di output configurato.
     * <p>
     * I target possono essere o la coda pn-external_channel_outputs o la tabella PnPaperTrackerDryRunOutputs
     * Per configurare il target di output si utilizza un flag sendToDeliveryPushFlag
     *
     * @param event evento da salvare nel target di output
     */
    public Mono<SendEvent> sendToOutputTarget(SendEvent event, HandlerContext context) {
        return Mono.just(event)
                .flatMap(sendEvent -> {
                    String anonymizedEvent = logUtility.maskSensitiveData(sendEvent, JSON_PROPERTY_DISCOVERED_ADDRESS);
                    log.info("Sending to output target for event: {}", anonymizedEvent);
                    if (context.isDryRunEnabled()) {
                        log.info("Sending event to PnPaperTrackerDryRunOutputs");
                        return paperTrackerDryRunOutputsDAO.insertOutputEvent(PaperTrackerDryRunOutputsMapper.dtoToEntity(sendEvent, context.getAnonymizedDiscoveredAddressId()));
                    } else {
                        log.info("Sending event to pn-external_channel_outputs");
                        sendEvent.setRequestId(context.getPaperTrackings().getAttemptId());
                        PaperChannelUpdate paperChannelUpdate = new PaperChannelUpdate();
                        paperChannelUpdate.setSendEvent(sendEvent);
                        paperChannelUpdate.setClientId(StringUtils.hasText(context.getPaperTrackings().getAnalogRequestClientId()) ?
                                context.getPaperTrackings().getAnalogRequestClientId() : CLIENT_ID);
                        return eventBridgePublisher.publish(paperChannelUpdate);
                    }
                })
                .thenReturn(event);
    }

    private PaperTrackings getPaperTrackingsDone(String nextRequestIdPcRetry, String finalStatusCode) {
        PaperTrackings paperTrackings = new PaperTrackings();
        setNewStatus(paperTrackings, finalStatusCode, BusinessState.DONE, PaperTrackingsState.DONE);
        if(StringUtils.hasText(nextRequestIdPcRetry)){
            paperTrackings.setNextRequestIdPcretry(nextRequestIdPcRetry);
        }
        if(StringUtils.hasText(finalStatusCode)){
            PaperStatus paperStatus = new PaperStatus();
            paperStatus.setFinalStatusCode(finalStatusCode);
            paperTrackings.setPaperStatus(paperStatus);
        }
        return paperTrackings;
    }

}