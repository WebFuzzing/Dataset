package it.pagopa.pn.papertracker.service.handler_step.RS;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.service.handler_step.generic.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HandlersFactoryRs extends AbstractHandlersFactory {

    public HandlersFactoryRs(MetadataUpserter metadataUpserter,
                             CheckTrackingProduct checkTrackingProduct,
                             OutputTargetSender outputTargetSender,
                             FinalEventBuilderRs finalEventBuilder,
                             IntermediateEventsBuilder intermediateEventsBuilder,
                             DematValidatorRs dematValidator,
                             SequenceValidatorRs sequenceValidator,
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
    public ProductType getProductType() { return ProductType.RS; }
}
