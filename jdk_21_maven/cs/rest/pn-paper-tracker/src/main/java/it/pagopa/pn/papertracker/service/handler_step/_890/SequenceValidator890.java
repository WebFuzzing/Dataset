package it.pagopa.pn.papertracker.service.handler_step._890;

import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.model.sequence.SequenceConfig;
import it.pagopa.pn.papertracker.model.sequence.SequenceConfiguration;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.mapper.PaperTrackingsErrorsMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.service.handler_step.generic.GenericSequenceValidator;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@Component
@Slf4j
public class SequenceValidator890 extends GenericSequenceValidator implements HandlerStep {

    private final PaperTrackingsDAO paperTrackingsDAO;

    public SequenceValidator890(PaperTrackingsDAO paperTrackingsDAO, PaperTrackerErrorService paperTrackerErrorService) {
        super(paperTrackingsDAO, paperTrackerErrorService);
        this.paperTrackingsDAO = paperTrackingsDAO;
    }

    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("SequenceValidator890 execute for trackingId: {}", context.getTrackingId());
        SequenceConfig sequenceConfig;
        boolean isStock890;
        if (Objects.nonNull(context.getPaperProgressStatusEvent())) {
            isStock890 = TrackerUtility.isStockStatus890(context.getPaperProgressStatusEvent().getStatusCode());
            sequenceConfig = SequenceConfiguration.getConfig(context.getPaperProgressStatusEvent().getStatusCode());
        } else {
            Event event = TrackerUtility.extractEventFromContext(context);
            isStock890 = TrackerUtility.isStockStatus890(event.getStatusCode());
            sequenceConfig = SequenceConfiguration.getConfig(event.getStatusCode());

        }
        Boolean strictFinalValidationStock890 = context.getPaperTrackings().getValidationConfig().getStrictFinalValidationStock890();

        return Mono.just(context.getPaperTrackings())
                .flatMap(paperTrackings -> {
                    if (!isStock890) {
                        return Mono.just(paperTrackings);
                    }
                   return checkState(context)
                            .filter(Boolean::booleanValue)
                            .map(ignored -> paperTrackings);
                })
                .flatMap(paperTrackings -> validateSequence(paperTrackings, context, sequenceConfig, isStock890 ? strictFinalValidationStock890 : true))
                .doOnNext(context::setPaperTrackings)
                .then();
    }

    private Mono<Boolean> checkState(HandlerContext context) {
        PaperTrackingsState state = context.getPaperTrackings().getState();
        log.info("Current state for trackingId {}: {}", context.getTrackingId(), state);
        return switch (state) {
            case DONE -> Mono.just(true);
            case AWAITING_REFINEMENT, AWAITING_REWORK_EVENTS -> Mono.error( new PnPaperTrackerValidationException(
                    "invalid AWAITING_REFINEMENT state for stock 890",
                    PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                            context.getPaperTrackings(),
                            context.getPaperProgressStatusEvent().getStatusCode(),
                            ErrorCategory.INCONSISTENT_STATE,
                            ErrorCause.STOCK_890_REFINEMENT_MISSING,
                            "invalid AWAITING_REFINEMENT state for stock 890",
                            Map.of("statusCode", context.getPaperProgressStatusEvent().getStatusCode(),
                                "statusTimestamp", context.getPaperProgressStatusEvent().getStatusDateTime().toString()
                            ),
                            FlowThrow.SEQUENCE_VALIDATION,
                            ErrorType.ERROR,
                            context.getEventId()
                    )
            ));
            case AWAITING_OCR -> {
                log.info("Awaiting OCR response for refinement, updating business state to AWAITING_REFINEMENT_OCR for trackingId: {}", context.getTrackingId());
                yield paperTrackingsDAO.updateItem(context.getTrackingId(), getPaperTrackingsToUpdate(context.getEventId()))
                        .doOnNext(paperTrackings -> context.setStopExecution(true))
                        .thenReturn(false);
            }
            case KO -> Mono.error(new PnPaperTrackerValidationException(
                    "Refinement process reached KO state, cannot proceed with final event validation",
                    PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                            context.getPaperTrackings(),
                            context.getPaperProgressStatusEvent().getStatusCode(),
                            ErrorCategory.INCONSISTENT_STATE,
                            ErrorCause.STOCK_890_REFINEMENT_ERROR,
                            "Refinement process reached KO state, cannot proceed with final event validation",
                            Map.of("statusCode", context.getPaperProgressStatusEvent().getStatusCode(),
                                    "statusTimestamp", context.getPaperProgressStatusEvent().getStatusDateTime().toString()
                            ),
                            FlowThrow.SEQUENCE_VALIDATION,
                            ErrorType.ERROR,
                            context.getEventId()
                    )
            ));
            case AWAITING_FINAL_STATUS_CODE -> Mono.error(new PnPaperTrackerValidationException(
                    "Invalid state for processing stock 890 final event",
                    PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                            context.getPaperTrackings(),
                            context.getPaperProgressStatusEvent().getStatusCode(),
                            ErrorCategory.INVALID_STATE_FOR_STOCK_890,
                            ErrorCause.STOCK_890_REFINEMENT_ERROR,
                            String.format("Invalid state %s for processing stock 890 final event", state),
                            null,
                            FlowThrow.SEQUENCE_VALIDATION,
                            ErrorType.ERROR,
                            context.getEventId()
                    )));
        };
    }

    private PaperTrackings getPaperTrackingsToUpdate(String eventId) {
        PaperTrackings paperTrackings = new PaperTrackings();
        paperTrackings.setBusinessState(BusinessState.AWAITING_REFINEMENT_OCR);
        paperTrackings.setPendingFinalEventId(eventId);
        return paperTrackings;
    }
}
