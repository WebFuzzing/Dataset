package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.mapper.PaperTrackingsErrorsMapper;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

import static it.pagopa.pn.papertracker.utils.TrackerUtility.createAffectedEventsMap;


@Component
@Slf4j
public class CheckTrackingProduct implements HandlerStep {

    /**
     * Step che effettua un controllo di coerenza tra il tipo di prodotto associato al tracking e quello atteso per lo statusCode dell'evento ricevuto &&
     * quello contenuto nel payload.
     * Se il tipo di prodotto non corrisponde, viene generato un errore di validazione.
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono<Void>
     */

    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing CheckTrackingProduct for trackingId: {}", context.getTrackingId());

        String paperTrackingProduct = context.getPaperTrackings().getProductType();
        String payloadProduct = context.getPaperProgressStatusEvent().getProductType();
        String statusCode = context.getPaperProgressStatusEvent().getStatusCode();
        String productType = EventStatusCodeEnum.fromKey(statusCode).getProductType().getValue();

        if (productType.equals(ProductType.ALL.getValue())) {
            return Mono.empty();
        }

        if (!paperTrackingProduct.equals(payloadProduct) || !paperTrackingProduct.equals(productType)) {
            String errorMsg = String.format("Product type mismatch for trackingId %s: expected %s, but got %s",
                    context.getTrackingId(), paperTrackingProduct,
                    !paperTrackingProduct.equals(payloadProduct) ? payloadProduct : productType);

            Map<String, Object> additionalDetails = createAffectedEventsMap(false,
                    List.of(TrackerUtility.extractEventFromContext(context)));

            return Mono.error(new PnPaperTrackerValidationException(
                    errorMsg,
                    PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                            context.getPaperTrackings(),
                            context.getPaperProgressStatusEvent().getStatusCode(),
                            ErrorCategory.INCONSISTENT_STATE,
                            ErrorCause.VALUES_NOT_MATCHING,
                            errorMsg,
                            additionalDetails,
                            FlowThrow.CHECK_TRACKING_PRODUCT,
                            ErrorType.WARNING,
                            context.getEventId()
                    )));
        }

        return Mono.empty();
    }
}