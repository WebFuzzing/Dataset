package it.pagopa.pn.papertracker.middleware.queue.consumer.internal;

import it.pagopa.pn.commons.utils.MDCUtils;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.exception.PaperTrackerExceptionHandler;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.model.EventTypeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.generic.HandlersRegistry;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.CustomLog;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.Optional;

import static it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType.ALL;
import static it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType.UNKNOWN;

@Component
@RequiredArgsConstructor
@CustomLog
public class ExternalChannelHandler {

    private final PaperTrackerExceptionHandler paperTrackerExceptionHandler;
    private final HandlersRegistry handlersRegistry; // contiene HandlersFactoryAr, HandlersFactoryRir, ecc.
    private final PnPaperTrackerConfigs pnPaperTrackerConfigs;

    private static final String HANDLING_EVENT_LOG = "Handling {} event for productType: [{}] and event: [{}]";

    /**
     * Riceve i messaggi provenienti dalla coda pn-external_channel_to_paper_tracker e gestisce, invia l'evento all'handlersRegistry per lo
     * smistamento allo specifico handler in base a productType ed eventType
     *
     * @param payload il SingleStatusUpdate contenente le informazioni da processare
     */
    public void handleExternalChannelMessage(SingleStatusUpdate payload, boolean dryRunEnabled, String reworkId, String messageId, String senderId) {
        if (Objects.isNull(payload) || Objects.isNull(payload.getAnalogMail())) {
            log.error("Received null payload or analogMail in ExternalChannelHandler");
            throw new IllegalArgumentException("Payload or analogMail cannot be null");
        }

        String processName = "processExternalChannelMessage";
        MDC.put(MDCUtils.MDC_PN_CTX_REQUEST_ID, payload.getAnalogMail().getRequestId());
        log.logStartingProcess(processName);
        HandlerContext context = initializeContext(payload, dryRunEnabled, reworkId, messageId, senderId);

        var statusCode = payload.getAnalogMail().getStatusCode();
        var productType = resolveProductType(statusCode, payload.getAnalogMail().getProductType());
        var eventType = Optional.ofNullable(EventStatusCodeEnum.fromKey(statusCode)).map(EventStatusCodeEnum::getCodeType).orElse(null);

        logStatusEvent(eventType, productType, statusCode);

        MDCUtils.addMDCToContextAndExecute(handlersRegistry.handleEvent(productType, eventType, context)
                        .doOnSuccess(unused -> log.logEndingProcess(processName))
                        .onErrorResume(PnPaperTrackerValidationException.class, ex -> paperTrackerExceptionHandler.handleInternalException(ex, context.getMessageReceiveCount())))
                        .block();
    }

    private HandlerContext initializeContext(SingleStatusUpdate payload, boolean dryRunEnabled, String reworkId, String messageId, String senderId) {
        HandlerContext context = new HandlerContext();
        context.setTrackingId(payload.getAnalogMail().getRequestId());
        context.setPaperProgressStatusEvent(payload.getAnalogMail());
        context.setEventId(messageId);
        context.setDryRunEnabled(dryRunEnabled);
        context.setReworkId(reworkId);
        context.setRedrive(TrackerUtility.checkIfIsRedrive(senderId, pnPaperTrackerConfigs.getRedriveEnabledDomains()));
        return context;
    }

    private ProductType resolveProductType(String statusCode, String payloadProductType) {
        ProductType eventProductType = Optional.ofNullable(EventStatusCodeEnum.fromKey(statusCode))
                .map(EventStatusCodeEnum::getProductType)
                .orElse(UNKNOWN);

        if (ALL.equals(eventProductType) && StringUtils.hasText(payloadProductType)) {
            return ProductType.fromValue(payloadProductType);
        }
        return eventProductType;
    }

    private void logStatusEvent(EventTypeEnum statusEvent, ProductType productType, String statusCode) {
        log.info(HANDLING_EVENT_LOG, statusEvent, productType, statusCode);
    }

}
