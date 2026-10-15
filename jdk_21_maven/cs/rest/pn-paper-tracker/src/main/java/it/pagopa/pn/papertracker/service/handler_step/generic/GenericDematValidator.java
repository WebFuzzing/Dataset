package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PaperTrackerException;
import it.pagopa.pn.papertracker.mapper.PaperTrackingsErrorsMapper;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.FileType;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.model.SourceType;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.OcrUtility;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pn.papertracker.utils.OcrUtility.retrieveFileType;

@RequiredArgsConstructor
public abstract class GenericDematValidator implements HandlerStep {

    private static final Logger log = LoggerFactory.getLogger(GenericDematValidator.class);

    private final OcrUtility ocrUtility;
    private final PaperTrackerErrorService paperTrackerErrorService;

    /**
     * Step che gestisce la validazione dematerializzazione. Se la validazione OCR è abilitata, invia un messaggio al servizio OCR e aggiorna lo stato del tracciamento.
     * Se la validazione OCR non è abilitata, aggiorna direttamente lo stato del tracciamento.
     *
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono(Void)
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        return validateDemat(context)
                .then();
    }

    public Mono<Void> validateDemat(HandlerContext context) {
        PaperTrackings paperTrackings = context.getPaperTrackings();
        String trackingId = paperTrackings.getTrackingId();
        log.info("Starting demat validation for trackingId={}", trackingId);

        Event currentEvent = TrackerUtility.extractEventFromContext(context);
        List<String> requiredAttachments = getRequiredAttachments(currentEvent, paperTrackings);
        List<Event> validatedEvent = TrackerUtility.validatedEvents(paperTrackings.getPaperStatus().getValidatedEvents(), paperTrackings.getEvents());
        Map<String, List<Attachment>> attachmentList = retrieveFinalDemat(validatedEvent, requiredAttachments);
        return checkSourceTypeFileTypeCoherent(paperTrackings, validatedEvent)
                .then(Mono.defer(() -> ocrUtility.checkAndSendToOcr(currentEvent, attachmentList, context)
                        .onErrorResume(e -> Mono.error(new PaperTrackerException("Error during Demat Validation", e)))
                        .filter(isSentToOcr -> Boolean.TRUE.equals(isSentToOcr))
                        // Ferma l'esecuzione degli step se è stato inviato all'OCR
                        .doOnNext(unused -> context.setStopExecution(true))
                        .then()));
    }

    private Map<String, List<Attachment>> retrieveFinalDemat(List<Event> validatedEvents, List<String> requiredAttachments) {
        return validatedEvents.stream()
                .filter(event -> !CollectionUtils.isEmpty(event.getAttachments()))
                .map(event -> Map.entry(
                        event.getId(),
                        event.getAttachments().stream()
                                .filter(att -> requiredAttachments.contains(att.getDocumentType()))
                                .toList()
                ))
                .filter(entry -> !CollectionUtils.isEmpty(entry.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<String> getRequiredAttachments(Event event, PaperTrackings paperTrackings) {
        if (TrackerUtility.isStockStatus890(event.getStatusCode()))
            return paperTrackings.getValidationConfig().getSendOcrAttachmentsFinalValidationStock890();

        return paperTrackings.getValidationConfig().getSendOcrAttachmentsFinalValidation();
    }

    private Mono<Void> checkSourceTypeFileTypeCoherent(PaperTrackings paperTracking, List<Event> validatedEvent) {
        return Flux.fromIterable(validatedEvent)
                .filter(event -> !CollectionUtils.isEmpty(event.getAttachments()))
                .flatMap(event -> Flux.fromIterable(event.getAttachments())
                        .flatMap(attachment -> {
                            String fileType = retrieveFileType(attachment.getUri());
                            return isSourceTypeFileTypeCoherent(attachment.getSourceType(), fileType)
                                    ? Mono.empty()
                                    : insertSourceTypeFileTypeWarning(paperTracking, event, attachment, fileType);
                        }))
                .then();
    }

    private boolean isSourceTypeFileTypeCoherent(String sourceType, String fileType) {
        if (StringUtils.isBlank(sourceType) || StringUtils.isBlank(fileType)) {
            return true;
        }

        if (!SourceType.SCANNED.name().equalsIgnoreCase(sourceType)) {
            return true;
        }

        return FileType.PDF.getValue().equalsIgnoreCase(fileType);

    }

    private Mono<Void> insertSourceTypeFileTypeWarning(PaperTrackings paperTracking,
                                                       Event event,
                                                       Attachment attachment,
                                                       String fileType) {
        String sourceType = attachment.getSourceType();
        String message = String.format(
                "SourceType/fileType incoherent for OCR: sourceType=%s, fileType=%s, uri=%s",
                sourceType, fileType, attachment.getUri()
        );

        PaperTrackingsErrors warning = PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                paperTracking,
                event.getStatusCode(),
                ErrorCategory.ATTACHMENTS_ERROR,
                ErrorCause.SOURCETYPE_FILETYPE_INCOHERENT,
                message,
                Map.of(
                        "sourceType", sourceType,
                        "fileType", fileType,
                        "uri", attachment.getUri()
                ),
                FlowThrow.DEMAT_VALIDATION,
                ErrorType.WARNING,
                event.getId()
        );

        return paperTrackerErrorService.insertPaperTrackingsError(warning)
                .doOnError(ex -> log.warn("Unable to persist warning for trackingId={}, eventId={}",
                        paperTracking.getTrackingId(), event.getId(), ex))
                .onErrorResume(ex -> Mono.empty())
                .then();
    }

}