package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.SendEvent;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperStatus;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.msclient.PaperChannelClient;
import it.pagopa.pn.papertracker.model.DeliveryFailureCauseEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.*;

/**
 * Attiva il retry verso pn-paper-channel solo quando il tracking presenta la
 * delivery failure cause {@code M10}.
 */
@Component
@Slf4j
public class M10RetryTrigger extends RetrySender {

    private static final Set<String> M10_RETRY_STATUS_CODES = Set.of(
            RECRS002C.name(),
            RECRN002C.name(),
            RECAG003C.name(),
            RECRI004C.name(),
            RECRSI004C.name()
    );

    public M10RetryTrigger(PaperChannelClient paperChannelClient, PcRetryService pcRetryService) {
        super(paperChannelClient, pcRetryService);
    }

    /**
     * Verifica la causale di mancata consegna del tracking e, se corrisponde a
     * {@code M10} e lo status finale è supportato, riallinea gli eventi finali
     * verso uno stato di PROGRESS ed esegue il retry ereditato da {@link RetrySender}.
     *
     * @param context contesto contenente il tracking da verificare
     * @return mono vuoto se la causale non è {@code M10} o lo status finale non è supportato,
     * altrimenti il mono del retry
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        boolean hasM10FailureCause = DeliveryFailureCauseEnum.M10.equals(
                Optional.ofNullable(context.getPaperTrackings())
                        .map(PaperTrackings::getPaperStatus)
                        .map(PaperStatus::getDeliveryFailureCause)
                        .map(DeliveryFailureCauseEnum::fromValue)
                        .orElse(DeliveryFailureCauseEnum.UNKNOWN)
        );

        if (!hasM10FailureCause || !M10_RETRY_STATUS_CODES.contains(context.getFinalStatusCode())) {
            log.info("M10 retry skipped for trackingId: {} because hasM10FailureCause: {} and finalStatusCode: {}",
                    context.getTrackingId(), hasM10FailureCause, context.getFinalStatusCode());
            return Mono.empty();
        }

        context.getEventsToSend().stream()
                .filter(sendEvent -> M10_RETRY_STATUS_CODES.contains(sendEvent.getStatusDetail()))
                .filter(sendEvent -> isFeedbackEvent(context, sendEvent))
                .forEach(sendEvent -> sendEvent.setStatusCode(StatusCodeEnum.PROGRESS));

        log.info("DeliveryFailureCause M10 found for trackingId: {}, executing PcRetryService", context.getTrackingId());
        return super.execute(context);
    }

    private boolean isFeedbackEvent(HandlerContext context, SendEvent sendEvent) {
        return Objects.equals(context.getFinalStatusCode(), sendEvent.getStatusDetail())
                && (sendEvent.getStatusCode() == StatusCodeEnum.OK
                || sendEvent.getStatusCode() == StatusCodeEnum.KO);
    }
}
