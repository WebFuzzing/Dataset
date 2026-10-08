package it.pagopa.pn.papertracker.service.handler_step._890;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.model.EventTypeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.Handler;
import it.pagopa.pn.papertracker.service.handler_step.HandlerImpl;
import it.pagopa.pn.papertracker.service.handler_step.generic.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HandlersFactory890Test {

    @Mock
    private MetadataUpserter metadataUpserter;

    @Mock
    private CheckTrackingProduct checkTrackingProduct;

    @Mock
    private OutputTargetSender outputTargetSender;

    @Mock
    private FinalEventBuilder890 finalEventBuilder;

    @Mock
    private IntermediateEventsBuilder intermediateEventsBuilder;

    @Mock
    private DematValidator890 dematValidator;

    @Mock
    private SequenceValidator890 sequenceValidator;

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

    @Mock
    private RECAG012EventChecker recag012EventChecker;

    @Mock
    private RECAG012EventBuilder recag012EventBuilder;

    @Mock
    private PendingFinalEventTrigger pendingFinalEventTrigger;

    private HandlersFactory890 handlersFactory;

    @BeforeEach
    void setUp() {
        handlersFactory = new HandlersFactory890(
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
                retrySenderCON996,
                recag012EventChecker,
                recag012EventBuilder,
                pendingFinalEventTrigger
        );
    }

    @Test
    void getProductTypeReturnsCorrectProductType() {
        assertEquals(ProductType._890, handlersFactory.getProductType());
    }

    @Test
    void getDispatcherReturnsStockIntermediateEventHandler() {
        // Arrange & Act
        Function<HandlerContext, Handler> dispatcher = handlersFactory.getDispatcher(EventTypeEnum.STOCK_INTERMEDIATE_EVENT);
        HandlerContext context = mock(HandlerContext.class);
        Handler handler = dispatcher.apply(context);

        // Assert
        assertNotNull(handler);
        assertInstanceOf(HandlerImpl.class, handler);
    }

    @Test
    void getDispatcherReturnsRecag012EventHandler() {
        // Arrange & Act
        Function<HandlerContext, Handler> dispatcher = handlersFactory.getDispatcher(EventTypeEnum.RECAG012_EVENT);
        HandlerContext context = mock(HandlerContext.class);
        Handler handler = dispatcher.apply(context);

        // Assert
        assertNotNull(handler);
        assertInstanceOf(HandlerImpl.class, handler);
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
        StepVerifier.create(handlersFactory.buildFinalEventsHandler(context).execute(context))
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
    void buildOcrResponseHandler890_executesM10RetryTriggerBeforeOutputTargetSender() {
        // Arrange
        HandlerContext context = new HandlerContext();
        when(checkOcrResponse.execute(context)).thenReturn(Mono.empty());
        when(finalEventBuilder.execute(context)).thenReturn(Mono.empty());
        when(recag012EventBuilder.execute(context)).thenReturn(Mono.empty());
        when(outputTargetSender.execute(context)).thenReturn(Mono.empty());
        when(pendingFinalEventTrigger.execute(context)).thenReturn(Mono.empty());
        when(sequenceValidator.execute(context)).thenReturn(Mono.empty());
        when(dematValidator.execute(context)).thenReturn(Mono.empty());
        when(m10RetryTrigger.execute(context)).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(handlersFactory.buildOcrResponseHandler890(context).execute(context))
                .verifyComplete();

        // Assert
        InOrder inOrder = inOrder(checkOcrResponse, finalEventBuilder, m10RetryTrigger, recag012EventBuilder,
                outputTargetSender, pendingFinalEventTrigger, sequenceValidator, dematValidator);
        inOrder.verify(checkOcrResponse).execute(context);
        inOrder.verify(finalEventBuilder).execute(context);
        inOrder.verify(m10RetryTrigger).execute(context);
        inOrder.verify(recag012EventBuilder).execute(context);
        inOrder.verify(outputTargetSender).execute(context);
        inOrder.verify(pendingFinalEventTrigger).execute(context);
        inOrder.verify(sequenceValidator).execute(context);
        inOrder.verify(dematValidator).execute(context);
        inOrder.verify(finalEventBuilder).execute(context);
        inOrder.verify(outputTargetSender).execute(context);
    }

}