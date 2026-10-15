package it.pagopa.pn.papertracker.service.handler_step.RIR;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.service.handler_step.generic.NotRetryableErrorInserting;
import it.pagopa.pn.papertracker.service.handler_step.generic.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class HandlersFactoryRir extends AbstractHandlersFactory {

    public HandlersFactoryRir(MetadataUpserter metadataUpserter,
                              CheckTrackingProduct checkTrackingProduct,
                              OutputTargetSender outputTargetSender,
                              FinalEventBuilderRir finalEventBuilder,
                              IntermediateEventsBuilder intermediateEventsBuilder,
                              DematValidatorRir dematValidator,
                              SequenceValidatorRir sequenceValidator,
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
    public ProductType getProductType() { return ProductType.RIR; }
}
