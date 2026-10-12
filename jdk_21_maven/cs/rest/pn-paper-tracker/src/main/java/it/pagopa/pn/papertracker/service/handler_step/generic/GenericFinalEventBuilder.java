package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.SendEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.papertracker.mapper.SendEventMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Event;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ValidationFlow;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Slf4j
@RequiredArgsConstructor
public abstract class GenericFinalEventBuilder implements HandlerStep {

    private final DataVaultClient dataVaultClient;
    private final PaperTrackingsDAO paperTrackingsDAO;

    /**
     * Step che elabora l'evento finale in base alla logica di business definita.
     * Viene semplicemente aggiunto alla lista degli eventi da inviare.
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono(Void)
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing GenericFinalEventBuilder step for trackingId: {}", context.getTrackingId());

        Event finalEvent = TrackerUtility.extractEventFromContext(context);
        return addEventToSend(context, finalEvent, EventStatusCodeEnum.fromKey(finalEvent.getStatusCode()).getStatus().name())
                .thenReturn(finalEvent)
                .doOnNext(event -> context.setFinalStatusCode(finalEvent.getStatusCode()))
                .map(sendEvent -> paperTrackingsDAO.updateItem(context.getPaperTrackings().getTrackingId(), getPaperTrackingsToUpdate()))
                .then();
    }

    protected Mono<Void> addEventToSend(HandlerContext ctx, Event finalEvent, String status) {
        return buildSendEvent(ctx, finalEvent, StatusCodeEnum.valueOf(status), finalEvent.getStatusCode(), finalEvent.getStatusTimestamp().atOffset(ZoneOffset.UTC))
                .doOnNext(event -> ctx.getEventsToSend().add(event))
                .then();
    }

    protected PaperTrackings getPaperTrackingsToUpdate() {
        PaperTrackings paperTrackings = new PaperTrackings();
        ValidationFlow validationFlow = new ValidationFlow();
        validationFlow.setFinalEventBuilderTimestamp(Instant.now());
        paperTrackings.setValidationFlow(validationFlow);
        return paperTrackings;
    }

    protected Flux<SendEvent> buildSendEvent(HandlerContext context,
                                             Event source,
                                             StatusCodeEnum status,
                                             String logicalStatus,
                                             OffsetDateTime ts) {
        return SendEventMapper.createSendEventsFromEventEntity(context.getTrackingId(), source, status, logicalStatus, ts)
                .flatMap(sendEvent -> enrichWithDeliveryFailureCauseAndDiscoveredAddress(context, sendEvent));
    }

    private Mono<SendEvent> enrichWithDeliveryFailureCauseAndDiscoveredAddress(HandlerContext context, SendEvent sendEvent) {
        sendEvent.setDeliveryFailureCause(context.getPaperTrackings().getPaperStatus().getDeliveryFailureCause());
        return enrichWithDiscoveredAddress(context, sendEvent);
    }

    protected Mono<SendEvent> enrichWithDiscoveredAddress(HandlerContext context, SendEvent sendEvent) {
        if (StringUtils.isBlank(context.getPaperTrackings().getPaperStatus().getAnonymizedDiscoveredAddress())) {
            return Mono.just(sendEvent);
        }
        context.setAnonymizedDiscoveredAddressId(context.getPaperTrackings().getPaperStatus().getAnonymizedDiscoveredAddress());
        return dataVaultClient.deAnonymizeDiscoveredAddress(context.getPaperTrackings().getTrackingId(), context.getPaperTrackings().getPaperStatus().getAnonymizedDiscoveredAddress())
                .map(SendEventMapper::toAnalogAddress)
                .doOnNext(sendEvent::setDiscoveredAddress)
                .thenReturn(sendEvent);
    }

}
