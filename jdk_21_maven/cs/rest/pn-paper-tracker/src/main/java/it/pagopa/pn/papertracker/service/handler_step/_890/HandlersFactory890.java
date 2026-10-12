package it.pagopa.pn.papertracker.service.handler_step._890;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.model.EventTypeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.Handler;
import it.pagopa.pn.papertracker.service.handler_step.HandlerImpl;
import it.pagopa.pn.papertracker.service.handler_step.generic.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Slf4j
public class HandlersFactory890 extends AbstractHandlersFactory {

    private final RECAG012EventChecker recag012EventChecker;
    private final RECAG012EventBuilder recag012EventBuilder;
    private final PendingFinalEventTrigger pendingFinalEventTrigger;

    public HandlersFactory890(
            MetadataUpserter metadataUpserter,
            CheckTrackingProduct checkTrackingProduct,
            OutputTargetSender outputTargetSender,
            FinalEventBuilder890 finalEventBuilder,
            IntermediateEventsBuilder intermediateEventsBuilder,
            DematValidator890 dematValidator,
            SequenceValidator890 sequenceValidator,
            @Qualifier("retrySender")
            RetrySender retrySender,
            M10RetryTrigger m10RetryTrigger,
            NotRetryableErrorInserting notRetryableErrorInserting,
            DuplicatedEventFiltering duplicatedEventFiltering,
            CheckTrackingState checkTrackingState,
            CheckOcrResponse checkOcrResponse,
            RetrySenderCON996 retrySenderCON996,
            RECAG012EventChecker recag012EventChecker,
            RECAG012EventBuilder recag012EventBuilder,
            PendingFinalEventTrigger pendingFinalEventTrigger
    ) {
        super(
                metadataUpserter,
                checkTrackingProduct,
                outputTargetSender,
                finalEventBuilder,
                intermediateEventsBuilder,
                dematValidator,
                sequenceValidator,
                retrySender,
                m10RetryTrigger,
                notRetryableErrorInserting,
                duplicatedEventFiltering,
                checkTrackingState,
                checkOcrResponse,
                retrySenderCON996
        );
        this.recag012EventChecker = recag012EventChecker;
        this.recag012EventBuilder = recag012EventBuilder;
        this.pendingFinalEventTrigger = pendingFinalEventTrigger;
    }


    @Override
    public ProductType getProductType() {
        return ProductType._890;
    }

    @Override
    public Function<HandlerContext, Handler> getDispatcher(EventTypeEnum eventType) {
        return switch (eventType) {
            case STOCK_INTERMEDIATE_EVENT -> this::buildStockIntermediateEventHandler;
            case RECAG012_EVENT -> this::buildRecag012EventHandler;
            case OCR_RESPONSE_EVENT -> this::buildOcrResponseHandler890;
            default -> super.getDispatcher(eventType);
        };
    }

    public Handler buildStockIntermediateEventHandler(HandlerContext context) {
        return new HandlerImpl(
                List.of(
                        metadataUpserter,
                        checkTrackingState,
                        duplicatedEventFiltering,
                        recag012EventChecker,
                        intermediateEventsBuilder,
                        recag012EventBuilder,
                        outputTargetSender
                ));
    }

    public Handler buildRecag012EventHandler(HandlerContext context) {
        return new HandlerImpl(
                List.of(
                        metadataUpserter,
                        checkTrackingState,
                        duplicatedEventFiltering,
                        recag012EventChecker,
                        recag012EventBuilder,
                        outputTargetSender
                ));
    }

    public Handler buildOcrResponseHandler890(HandlerContext context) {
        return new HandlerImpl(
                List.of(
                        checkOcrResponse,
                        finalEventBuilder,
                        m10RetryTrigger,
                        recag012EventBuilder,
                        outputTargetSender,
                        pendingFinalEventTrigger,
                        sequenceValidator,
                        dematValidator,
                        finalEventBuilder,
                        outputTargetSender
                ));
    }
}
