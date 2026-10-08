package it.pagopa.pn.papertracker.service.handler_step.RIR;

import it.pagopa.pn.papertracker.model.EventTypeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.service.handler_step.generic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class HandlersFactoryRirTest {

    @Mock
    private MetadataUpserter metadataUpserter;

    @Mock
    private CheckTrackingProduct checkTrackingProduct;

    @Mock
    private SequenceValidatorRir sequenceValidatorRir;

    @Mock
    private DematValidatorRir dematValidator;

    @Mock
    private FinalEventBuilderRir finalEventBuilder;

    @Mock
    private DuplicatedEventFiltering duplicatedEventFiltering;

    @Mock
    private RetrySender retrySender;

    @Mock
    private M10RetryTrigger m10RetryTrigger;

    @Mock
    private OutputTargetSender outputTargetSender;

    @Mock
    private IntermediateEventsBuilder intermediateEventsBuilder;

    @Mock
    private NotRetryableErrorInserting notRetryableErrorInserting;

    @Mock
    private HandlerStep mockHandlerStep1;

    @Mock
    private HandlerStep mockHandlerStep2;

    @Mock
    private CheckTrackingState checkTrackingState;

    @Mock
    private CheckOcrResponse checkOcrResponse;

    @Mock
    private RetrySenderCON996 retrySenderCON996;

    private HandlersFactoryRir handlersFactoryRir;

    private HandlerContext handlerContext;

    @BeforeEach
    void setUp() {
        handlerContext = new HandlerContext();
        handlersFactoryRir = new HandlersFactoryRir(
                metadataUpserter,
                checkTrackingProduct,
                outputTargetSender,
                finalEventBuilder,
                intermediateEventsBuilder,
                dematValidator,
                sequenceValidatorRir,
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
    void buildEventsHandler_WithValidSteps_ExecutesAllStepsInOrder() {
        // Arrange
        when(metadataUpserter.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingProduct.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingState.execute(handlerContext)).thenReturn(Mono.empty());
        when(sequenceValidatorRir.execute(handlerContext)).thenReturn(Mono.empty());
        when(dematValidator.execute(handlerContext)).thenReturn(Mono.empty());
        when(finalEventBuilder.execute(handlerContext)).thenReturn(Mono.empty());
        when(m10RetryTrigger.execute(handlerContext)).thenReturn(Mono.empty());
        when(outputTargetSender.execute(handlerContext)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(handlersFactoryRir.buildFinalEventsHandler(handlerContext).execute(handlerContext))
                .verifyComplete();

        // Assert
        InOrder inOrder = inOrder(metadataUpserter, sequenceValidatorRir, dematValidator, finalEventBuilder, m10RetryTrigger, outputTargetSender);
        inOrder.verify(metadataUpserter).execute(handlerContext);
        inOrder.verify(sequenceValidatorRir).execute(handlerContext);
        inOrder.verify(dematValidator).execute(handlerContext);
        inOrder.verify(finalEventBuilder).execute(handlerContext);
        inOrder.verify(m10RetryTrigger).execute(handlerContext);
        inOrder.verify(outputTargetSender).execute(handlerContext);
    }


    @Test
    void buildEventsHandler_WhenSecondStepFails_FirstStepStillExecuted() {
        // Arrange
        RuntimeException testException = new RuntimeException("Second step failed");

        when(metadataUpserter.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingProduct.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingState.execute(handlerContext)).thenReturn(Mono.error(testException));

        // Act & Assert
        StepVerifier.create(handlersFactoryRir.build(EventTypeEnum.INTERMEDIATE_EVENT, handlerContext).execute(handlerContext))
                .expectError(RuntimeException.class)
                .verify();

        InOrder inOrder = inOrder(metadataUpserter, checkTrackingState);
        inOrder.verify(metadataUpserter).execute(handlerContext);
        inOrder.verify(checkTrackingState).execute(handlerContext);

    }

    @Test
    void buildFinalEventsHandler_ExecutesSuccessfully() {
        // Arrange
        when(metadataUpserter.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingProduct.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingState.execute(handlerContext)).thenReturn(Mono.empty());
        when(sequenceValidatorRir.execute(handlerContext)).thenReturn(Mono.empty());
        when(dematValidator.execute(handlerContext)).thenReturn(Mono.empty());
        when(finalEventBuilder.execute(handlerContext)).thenReturn(Mono.empty());
        when(m10RetryTrigger.execute(handlerContext)).thenReturn(Mono.empty());
        when(outputTargetSender.execute(handlerContext)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(handlersFactoryRir.buildFinalEventsHandler(handlerContext).execute(handlerContext))
                .verifyComplete();

        // Assert
        InOrder inOrder = inOrder(metadataUpserter, sequenceValidatorRir, dematValidator, finalEventBuilder, m10RetryTrigger, outputTargetSender);
        inOrder.verify(metadataUpserter).execute(handlerContext);
        inOrder.verify(sequenceValidatorRir).execute(handlerContext);
        inOrder.verify(dematValidator).execute(handlerContext);
        inOrder.verify(finalEventBuilder).execute(handlerContext);
        inOrder.verify(m10RetryTrigger).execute(handlerContext);
        inOrder.verify(outputTargetSender).execute(handlerContext);
    }

    @Test
    void buildIntermediateEventsHandler_ExecutesMetadataUpserterAndDeliveryPushSender() {
        // Arrange
        when(metadataUpserter.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingProduct.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingState.execute(handlerContext)).thenReturn(Mono.empty());
        when(outputTargetSender.execute(handlerContext)).thenReturn(Mono.empty());
        when(intermediateEventsBuilder.execute(handlerContext)).thenReturn(Mono.empty());
        when(duplicatedEventFiltering.execute(handlerContext)).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(handlersFactoryRir.buildIntermediateEventsHandler(handlerContext).execute(handlerContext))
                .verifyComplete();

        // Verify both steps were executed in the correct order
        InOrder inOrder = inOrder(metadataUpserter, intermediateEventsBuilder, outputTargetSender);
        inOrder.verify(metadataUpserter).execute(handlerContext);
        inOrder.verify(intermediateEventsBuilder).execute(handlerContext);
        inOrder.verify(outputTargetSender).execute(handlerContext);
    }

    @Test
    void buildRetryEventHandler_ExecutesSuccessfully() {
        // Arrange
        when(metadataUpserter.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingProduct.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingState.execute(handlerContext)).thenReturn(Mono.empty());
        when(retrySender.execute(handlerContext)).thenReturn(Mono.empty());
        when(outputTargetSender.execute(handlerContext)).thenReturn(Mono.empty());
        when(intermediateEventsBuilder.execute(handlerContext)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(handlersFactoryRir.buildRetryEventHandler(handlerContext).execute(handlerContext))
                .verifyComplete();

        // Assert
        InOrder inOrder = inOrder(metadataUpserter, retrySender);
        inOrder.verify(metadataUpserter).execute(handlerContext);
        inOrder.verify(retrySender).execute(handlerContext);
    }

    @Test
    void buildNotRetryableEventHandler_ExecutesSuccessfully() {
        // Arrange
        when(metadataUpserter.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingProduct.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingState.execute(handlerContext)).thenReturn(Mono.empty());
        when(duplicatedEventFiltering.execute(handlerContext)).thenReturn(Mono.empty());
        when(outputTargetSender.execute(handlerContext)).thenReturn(Mono.empty());
        when(intermediateEventsBuilder.execute(handlerContext)).thenReturn(Mono.empty());
        when(notRetryableErrorInserting.execute(handlerContext)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(handlersFactoryRir.buildNotRetryableEventHandler(handlerContext).execute(handlerContext))
                .verifyComplete();

        // Assert
        InOrder inOrder = inOrder(metadataUpserter, checkTrackingProduct, duplicatedEventFiltering, notRetryableErrorInserting, intermediateEventsBuilder, outputTargetSender);
        inOrder.verify(metadataUpserter).execute(handlerContext);
        inOrder.verify(checkTrackingProduct).execute(handlerContext);
        inOrder.verify(duplicatedEventFiltering).execute(handlerContext);
        inOrder.verify(notRetryableErrorInserting).execute(handlerContext);
        inOrder.verify(intermediateEventsBuilder).execute(handlerContext);
        inOrder.verify(outputTargetSender).execute(handlerContext);
    }

    @Test
    void buildOcrResponseHandler_executesM10RetryTriggerBeforeOutputTargetSender() {
        // Arrange
        when(checkOcrResponse.execute(handlerContext)).thenReturn(Mono.empty());
        when(finalEventBuilder.execute(handlerContext)).thenReturn(Mono.empty());
        when(m10RetryTrigger.execute(handlerContext)).thenReturn(Mono.empty());
        when(outputTargetSender.execute(handlerContext)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(handlersFactoryRir.buildOcrResponseHandler(handlerContext).execute(handlerContext))
                .verifyComplete();

        // Assert
        InOrder inOrder = inOrder(checkOcrResponse, finalEventBuilder, m10RetryTrigger, outputTargetSender);
        inOrder.verify(checkOcrResponse).execute(handlerContext);
        inOrder.verify(finalEventBuilder).execute(handlerContext);
        inOrder.verify(m10RetryTrigger).execute(handlerContext);
        inOrder.verify(outputTargetSender).execute(handlerContext);
    }

    @Test
    void buildSaveOnlyEventHandler_ExecutesMetadataUpserter() {
        // Arrange
        when(metadataUpserter.execute(handlerContext)).thenReturn(Mono.empty());
        when(checkTrackingProduct.execute(handlerContext)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(handlersFactoryRir.buildSaveOnlyEventHandler(handlerContext).execute(handlerContext))
                .verifyComplete();

        // Assert
        InOrder inOrder = inOrder(metadataUpserter, checkTrackingProduct);
        inOrder.verify(metadataUpserter).execute(handlerContext);
        inOrder.verify(checkTrackingProduct).execute(handlerContext);
    }
}