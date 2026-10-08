package it.pagopa.pn.papertracker.service.handler_step.AR;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.service.handler_step.generic.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HandlersFactoryAr extends AbstractHandlersFactory {

    public HandlersFactoryAr(MetadataUpserter metadataUpserter,
                             CheckTrackingProduct checkTrackingProduct,
                             OutputTargetSender outputTargetSender,
                             FinalEventBuilderAr finalEventBuilder,
                             IntermediateEventsBuilder intermediateEventsBuilder,
                             DematValidatorAr dematValidator,
                             SequenceValidatorAr sequenceValidator,
                             @Qualifier("retrySender")
                             RetrySender retrySender,
                             M10RetryTrigger m10RetryTrigger,
                             NotRetryableErrorInserting notRetryableErrorInserting,
                             DuplicatedEventFiltering duplicatedEventFiltering,
                             CheckTrackingState checkTrackingState,
                             CheckOcrResponse checkOcrResponse,
                             RetrySenderCON996 retrySenderCON996) {
        super(metadataUpserter,
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
                retrySenderCON996);
    }

    @Override
    public ProductType getProductType() { return ProductType.AR; }
}