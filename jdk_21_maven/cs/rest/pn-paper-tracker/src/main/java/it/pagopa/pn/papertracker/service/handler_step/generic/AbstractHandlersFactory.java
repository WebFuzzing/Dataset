package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PaperTrackerException;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.model.EventTypeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.Handler;
import it.pagopa.pn.papertracker.service.handler_step.HandlerImpl;
import it.pagopa.pn.papertracker.service.handler_step.HandlersFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractHandlersFactory implements HandlersFactory {
    protected final MetadataUpserter metadataUpserter;
    protected final CheckTrackingProduct checkTrackingProduct;
    protected final OutputTargetSender outputTargetSender;
    protected final GenericFinalEventBuilder finalEventBuilder;
    protected final IntermediateEventsBuilder intermediateEventsBuilder;
    protected final GenericDematValidator dematValidator;
    protected final GenericSequenceValidator sequenceValidator;
    protected final RetrySender retrySender;
    protected final M10RetryTrigger m10RetryTrigger;
    protected final NotRetryableErrorInserting notRetryableErrorInserting;
    protected final DuplicatedEventFiltering duplicatedEventFiltering;
    protected final CheckTrackingState checkTrackingState;
    protected final CheckOcrResponse checkOcrResponse;
    protected final RetrySenderCON996 retrySenderCON996;

    public abstract ProductType getProductType();

    public Function<HandlerContext, Handler> getDispatcher(EventTypeEnum eventType) {
        return switch (eventType) {
            case INTERMEDIATE_EVENT -> this::buildIntermediateEventsHandler;
            case RETRYABLE_EVENT -> this::buildRetryEventHandler;
            case NOT_RETRYABLE_EVENT -> this::buildNotRetryableEventHandler;
            case FINAL_EVENT -> this::buildFinalEventsHandler;
            case SAVE_ONLY_EVENT -> this::buildSaveOnlyEventHandler;
            case OCR_RESPONSE_EVENT -> this::buildOcrResponseHandler;
            case CON996_EVENT -> this::buildCon996EventHandler;
            default -> throw new IllegalStateException("Unexpected value: " + eventType);
        };
    }

    public Handler build(EventTypeEnum eventType, HandlerContext context) {
        var handler = getDispatcher(eventType);
        if (Objects.isNull(handler)) {
            log.error("No handler founded for EventType ={} and productType: [{}] (trackingId={})", eventType, getProductType(), context.getTrackingId());
            throw new PaperTrackerException(String.format("No handler founded for EventType =%s and productType: %s", eventType, getProductType()));
        }
        return handler.apply(context);
    }

    /**
     * Metodo che prende in carico gli eventi finali per poi inviare la risposta a delivery-push.
     * I step da compiere sono i seguenti:
     *  - Upsert metadati e demat
     *  - filtraggio eventi duplicati
     *  - Validazione triplette
     *  - Validazione demat tramite invio di un messaggio all'OCR
     *  - Costruzione evento finale
     *  - Attivazione retry per causale M10 e riallineamento dello stato evento
     *  - Invio evento finale a delivery-push
     *  - aggiornamento stato su PaperTrackings
     *
     * @param context   contesto in cui sono presenti tutti i dati necessari per il processo
     * @return Empty Mono se tutto è andato a buon fine, altrimenti un Mono Error
     */
    @Override
    public Handler buildFinalEventsHandler(HandlerContext context) {
        return new HandlerImpl(
                List.of(
                        metadataUpserter,
                        checkTrackingProduct,
                        checkTrackingState,
                        sequenceValidator,
                        dematValidator,
                        finalEventBuilder,
                        m10RetryTrigger,
                        outputTargetSender
                ));
    }

    /**
     * Metodo che costruisce la lista di steps necessari al processamento di un evento intermedio.
     * I step da compiere sono i seguenti:
     *  - Upsert metadati e demat (se presenti)
     *  - filtraggio eventi duplicati
     *  - Costruzione eventi intermedi
     *  - Invio a pn-delivery-push
     *
     * @param context   contesto in cui sono presenti tutti i dati necessari per il processo
     * @return Empty Mono se tutto è andato a buon fine, altrimenti un Mono Error
     */
    @Override
    public Handler buildIntermediateEventsHandler(HandlerContext context) {
        return new HandlerImpl(
                List.of(
                        metadataUpserter,
                        checkTrackingProduct,
                        checkTrackingState,
                        duplicatedEventFiltering,
                        intermediateEventsBuilder,
                        outputTargetSender
                ));
    }

    /**
     * Metodo che costruisce la lista di steps necessari al processamento di un evento di retry.
     * I step da compiere sono i seguenti:
     *  - Upsert metadati e demat (se presenti)
     *  - filtraggio eventi duplicati
     *  - chiamata PaperChannel per richiedere il nuovo PCRETRY se esistente, e salva la nuova entità se esiste un nuovo PCRETRY
     *  altrimenti inserisce un errore nella tabella degli errori con category MAX_RETRY_REACHED_ERROR
     *  - costruzione dell'evento da inviare a pn-delivery-push
     *  - invio a pn-delivery-push
     *  - aggiornamento stato su PaperTrackings
     *
     * @param context   contesto in cui sono presenti tutti i dati necessari per il processo
     * @return Empty Mono se tutto è andato a buon fine, altrimenti un Mono Error
     */
    @Override
    public Handler buildRetryEventHandler(HandlerContext context) {
        return new HandlerImpl(
                List.of(
                        metadataUpserter,
                        checkTrackingProduct,
                        checkTrackingState,
                        retrySender,
                        intermediateEventsBuilder,
                        outputTargetSender
                ));
    }

    /**
     * Metodo che costruisce la lista di steps necessari al processamento di un evento notRetryable (CON998, CON997, CON995, CON993).
     * I step da compiere sono i seguenti:
     *  - Upsert metadati
     *  - filtraggio eventi duplicati
     *  - inserimento errore nella tabella PaperTrackingsError
     *  - costruzione dell'evento da inviare a pn-delivery-push
     *  - invio a pn-delivery-push
     *
     * @param context   contesto in cui sono presenti tutti i dati necessari per il processo
     * @return Empty Mono se tutto è andato a buon fine, altrimenti un Mono Error
     */
    @Override
    public Handler buildNotRetryableEventHandler(HandlerContext context) {
        return new HandlerImpl(
                List.of(
                        metadataUpserter,
                        checkTrackingProduct,
                        checkTrackingState,
                        duplicatedEventFiltering,
                        notRetryableErrorInserting,
                        intermediateEventsBuilder,
                        outputTargetSender
                ));
    }

    /**
     * Metodo che costruisce la lista di steps necessari al processamento di un evento di risposta della validazione ocr.
     * I step da compiere sono i seguenti:
     *  - Upsert metadati e demat (se presenti)
     *  - Costruzione evento finale
     *  - Invio a pn-delivery-push
     *  - aggiornamento stato su PaperTrackings
     *
     * @param context   contesto in cui sono presenti tutti i dati necessari per il processo
     * @return Empty Mono se tutto è andato a buon fine, altrimenti un Mono Error
     */
    @Override
    public Handler buildOcrResponseHandler(HandlerContext context) {
        return new HandlerImpl(
                List.of(
                        checkOcrResponse,
                        finalEventBuilder,
                        m10RetryTrigger,
                        outputTargetSender
                ));
    }

    @Override
    public Handler buildSaveOnlyEventHandler(HandlerContext context) {
        return new HandlerImpl(
                List.of(
                        metadataUpserter,
                        checkTrackingProduct
                ));
    }

    /**
     * Metodo che costruisce la lista di steps necessari al processamento di un evento CON996.
     * I step da compiere sono i seguenti:
     *  - Upsert metadati
     *  - filtraggio eventi duplicati
     *  - chiamata PaperChannel per richiedere il nuovo PCRETRY se esistente, e salva la nuova entità se esiste un nuovo PCRETRY
     *  altrimenti inserisce un errore nella tabella degli errori con category NOT_RETRYABLE_EVENT_ERROR
     *  - costruzione dell'evento da inviare a pn-delivery-push
     *  - invio a pn-delivery-push
     *  - aggiornamento stato su PaperTrackings
     *
     * @param context   contesto in cui sono presenti tutti i dati necessari per il processo
     * @return Empty Mono se tutto è andato a buon fine, altrimenti un Mono Error
     */
    @Override
    public Handler buildCon996EventHandler(HandlerContext context) {
        return new HandlerImpl(
                List.of(
                        metadataUpserter,
                        checkTrackingProduct,
                        checkTrackingState,
                        duplicatedEventFiltering,
                        retrySenderCON996,
                        intermediateEventsBuilder,
                        outputTargetSender
                ));
    }
}