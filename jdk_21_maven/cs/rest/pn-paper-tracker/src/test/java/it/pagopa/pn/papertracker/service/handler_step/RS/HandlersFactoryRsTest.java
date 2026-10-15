package it.pagopa.pn.papertracker.service.handler_step.RS;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.generic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandlersFactoryRsTest {

    @Mock
    private MetadataUpserter metadataUpserter;

    @Mock
    private CheckTrackingProduct checkTrackingProduct;

    @Mock
    private OutputTargetSender outputTargetSender;

    @Mock
    private FinalEventBuilderRs finalEventBuilder;

    @Mock
    private IntermediateEventsBuilder intermediateEventsBuilder;

    @Mock
    private DematValidatorRs dematValidator;

    @Mock
    private SequenceValidatorRs sequenceValidator;

    @Mock
    private RetrySender retrySender;

    @Mock
    private M10RetryTrigger m10RetryTrigger;

    @Mock
    private NotRetryableErrorInserting notRetryableErrorInserting;

    @Mock
    private DuplicatedEventFiltering duplicatedEventFiltering;

    @Mock
    private CheckTrackingState checkTrackingState;

    @Mock
    private CheckOcrResponse checkOcrResponse;

    @Mock
    private RetrySenderCON996 retrySenderCON996;

    private HandlersFactoryRs handlersFactoryRs;

    @BeforeEach
    void setUp() {
        handlersFactoryRs = new HandlersFactoryRs(
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
    }

    @Test
    void getProductType_returnsRsProductType() {
        // Arrange / Act
        ProductType productType = handlersFactoryRs.getProductType();

        // Assert
        assertEquals(ProductType.RS, productType);
    }

    @Test
    void buildFinalEventsHandler_executesM10RetryTriggerBeforeOutputTargetSender() {
        // Arrange
        HandlerContext context = new HandlerContext();
        when(metadataUpserter.execute(context)).thenReturn(Mono.empty());
        when(checkTrackingProduct.execute(context)).thenReturn(Mono.empty());
        when(checkTrackingState.execute(context)).thenReturn(Mono.empty());
        when(sequenceValidator.execute(context)).thenReturn(Mono.empty());
        when(dematValidator.execute(context)).thenReturn(Mono.empty());
        when(finalEventBuilder.execute(context)).thenReturn(Mono.empty());
        when(m10RetryTrigger.execute(context)).thenReturn(Mono.empty());
        when(outputTargetSender.execute(context)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(handlersFactoryRs.buildFinalEventsHandler(context).execute(context))
                .verifyComplete();

        // Assert
        InOrder inOrder = inOrder(metadataUpserter, checkTrackingProduct, checkTrackingState, sequenceValidator, dematValidator, finalEventBuilder, m10RetryTrigger, outputTargetSender);
        inOrder.verify(metadataUpserter).execute(context);
        inOrder.verify(checkTrackingProduct).execute(context);
        inOrder.verify(checkTrackingState).execute(context);
        inOrder.verify(sequenceValidator).execute(context);
        inOrder.verify(dematValidator).execute(context);
        inOrder.verify(finalEventBuilder).execute(context);
        inOrder.verify(m10RetryTrigger).execute(context);
        inOrder.verify(outputTargetSender).execute(context);
    }

    @Test
    void buildOcrResponseHandler_executesM10RetryTriggerBeforeOutputTargetSender() {
        // Arrange
        HandlerContext context = new HandlerContext();
        when(checkOcrResponse.execute(context)).thenReturn(Mono.empty());
        when(finalEventBuilder.execute(context)).thenReturn(Mono.empty());
        when(m10RetryTrigger.execute(context)).thenReturn(Mono.empty());
        when(outputTargetSender.execute(context)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(handlersFactoryRs.buildOcrResponseHandler(context).execute(context))
                .verifyComplete();

        // Assert
        InOrder inOrder = inOrder(checkOcrResponse, finalEventBuilder, m10RetryTrigger, outputTargetSender);
        inOrder.verify(checkOcrResponse).execute(context);
        inOrder.verify(finalEventBuilder).execute(context);
        inOrder.verify(m10RetryTrigger).execute(context);
        inOrder.verify(outputTargetSender).execute(context);
    }
}
