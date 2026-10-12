package it.pagopa.pn.papertracker.service.handler_step.RIS;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.service.handler_step.generic.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class HandlersFactoryRis extends AbstractHandlersFactory {

    public HandlersFactoryRis(MetadataUpserter metadataUpserter,
                              CheckTrackingProduct checkTrackingProduct,
                              OutputTargetSender outputTargetSender,
                              FinalEventBuilderRis finalEventBuilder,
                              IntermediateEventsBuilder intermediateEventsBuilder,
                              DematValidatorRis dematValidator,
                              SequenceValidatorRis sequenceValidator,
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
    public ProductType getProductType() { return ProductType.RIS; }
}
