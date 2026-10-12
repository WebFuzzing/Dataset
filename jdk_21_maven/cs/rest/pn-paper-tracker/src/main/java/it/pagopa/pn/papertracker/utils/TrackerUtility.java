package it.pagopa.pn.papertracker.utils;

import com.sngular.apigenerator.asyncapi.business_model.model.event.Data;
import it.pagopa.pn.papertracker.exception.PaperTrackerException;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.*;
import it.pagopa.pn.papertracker.model.sequence.SequenceConfig;
import it.pagopa.pn.papertracker.model.sequence.SequenceConfiguration;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.*;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class TrackerUtility {

    public static boolean checkIfIsFinalDemat(String eventStatusCode) {
        var parsedStatusCode = EventStatusCodeEnum.fromKey(eventStatusCode);
        return parsedStatusCode != null && parsedStatusCode.isFinalDemat();
    }

    public static boolean checkIfIsP000event(String eventStatusCode) {
        return P000.name().equalsIgnoreCase(eventStatusCode);
    }

    public static boolean checkIfIsInternalEvent(List<String> internalEvents, String eventStatusCode) {
        return internalEvents.contains(eventStatusCode);
    }

    public static boolean checkIfIsRecag012event(String statusCode) {
        return RECAG012.name().equalsIgnoreCase(statusCode);
    }

    public static String buildOcrRequestId(String trackingId, String eventId, String documentType) {
        return String.join("#", trackingId, eventId, documentType);
    }

    public static String[] getParsedOcrCommandId(String ocrCommandId) {
        return ocrCommandId.split("#");
    }

    public static List<Event> validatedEvents(List<String> eventsIds, List<Event> events) {
        return events.stream()
                .filter(event -> eventsIds.contains(event.getId()))
                .collect(Collectors.toMap(
                        Event::getId,
                        Function.identity(),
                        (existing, replacement) ->
                                existing.getCreatedAt().isAfter(replacement.getCreatedAt())
                                        ? existing
                                        : replacement
                ))
                .values()
                .stream()
                .toList();
    }

    public static boolean isStockStatus890(String status) {
        return RECAG005C.name().equalsIgnoreCase(status) ||
                RECAG006C.name().equalsIgnoreCase(status) ||
                RECAG007C.name().equalsIgnoreCase(status) ||
                RECAG008C.name().equalsIgnoreCase(status);
    }

    public static void setNewStatus(PaperTrackings paperTrackingsToUpdate, String statusCode, BusinessState businessState, PaperTrackingsState state) {
        if (RECAG012.name().equalsIgnoreCase(statusCode)) {
            paperTrackingsToUpdate.setState(state);
        } else if (TrackerUtility.isStockStatus890(statusCode)) {
            paperTrackingsToUpdate.setBusinessState(businessState);
        } else {
            paperTrackingsToUpdate.setState(state);
            paperTrackingsToUpdate.setBusinessState(businessState);
        }
    }

    public static void checkValidationConfig(PaperTrackings paperTrackings) {
        //CONFIGURAZIONE DI DEFAULT PER TUTTE LE SPEDIZIONI INIZIALIZZATE PRIMA DEL RILASCIO DELL 890 (PN-17784)
        if(Objects.isNull(paperTrackings.getValidationConfig())){
            ValidationConfig validationConfig = new ValidationConfig();
            validationConfig.setOcrEnabled(OcrStatusEnum.DISABLED);
            validationConfig.setSendOcrAttachmentsFinalValidation(List.of(DocumentTypeEnum.PLICO.getValue(), DocumentTypeEnum.AR.getValue()));
            paperTrackings.setValidationConfig(validationConfig);
        }
        // Conf di default per le spedizioni antecedenti a PN-18630
        if(Objects.isNull(paperTrackings.getValidationConfig().getOcrFileTypes())){
            paperTrackings.getValidationConfig().setOcrFileTypes(List.of(FileType.PDF.getValue()));
        }
    }


    public static void setDematValidationTimestamp(PaperTrackings paperTrackingsToUpdate, String statusCode) {
        ValidationFlow validationFlow = paperTrackingsToUpdate.getValidationFlow();
        if(Objects.isNull(validationFlow)){
            validationFlow = new ValidationFlow();
        }
        if (RECAG012.name().equalsIgnoreCase(statusCode)) {
            validationFlow.setRefinementDematValidationTimestamp(Instant.now());
        } else if (TrackerUtility.isStockStatus890(statusCode)) {
            validationFlow.setFinalEventDematValidationTimestamp(Instant.now());
        } else {
            validationFlow.setRefinementDematValidationTimestamp(Instant.now());
            validationFlow.setFinalEventDematValidationTimestamp(Instant.now());
        }
        paperTrackingsToUpdate.setValidationFlow(validationFlow);
    }

    public static boolean isInvalidState(HandlerContext ctx, String statusCode) {
        if (RECAG012.name().equalsIgnoreCase(statusCode) || !isStock890SequenceStatusCodes(statusCode)) {
            PaperTrackingsState state = ctx.getPaperTrackings().getState();
            return state == PaperTrackingsState.DONE || state == PaperTrackingsState.AWAITING_OCR;
        } else {
            BusinessState businessState = ctx.getPaperTrackings().getBusinessState();
            return businessState == BusinessState.DONE || businessState == BusinessState.AWAITING_OCR;
        }
    }

    public static boolean isInvalidStateForSendToOCRInRECAG012Checker(HandlerContext ctx) {
        PaperTrackingsState state = ctx.getPaperTrackings().getState();
        return state == PaperTrackingsState.DONE || state == PaperTrackingsState.AWAITING_OCR;
    }

    private static boolean isStock890SequenceStatusCodes(String statusCode) {
        SequenceConfig config005 = SequenceConfiguration.getConfig(RECAG005C.name());
        SequenceConfig config006 = SequenceConfiguration.getConfig(RECAG006C.name());
        SequenceConfig config007 = SequenceConfiguration.getConfig(RECAG007C.name());
        SequenceConfig config008 = SequenceConfiguration.getConfig(RECAG008C.name());
        return config005.sequenceStatusCodes().contains(statusCode) ||
                config006.sequenceStatusCodes().contains(statusCode) ||
                config007.sequenceStatusCodes().contains(statusCode) ||
                config008.sequenceStatusCodes().contains(statusCode);
    }

    public static boolean idRECRI004XEvent(Event event) {
        return RECRI004C.name().equals(event.getStatusCode())
                || RECRI004A.name().equals(event.getStatusCode())
                || RECRI004B.name().equals(event.getStatusCode());
    }

    public static boolean isInInvalidStateForOcr(PaperTrackings paperTrackings, String statusCode) {
        if (TrackerUtility.isStockStatus890(statusCode)) {
            BusinessState businessState = paperTrackings.getBusinessState();
            return businessState != BusinessState.AWAITING_OCR;
        } else{
            PaperTrackingsState state = paperTrackings.getState();
            return state != PaperTrackingsState.AWAITING_OCR;
        }
    }

    public static Map<String, Object> createAffectedEventsMap(boolean addDeliveryFailureCause, List<Event> events) {
        List<?> list;
        if(addDeliveryFailureCause){
            list = events.stream()
                    .map(event -> Map.of("statusCode", event.getStatusCode(),
                            "statusTimestamp", Optional.ofNullable(event.getStatusTimestamp()).map(Instant::toString).orElse(""),
                            "deliveryFailureCause", Optional.ofNullable(event.getDeliveryFailureCause()).orElse("")))
                    .toList();
        } else {
            list = events.stream()
                    .map(event -> Map.of("statusCode", event.getStatusCode(),
                            "statusTimestamp", Optional.ofNullable(event.getStatusTimestamp()).map(Instant::toString).orElse("")))
                    .toList();
        }
        return Map.of("affectedEvents", list);
    }


    public static boolean isOcrResponseCompleted(HandlerContext context, String statusCode) {
        ValidationConfig validationConfig = context.getPaperTrackings().getValidationConfig();
        ValidationFlow validationFlow = context.getPaperTrackings().getValidationFlow();
        List<String> requiredDocs;
        List<String> ocrFileTypes = validationConfig.getOcrFileTypes();
        if(RECAG012.name().equalsIgnoreCase(statusCode)){
            requiredDocs = Optional.ofNullable(validationConfig.getSendOcrAttachmentsRefinementStock890()).orElse(List.of());
            return checkAllOcrResponseRecag012(context, requiredDocs, ocrFileTypes);
        }else if(TrackerUtility.isStockStatus890(statusCode)){
            requiredDocs = validationConfig.getSendOcrAttachmentsFinalValidationStock890();
        } else {
            requiredDocs = validationConfig.getSendOcrAttachmentsFinalValidation();
        }

        return validationFlow.getOcrRequests().stream()
                .filter(ocrRequest -> !Data.ValidationStatus.KO.getValue().equalsIgnoreCase(ocrRequest.getResponseStatus()))
                .filter(ocrRequest -> requiredDocs.contains(ocrRequest.getDocumentType()))
                .allMatch(ocrRequest -> Data.ValidationStatus.OK.getValue().equalsIgnoreCase(ocrRequest.getResponseStatus()));
    }


    private static boolean checkAllOcrResponseRecag012(HandlerContext context,
                                                       List<String> requiredDocs,
                                                       List<String> ocrFileTypes) {
        if (requiredDocs.isEmpty()) {return true;}

        PaperTrackings tracking = context.getPaperTrackings();
        ValidationFlow flow = tracking.getValidationFlow();

        Set<String> eventsDocumentTypesNotSent = context.getPaperTrackings().getEvents().stream()
                .map(Event::getAttachments)
                .filter(attachments -> !CollectionUtils.isEmpty(attachments))
                .flatMap(List::stream)
                .filter(attachment ->
                        !ocrFileTypes.contains(
                                OcrUtility.retrieveFileType(attachment.getUri())
                        )
                )
                .map(Attachment::getDocumentType)
                .collect(Collectors.toSet());

        Set<String> completedDocuments = flow.getOcrRequests().stream()
                .filter(req -> requiredDocs.contains(req.getDocumentType()))
                .filter(ocrRequest -> Data.ValidationStatus.OK.getValue().equals(ocrRequest.getResponseStatus()))
                .map(OcrRequest::getDocumentType)
                .collect(Collectors.toSet());

        completedDocuments.addAll(eventsDocumentTypesNotSent);

        return TrackerUtility.hasRequiredAttachmentsRefinementStock890(requiredDocs, completedDocuments);
    }

    public static Event extractEventFromContext(HandlerContext context) {
        return context.getPaperTrackings().getEvents().stream()
                .filter(event -> context.getEventId().equalsIgnoreCase(event.getId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("The event with id " + context.getEventId() + " does not exist in the paperTrackings events list."));
    }

    public static String getStatusCodeFromEventId(PaperTrackings paperTrackings, String eventId) {
        Event event = getEventFromEventId(paperTrackings, eventId);
        return event != null ? event.getStatusCode() : null;
    }

    public static Event getEventFromEventId(PaperTrackings paperTrackings, String eventId) {
        if(!CollectionUtils.isEmpty(paperTrackings.getEvents())) {
            return paperTrackings.getEvents().stream()
                    .filter(event -> event.getId().equalsIgnoreCase(eventId))
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    public static Event extractFinalEventFromOcr(String commandId, PaperTrackings paperTrackings) {
        String eventId = TrackerUtility.getParsedOcrCommandId(commandId)[1];
        return paperTrackings.getEvents().stream()
                .filter(event -> eventId.equalsIgnoreCase(event.getId()))
                .findFirst()
                .orElseThrow(() -> new PaperTrackerException("Invalid eventId in ocrCommandId: " + eventId +
                        ". The event with id " + eventId + " does not exist in the paperTrackings events list."));
    }

    public static Integer getOcrRequestIndexByEventIdAndDocType(PaperTrackings tracking, String eventId, String docType) {
        List<OcrRequest> ocrRequests = tracking.getValidationFlow().getOcrRequests();
        if(CollectionUtils.isEmpty(ocrRequests)){
            return null;
        }
        return IntStream.iterate(ocrRequests.size() - 1, i -> i >= 0, i -> i - 1)
                .filter(i -> ocrRequests.get(i).getFinalEventId().equalsIgnoreCase(eventId) && ocrRequests.get(i).getDocumentType().equalsIgnoreCase(docType))
                .findFirst()
                .orElse(-1);
    }

    public static Attachment getAttachmentFromEventIdAndDocType(PaperTrackings tracking, String eventId, String docType) {
        List<OcrRequest> ocrRequests = tracking.getValidationFlow().getOcrRequests();
        if(CollectionUtils.isEmpty(ocrRequests)){
            return null;
        }
        return tracking.getValidationFlow().getOcrRequests().stream()
                .filter(req -> req.getFinalEventId().equalsIgnoreCase(eventId) && req.getDocumentType().equalsIgnoreCase(docType))
                .findFirst()
                .map(ocrRequest -> {
                    Attachment attachment = new Attachment();
                    attachment.setDocumentType(docType);
                    attachment.setUri(ocrRequest.getUri());
                    return attachment;
                })
                .orElse(null);
    }

    public static Optional<Event> findRECAG012Event(PaperTrackings paperTrackings) {
        return paperTrackings.getEvents().stream()
                .filter(event -> RECAG012.name().equalsIgnoreCase(event.getStatusCode()))
                .findFirst();
    }
    public static EventStatus evaluateStatusCodeAndRetrieveStatus(String statusCodeToEvaluate, String statusCode, PaperTrackings paperTrackings) {
        String deliveryFailureCause = paperTrackings.getPaperStatus().getDeliveryFailureCause();
        if (statusCodeToEvaluate.equalsIgnoreCase(statusCode)) {
            if (StringUtils.equals("M02", deliveryFailureCause) || StringUtils.equals("M05", deliveryFailureCause)) {
                return EventStatus.OK;
            }
            if (StringUtils.equals("M06", deliveryFailureCause) || StringUtils.equals("M07", deliveryFailureCause) ||
                    StringUtils.equals("M08", deliveryFailureCause) || StringUtils.equals("M09", deliveryFailureCause)) {
                return EventStatus.KO;
            }
        }
        return EventStatusCodeEnum.fromKey(statusCode).getStatus();
    }


    public static EventStatus evaluateStatusCodeAndRetrieveStatus(String statusCodeToEvaluate, String deliveryFailureCause, String productType) {

        EventStatusCodeEnum statusCodeEnum = EventStatusCodeEnum.fromKey(statusCodeToEvaluate);
        if (Objects.isNull(statusCodeEnum)) {
            return null;
        }

        if (!statusCodeEnum.getProductType().getValue().equalsIgnoreCase(productType) && statusCodeEnum.getProductType() != ProductType.RIR) {
            return null;
        }

        if (StringUtils.isNotBlank(deliveryFailureCause) && (statusCodeEnum.equals(EventStatusCodeEnum.RECAG003C) || statusCodeEnum.equals(EventStatusCodeEnum.RECRN002C))) {
            return switch (deliveryFailureCause) {
                case "M02", "M05" -> EventStatus.OK;
                case "M06", "M07", "M08", "M09" -> EventStatus.KO;
                default -> statusCodeEnum.getStatus();
            };
        }

        if(isStockStatus890(statusCodeToEvaluate)){
            return EventStatus.OK;
        }

        return statusCodeEnum.getStatus();
    }

    /**
     * Verifica se i tipi di documento forniti contengono tutti gli allegati richiesti
     * nel caso del perfezionamento della giacenza 890".
     * Se nella lista degli allegati obbligatori sono presenti sia "ARCAD" che "CAD",
     * verifica se almeno uno tra ARCAD e CAD è presente nei tipi di documento forniti
     *
     * @param requiredAttachments Una lista di stringhe che rappresentano gli allegati obbligatori richiesti.
     * @param documentTypes Un insieme di stringhe che rappresentano i tipi di documento disponibili.
     * @return {@code true} se i tipi di documento forniti soddisfano i requisiti degli allegati richiesti,
     *         {@code false} altrimenti.
     */
    public static boolean hasRequiredAttachmentsRefinementStock890(
            List<String> requiredAttachments,
            Set<String> documentTypes) {

        String arcad = DocumentTypeEnum.ARCAD.getValue();
        String cad = DocumentTypeEnum.CAD.getValue();

        boolean hasArcad = requiredAttachments.contains(arcad);
        boolean hasCad = requiredAttachments.contains(cad);

        // Caso ARCAD o CAD
        if (hasArcad && hasCad) {

            // Rimuove ARCAD e CAD dalla lista degli allegati obbligatori
            List<String> remaining = requiredAttachments.stream()
                    .filter(r -> !r.equals(arcad) && !r.equals(cad))
                    .toList();

            boolean hasRemaining = documentTypes.containsAll(remaining);
            boolean hasAtLeastOne = documentTypes.contains(arcad) || documentTypes.contains(cad);

            return hasRemaining && hasAtLeastOne;
        }

        // Caso standard
        return documentTypes.containsAll(requiredAttachments);
    }

    public static boolean checkIfIsRedrive(String senderId, List<String> redriveEnabledDomains) {
        if(StringUtils.isBlank(senderId) || CollectionUtils.isEmpty(redriveEnabledDomains)) {
            return false;
        }
        return redriveEnabledDomains.stream().anyMatch(senderId::endsWith);
    }

}
