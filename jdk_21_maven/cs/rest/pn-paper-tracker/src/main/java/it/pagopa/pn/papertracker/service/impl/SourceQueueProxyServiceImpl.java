package it.pagopa.pn.papertracker.service.impl;

import it.pagopa.pn.papertracker.exception.PaperTrackerException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.queue.model.CustomEventHeader;
import it.pagopa.pn.papertracker.middleware.queue.model.ExternalChannelEvent;
import it.pagopa.pn.papertracker.middleware.queue.producer.ExternalChannelToPaperChannelDryRunMomProducer;
import it.pagopa.pn.papertracker.middleware.queue.producer.ExternalChannelToPaperTrackerMomProducer;
import it.pagopa.pn.papertracker.service.SourceQueueProxyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static it.pagopa.pn.papertracker.utils.QueueConst.*;

/**
 * Service che gestisce il routing degli eventi provenienti da pn-ec in base alla modalità operativa.
 * <p>
 * Implementa la logica di proxy tra pn-ec e i componenti paper-tracker e paper-channel,
 * determinando il flusso di elaborazione in base alle all'attributo processingMode presente nella tabella pn-PaperTrackings.
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SourceQueueProxyServiceImpl implements SourceQueueProxyService {
    private static final String DRY_RUN_KEY = "dryRun";

    private final ExternalChannelToPaperTrackerMomProducer paperTrackerProducer;
    private final ExternalChannelToPaperChannelDryRunMomProducer paperChannelDryRunProducer;
    private final PaperTrackingsDAO paperTrackingsDAO;

    /**
     * Gestisce un messaggio proveniente da pn-ec applicando le regole di routing in base al processingMode.
     * <p>
     * Le regole applicate sono:
     * <ul>
     *   <li>Spedizione NON presente su pn-PaperTrackings -> evento inoltrato solo a pn-paper-channel (se non duplicato)</li>
     *   <li>Spedizione presente e modalità DRY -> evento inoltrato a pn-paper-channel (se non duplicato) e pn-paper-tracker</li>
     *   <li>Spedizione presente e modalità RUN -> evento inoltrato solo a pn-paper-tracker</li>
     *   <li>Spedizione presente e processingMode null -> evento inoltrato a pn-paper-channel (se non duplicato) e pn-paper-tracker</li>
     * </ul>
     * </p>
     *
     * @param message il messaggio da pn-ec
     * @param messageAttributes attributi del messaggio
     * @return Mono<Void>
     */
    public Mono<Void> handleExternalChannelMessage(
            SingleStatusUpdate message,
            Map<String, MessageAttributeValue> messageAttributes
    ) {
        String requestId;
        try {
            requestId = message.getAnalogMail().getRequestId();
        } catch (NullPointerException e) {
            return Mono.error(new PaperTrackerException("Malformed event from external-channel", e));
        }

        log.info("Routing message - requestId: {}, statusCode: {}, isDuplicate: {}, clientRequestTimeStamp: {}",
                requestId, 
                message.getAnalogMail().getStatusCode(), 
                message.getAnalogMail().getIsDuplicate(),
                message.getAnalogMail().getClientRequestTimeStamp());

        return paperTrackingsDAO.retrieveEntityByTrackingId(requestId)
                // caso: tracking NON trovato
                .switchIfEmpty(handlePaperChannelEvent(message, messageAttributes).then(Mono.empty()))
                // caso: tracking trovato
                .flatMap(tracking -> handleByProcessingMode(tracking, message, messageAttributes))
                .then();
    }

    /**
     * Gestisce un evento quando il tracking non è presente su pn-PaperTrackings.
     * L'evento viene inoltrato a pn-paper-channel in modalità dry-run solo se non è un duplicato
     * (isDuplicate == false o isDuplicate == null).
     *
     * @param event l'evento da inoltrare
     * @return Mono<Void>
     */
    private Mono<Void> handlePaperChannelEvent(SingleStatusUpdate event,
                                               Map<String, MessageAttributeValue> messageAttributes) {
        return Mono.fromRunnable(() -> {
            if (shouldForwardToPaperChannel(event)) {
                paperChannelDryRunProducer.push(buildOutputMessage(event, messageAttributes, true));
            }
        });
    }

    /**
     * Gestisce l'evento in base alla modalità operativa (DRY o RUN) recuperata dal tracking.
     * <p>
     * Modalità DRY: l'evento viene inoltrato a pn-paper-tracker e a pn-paper-channel solo se non duplicato.
     * Modalità RUN: l'evento viene inviato solo a pn-paper-tracker.
     * Null: spedizioni inizializzate prima della GA26Q1.A, stesse regole di DRY per l'inoltro a pn-paper-channel.
     * </p>
     *
     * @param tracking oggetto della spedizione
     * @param event l'evento da processare
     * @param messageAttributes attributi del messaggio
     * @return Mono<Void>
     */
    private Mono<Void> handleByProcessingMode(
            PaperTrackings tracking,
            SingleStatusUpdate event,
            Map<String, MessageAttributeValue> messageAttributes
    ) {
        return switch (tracking.getProcessingMode()) {
            case DRY -> Mono.fromRunnable(() -> forwardInDryRun(tracking, event, messageAttributes));
            case RUN -> Mono.fromRunnable(() ->
                    paperTrackerProducer.push(buildOutputMessage(event, messageAttributes, false))
            );
            case null -> Mono.fromRunnable(() -> {
                log.info("Tracking entity created without processignMode, createdAt: {}", tracking.getCreatedAt());
                forwardInDryRun(tracking, event, messageAttributes);
            });
        };
    }

    /**
     * Inoltra l'evento in modalità dry-run a pn-paper-tracker e, solo se non è un duplicato
     * (isDuplicate == false o isDuplicate == null), anche a pn-paper-channel.
     *
     * @param tracking oggetto della spedizione
     * @param event l'evento da inoltrare
     * @param messageAttributes attributi del messaggio
     */
    private void forwardInDryRun(
            PaperTrackings tracking,
            SingleStatusUpdate event,
            Map<String, MessageAttributeValue> messageAttributes
    ) {
        var enrichedMessage = buildOutputMessage(event, messageAttributes, true);
        if (shouldForwardToPaperChannel(event)) {
            paperChannelDryRunProducer.push(enrichedMessage);
        } else {
            log.info("Event is duplicate, not forwarding to paper-channel, requestId: {}, statusCode: {}, clientRequestTimeStamp: {}",
                    tracking.getTrackingId(),
                    event.getAnalogMail().getStatusCode(),
                    event.getAnalogMail().getClientRequestTimeStamp());
        }
        paperTrackerProducer.push(enrichedMessage);
    }

    /**
     * Determina se l'evento deve essere inoltrato a pn-paper-channel.
     * L'evento viene inoltrato solo se non è un duplicato (isDuplicate == false o isDuplicate == null).
     *
     * @param event l'evento da verificare
     * @return true se l'evento deve essere inoltrato a pn-paper-channel
     */
    private boolean shouldForwardToPaperChannel(SingleStatusUpdate event) {
        Boolean isDuplicate = event.getAnalogMail() != null
                ? event.getAnalogMail().getIsDuplicate()
                : null;
        return !Boolean.TRUE.equals(isDuplicate);
    }

    /**
     * Costruisce il messaggio di output da inoltrare ai consumer, arricchendolo con header e flag dry-run.
     *
     * @param event il messaggio originale da pn-ec
     * @param messageAttributes attributi del messaggio
     * @param isDryRun header dryRun
     * @return evento arricchito pronto per essere inoltrato
     */
    private ExternalChannelEvent buildOutputMessage(
            SingleStatusUpdate event,
            Map<String, MessageAttributeValue> messageAttributes,
            boolean isDryRun) {

        Map<String, MessageAttributeValue> attributes =
                new HashMap<>(messageAttributes != null ? messageAttributes : Map.of());

        attributes.put(DRY_RUN_KEY, MessageAttributeValue.builder()
                .dataType("String")
                .stringValue(String.valueOf(isDryRun))
                .build());

        return ExternalChannelEvent.builder()
                .header(CustomEventHeader.builder()
                        .publisher(PUBLISHER)
                        .eventId(UUID.randomUUID().toString())
                        .createdAt(Instant.now())
                        .eventType(TRACKER_QUEUE_PROXY_EVENT_TYPE)
                        .messageAttributes(attributes)
                        .build())
                .payload(event)
                .build();
    }
}