package it.pagopa.pn.papertracker.utils;

import com.sngular.apigenerator.asyncapi.business_model.model.event.DataDTO;
import com.sngular.apigenerator.asyncapi.business_model.model.event.DetailsDTO;
import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataPayloadDTO;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.papertracker.middleware.queue.model.OcrEvent;
import it.pagopa.pn.papertracker.middleware.queue.producer.OcrMomProducer;
import it.pagopa.pn.papertracker.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECAG010A;
import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECRN010;
import static it.pagopa.pn.papertracker.utils.QueueConst.OCR_REQUEST_EVENT_TYPE;
import static it.pagopa.pn.papertracker.utils.QueueConst.PUBLISHER;

@Component
@RequiredArgsConstructor
@Slf4j
public class OcrUtility {

    private final OcrMomProducer ocrMomProducer;
    private final SafeStorageClient safeStorageClient;
    private final PnPaperTrackerConfigs cfg;
    private final PaperTrackingsDAO paperTrackingsDAO;

    public Mono<Boolean> checkAndSendToOcr(Event event, Map<String, List<Attachment>> attachmentList, HandlerContext context) {
        PaperTrackings paperTracking = context.getPaperTrackings();
        OcrStatusEnum ocrStatusEnum = context.getPaperTrackings().getValidationConfig().getOcrEnabled();

        if (Objects.nonNull(ocrStatusEnum) && !ocrStatusEnum.equals(OcrStatusEnum.DISABLED)) {
            log.info("OCR validation enabled for trackingId={}", paperTracking.getTrackingId());
            return sendMessageToOcr(event, attachmentList, paperTracking, ocrStatusEnum)
                    .flatMap(sentToOcr -> OcrStatusEnum.DRY.equals(ocrStatusEnum) ? Mono.just(false) : Mono.just(sentToOcr));
        } else {
            log.info("OCR validation disabled for trackingId={}", paperTracking.getTrackingId());
            return paperTrackingsDAO.updateItem(paperTracking.getTrackingId(), getPaperTrackingsToUpdate(OcrStatusEnum.DISABLED, false, event, null))
                    .doOnNext(v -> log.debug("Updated PaperTrackings entity with OCR flag disabled for trackingId={}", paperTracking.getTrackingId()))
                    .thenReturn(false);
        }
    }

    /**
     * Invia i documenti validi al servizio OCR per la validazione allegati.
     * <p>
     * Il metodo filtra preliminarmente gli allegati verificando che il loro
     * tipo sia tra quelli abilitati alla validazione OCR da configurazione.
     *
     * @param event             evento corrente del flusso
     * @param attachmentList    mappa degli allegati associati all'evento
     * @param paperTracking     entità di tracking della spedizione
     * @param ocrStatusEnum     stato OCR da impostare sul tracking
     * @return {@link Mono} che emette:
     *         <ul>
     *             <li>{@code true} se la richiesta OCR è stata effettivamente inviata</li>
     *             <li>{@code false} se non sono presenti allegati validi e l'OCR è stato saltato</li>
     *         </ul>
     */
    private Mono<Boolean> sendMessageToOcr(Event event,
                                           Map<String, List<Attachment>> attachmentList,
                                           PaperTrackings paperTracking,
                                           OcrStatusEnum ocrStatusEnum) {
        Instant now = Instant.now();
        List<String> ocrFileTypes = paperTracking.getValidationConfig().getOcrFileTypes();
        Map<String, List<Attachment>> validAttachmentList = attachmentList.entrySet().stream()
                .map(entry -> Map.entry(
                        entry.getKey(),
                        entry.getValue().stream()
                                .filter(attachment ->
                                        ocrFileTypes.contains(retrieveFileType(attachment.getUri()))
                                                && getSourceType(attachment).equals(SourceType.SCANNED.name()) && getOriginType(attachment).equals(OriginType.ORIGINAL.name()))
                                .toList()))
                .filter(entry -> !entry.getValue().isEmpty())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));


        // Nessun allegato valido -> OCR non invocato
        if (validAttachmentList.isEmpty()) {
            log.info("No attachment type supported found for OCR, skipping OCR request for trackingId={}", paperTracking.getTrackingId());
            return paperTrackingsDAO.updateItem(paperTracking.getTrackingId(), getPaperTrackingsToUpdate(ocrStatusEnum,false, event, null))
                    .thenReturn(false);
        }

        // OCR invocato
        List<OcrRequest> ocrRequests = new ArrayList<>();
        return Flux.fromIterable(validAttachmentList.entrySet())
                .flatMap(attachmentEntry ->  processAttachments(attachmentEntry, paperTracking, event, ocrRequests, now))
                .collectList()
                .flatMap(unused -> paperTrackingsDAO.updateItem(paperTracking.getTrackingId(),
                        getPaperTrackingsToUpdate(ocrStatusEnum, true, event, ocrRequests)))
                .doOnNext(unused -> log.info("OCR validation completed for trackingId={}", paperTracking.getTrackingId()))
                .thenReturn(true);
    }

    private boolean isSourceTypeAndOriginTypeNotNull(Attachment attachment) {
        return StringUtils.isNotBlank(attachment.getSourceType()) && StringUtils.isNotBlank(attachment.getOriginType());
    }

    private String getSourceType(Attachment attachment) {
        if (isSourceTypeAndOriginTypeNotNull(attachment)) {
            return attachment.getSourceType();
        }
        return FileType.PDF.getValue().equalsIgnoreCase(retrieveFileType(attachment.getUri())) ? SourceType.SCANNED.name() : SourceType.DIGITAL.name();
    }

    private String getOriginType(Attachment attachment) {
        return isSourceTypeAndOriginTypeNotNull(attachment) ? attachment.getOriginType() : OriginType.ORIGINAL.name();
    }

    private Flux<String> processAttachments(Map.Entry<String, List<Attachment>> attachmentEntry,
                                            PaperTrackings paperTracking,
                                            Event event,
                                            List<OcrRequest> ocrRequests,
                                            Instant now) {
        return Flux.fromIterable(attachmentEntry.getValue())
                .flatMap(attachment -> processAttachmentForOcr(paperTracking, attachment, attachmentEntry.getKey(), event, ocrRequests, now));
    }

    private Mono<String> processAttachmentForOcr(PaperTrackings paperTracking,
                                                 Attachment attachment,
                                                 String attachmentEventId,
                                                 Event event,
                                                 List<OcrRequest> ocrRequests,
                                                 Instant now) {
        String documentType = attachment.getDocumentType();
        String ocrRequestId = TrackerUtility.buildOcrRequestId(paperTracking.getTrackingId(), event.getId(), documentType);
        ocrRequests.add(getOcrRequest(documentType, event.getId(), attachmentEventId, attachment.getUri(), now));

        return safeStorageClient.getSafeStoragePresignedUrl(attachment.getUri())
                .doOnNext(presignedUrl -> {
                    OcrEvent ocrEvent = buildOcrEvent(paperTracking, ocrRequestId, presignedUrl, DocumentTypeEnum.fromValue(documentType), event);
                    ocrMomProducer.push(ocrEvent);
                });
    }

    private OcrRequest getOcrRequest(String documentType, String finalEventId, String attachmentEventId, String uri, Instant now) {
        OcrRequest ocrRequest = new OcrRequest();
        ocrRequest.setDocumentType(documentType);
        ocrRequest.setFinalEventId(finalEventId);
        ocrRequest.setAttachmentEventId(attachmentEventId);
        ocrRequest.setRequestTimestamp(now);
        ocrRequest.setUri(uri);
        return ocrRequest;
    }

    static public String retrieveFileType(String uri) {
        return FilenameUtils.getExtension(uri).toLowerCase(Locale.ROOT);
    }

    private PaperTrackings getPaperTrackingsToUpdate(OcrStatusEnum ocrStatusEnum,
                                                     boolean isSentToOcr,
                                                     Event event,
                                                     List<OcrRequest> ocrRequests) {
        PaperTrackings paperTracking = new PaperTrackings();
        paperTracking.setValidationConfig(new ValidationConfig());
        paperTracking.setValidationFlow(new ValidationFlow());
        paperTracking.getValidationConfig().setOcrEnabled(ocrStatusEnum);
        paperTracking.getValidationFlow().setOcrRequests(ocrRequests != null ? ocrRequests : List.of());

        if(isSentToOcr && ocrStatusEnum.equals(OcrStatusEnum.RUN)) {
            TrackerUtility.setNewStatus(paperTracking, event.getStatusCode(), BusinessState.AWAITING_OCR, PaperTrackingsState.AWAITING_OCR);
        } else {
            TrackerUtility.setDematValidationTimestamp(paperTracking, event.getStatusCode());
        }

        return paperTracking;
    }

    private OcrEvent buildOcrEvent(PaperTrackings paperTracking, String ocrRequestId, String presignedUrl, DocumentTypeEnum documentType, Event event) {

        GenericEventHeader ocrHeader = GenericEventHeader.builder()
                .publisher(PUBLISHER)
                .eventId(UUID.randomUUID().toString())
                .createdAt(Instant.now())
                .eventType(OCR_REQUEST_EVENT_TYPE)
                .build();

        DetailsDTO.DeliveryFailureCause deliveryFailureCause = StringUtils.isNotBlank(paperTracking.getPaperStatus().getDeliveryFailureCause()) ?
                DetailsDTO.DeliveryFailureCause.valueOf(paperTracking.getPaperStatus().getDeliveryFailureCause()) : null;

        DataDTO.UnifiedDeliveryDriver unifiedDeliveryDriver = StringUtils.isNotBlank(paperTracking.getUnifiedDeliveryDriver()) ?
                DataDTO.UnifiedDeliveryDriver.valueOf(paperTracking.getUnifiedDeliveryDriver().toUpperCase()) : null;

        LocalDateTime deliveryAttemptDate = Optional.ofNullable(getDeliveryAttemptDate(paperTracking))
                .map(instant -> LocalDateTime.ofInstant(instant, ZoneOffset.UTC))
                .orElse(null);

        OcrDataPayloadDTO ocrDataPayload = OcrDataPayloadDTO.builder()
                .version("v1")
                .commandType(OcrDataPayloadDTO.CommandType.POSTAL)
                .commandId(ocrRequestId)
                .data(DataDTO.builder()
                        .documentType(DataDTO.DocumentType.valueOf(documentType.name()))
                        .productType(getProductType(paperTracking))
                        .unifiedDeliveryDriver(unifiedDeliveryDriver)
                        .details(
                                DetailsDTO.builder()
                                        .attachment(presignedUrl)
                                        .registeredLetterCode(paperTracking.getPaperStatus().getRegisteredLetterCode())
                                        .notificationDate(LocalDateTime.ofInstant(event.getStatusTimestamp(), ZoneOffset.UTC))
                                        .deliveryFailureCause(deliveryFailureCause)
                                        .deliveryDetailCode(event.getStatusCode())
                                        .deliveryAttemptDate(deliveryAttemptDate)
                                        .build()
                        )
                        .build())
                .build();

        return new OcrEvent(ocrHeader, ocrDataPayload);
    }

    private DataDTO.ProductType getProductType(PaperTrackings paperTracking) {
        return Arrays.stream(DataDTO.ProductType.values()).filter(ocrProductType -> ocrProductType.getValue().equals(paperTracking.getProductType()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("invalid ProductType for trackingId={}: {}", paperTracking.getTrackingId(), paperTracking.getProductType());
                    return new IllegalArgumentException("Invalid product type: " + paperTracking.getProductType());
                });
    }

    /**
     * Per il prodotto AR, recupera lo statusTimestamp dell'evento RECRN010 se presente, altrimenti ritorna null.
     * Per il prodotto 890, recupera lo statusTimestamp dell'evento RECAG010A se presente, altrimenti ritorna null.
     *
     * @param paperTrackings L'oggetto `PaperTrackings` contenente gli eventi associati al tracking.
     * @return Lo statusTimestamp del primo evento trovato, oppure null.
     */
    private Instant getDeliveryAttemptDate(PaperTrackings paperTrackings) {
        return paperTrackings.getEvents().stream()
                .filter(event -> RECRN010.name().equals(event.getStatusCode())
                        || RECAG010A.name().equals(event.getStatusCode()))
                .sorted(Comparator.comparing(Event::getRequestTimestamp).reversed())
                .map(Event::getStatusTimestamp)
                .findFirst()
                .orElse(null);
    }

}
