package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.mapper.PaperTrackingsErrorsMapper;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ErrorCategory;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ErrorCause;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ErrorType;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.FlowThrow;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

import static it.pagopa.pn.papertracker.utils.TrackerUtility.checkIfIsFinalDemat;
import static it.pagopa.pn.papertracker.utils.TrackerUtility.isInvalidState;

@Component
@RequiredArgsConstructor
@Slf4j
public class CheckTrackingState implements HandlerStep {

    /**
     * Step che effettua un controllo se lo stato del tracking associato all'evento ricevuto
     * è in stato DONE o AWAITING_OCR.
     * Vengono esclusi da questo controllo gli eventi con statusCode che iniziano con "CON".
     * Se il tracking è in uno di questi stati, viene generato un errore di validazione,
     * altrimenti, il flusso prosegue con gli step successivi.
     *
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono(Void)
     */
    public Mono<Void> execute(HandlerContext context) {
        log.info("Executing CheckTrackingState step for trackingId: {}", context.getTrackingId());

        return Mono.just(context)
                .flatMap(ctx -> {
                    String statusCode = TrackerUtility.getStatusCodeFromEventId(ctx.getPaperTrackings(), ctx.getEventId());
                    if (statusCode.startsWith("CON") || checkIfIsFinalDemat(statusCode)) {
                        log.info("StatusCode excluded from CheckTrackingState: {}", statusCode);
                        return Mono.empty();
                    }
                    return isInvalidState(ctx, statusCode)
                            ? createValidationError(ctx, statusCode)
                            : Mono.empty();
                });
    }

    private Mono<Void> createValidationError(HandlerContext ctx, String statusCode) {
        String state = TrackerUtility.isStockStatus890(statusCode)
                ? ctx.getPaperTrackings().getBusinessState().name()
                : ctx.getPaperTrackings().getState().name();

        String errorMsg = String.format("Tracking in state %s, statusCode %s: %s", state, statusCode, ctx.getTrackingId());
        Map<String, Object> additionalDetails = Map.of("statusCode", statusCode,
                "statusTimestamp", Objects.toString(ctx.getPaperProgressStatusEvent().getStatusDateTime(), null)
        );

        return Mono.error(new PnPaperTrackerValidationException(
                errorMsg,
                PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                        ctx.getPaperTrackings(),
                        statusCode,
                        ErrorCategory.INCONSISTENT_STATE,
                        ErrorCause.VALUE_AFTER_REFINEMENT,
                        errorMsg,
                        additionalDetails,
                        FlowThrow.CHECK_TRACKING_STATE,
                        ErrorType.WARNING,
                        ctx.getEventId()
                )
        ));
    }
}
