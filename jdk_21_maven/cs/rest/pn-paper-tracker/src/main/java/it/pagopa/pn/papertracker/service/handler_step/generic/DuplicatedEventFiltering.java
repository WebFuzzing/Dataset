package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.AttachmentDetails;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.DiscoveredAddress;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.pndatavault.model.PaperAddress;
import it.pagopa.pn.papertracker.mapper.PaperTrackingsErrorsMapper;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.time.temporal.ChronoUnit.SECONDS;

@Component
@RequiredArgsConstructor
@Slf4j
public class DuplicatedEventFiltering implements HandlerStep {

    private final DataVaultClient dataVaultClient;

    /**
     * Step di validazione per filtrare eventi duplicati.
     * Verifica se l'evento in arrivo è già presente nella lista degli eventi.
     * Escludo dal controllo duplicati eventi con lo stesso ID (es. ultimo evento ricevuto precedentemente salvato
     * e retry dello stesso messaggio).
     * In caso di evento duplicato, genera un errore di validazione. Altrimenti, permette il proseguimento del flusso.
     *
     * @param context Contesto contenente le informazioni necessarie per l'elaborazione dell'evento.
     * @return Mono(Void)
     */
    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Starting DuplicatedEventFiltering for trackingId: {}", context.getTrackingId());

        PaperTrackings paperTrackings = context.getPaperTrackings();
        PaperProgressStatusEvent paperProgressStatusEvent = context.getPaperProgressStatusEvent();

        if (context.isRedrive()) {
            log.info("Skipping DuplicatedEventFiltering isRedrive: true");
            return Mono.empty();
        }

        return Flux.fromIterable(paperTrackings.getEvents())
                .filter(event -> !event.getId().equalsIgnoreCase(context.getEventId()))
                .flatMap(event -> isDuplicatedEvent(event, paperProgressStatusEvent, paperTrackings.getNotificationReworkId()))
                .any(Boolean::booleanValue)
                .flatMap(isDuplicate -> isDuplicate
                        ? Mono.error(createValidationException(paperTrackings, paperProgressStatusEvent, context))
                        : Mono.empty());
    }

    private PnPaperTrackerValidationException createValidationException(PaperTrackings paperTrackings,
                                                                        PaperProgressStatusEvent event,
                                                                        HandlerContext context) {
        String statusCode = event.getStatusCode();
        Map<String, Object> additionalDetails = Map.of(
                "statusCode", statusCode,
                "statusTimestamp", Objects.toString(event.getStatusDateTime(), null)
        );
        return new PnPaperTrackerValidationException(
                "Duplicated event found: " + statusCode,
                PaperTrackingsErrorsMapper.buildPaperTrackingsError(
                        paperTrackings,
                        statusCode,
                        ErrorCategory.DUPLICATED_EVENT,
                        null,
                        "Duplicated event found for statusCode: " + statusCode,
                        additionalDetails,
                        FlowThrow.DUPLICATED_EVENT_VALIDATION,
                        ErrorType.WARNING,
                        context.getEventId()
                )
        );
    }

    public Mono<Boolean> isDuplicatedEvent(Event event, PaperProgressStatusEvent paperProgressStatusEvent, String reworkId)  {
        boolean fieldsMatch = Objects.equals(paperProgressStatusEvent.getRegisteredLetterCode(),event.getRegisteredLetterCode())
                && Objects.equals(paperProgressStatusEvent.getProductType(), event.getProductType())
                && Objects.equals(paperProgressStatusEvent.getIun(), event.getIun())
                && Objects.equals(paperProgressStatusEvent.getStatusCode(), event.getStatusCode())
                && Objects.equals(paperProgressStatusEvent.getStatusDescription(), event.getStatusDescription())
                && paperProgressStatusEvent.getStatusDateTime().truncatedTo(SECONDS).isEqual(event.getStatusTimestamp().truncatedTo(SECONDS).atOffset(ZoneOffset.UTC))
                && Objects.equals(paperProgressStatusEvent.getDeliveryFailureCause(), event.getDeliveryFailureCause())
                && Objects.equals(reworkId, event.getNotificationReworkId())
                && isSameAttachments(paperProgressStatusEvent.getAttachments(), event.getAttachments());

        if(fieldsMatch){
            return deanonymizeAddressAndCheckIt(event.getAnonymizedDiscoveredAddressId(), paperProgressStatusEvent);
        }else {
            return Mono.just(false);
        }

    }

    private Mono<Boolean> deanonymizeAddressAndCheckIt(String anonymizedDiscoveredAddressId, PaperProgressStatusEvent paperProgressStatusEvent) {
        if (StringUtils.hasText(anonymizedDiscoveredAddressId) && Objects.nonNull(paperProgressStatusEvent.getDiscoveredAddress()))
            return dataVaultClient.deAnonymizeDiscoveredAddress(paperProgressStatusEvent.getRequestId(), anonymizedDiscoveredAddressId)
                    .map(paperAddress -> isSameAddress(paperAddress, paperProgressStatusEvent.getDiscoveredAddress()));
        return Mono.just(!StringUtils.hasText(anonymizedDiscoveredAddressId) && Objects.isNull(paperProgressStatusEvent.getDiscoveredAddress()));
    }

    private static boolean isSameAttachments(List<AttachmentDetails> attachmentsDetails, List<Attachment> attachments) {

        if(attachments == null) {
            attachments = List.of();
        }

        if(attachmentsDetails == null) {
            attachmentsDetails = List.of();
        }

        if(attachmentsDetails.isEmpty() && attachments.isEmpty()) {
            return true;
        }

        if (attachments.isEmpty() || attachmentsDetails.isEmpty()) {
            return false;
        }

        if (attachments.size() != attachmentsDetails.size()){
            return false;
        } else {

            for (int i = 0; i<attachments.size(); i++) {
                Attachment paperProgress = attachments.get(i);
                AttachmentDetails eventAttachments = attachmentsDetails.get(i);
                if(! (paperProgress.getDate().truncatedTo(SECONDS).atOffset(ZoneOffset.UTC).isEqual(eventAttachments.getDate().truncatedTo(SECONDS)) &&
                        Objects.equals(paperProgress.getId(), eventAttachments.getId()) &&
                        Objects.equals(paperProgress.getDocumentType(), eventAttachments.getDocumentType()) &&
                        Objects.equals(paperProgress.getSha256(), eventAttachments.getSha256()))) {
                    return false;
                }
            } return true;
        }
    }

    private static boolean isSameAddress (PaperAddress paperAddress, DiscoveredAddress discoveredAddress) {
        if (paperAddress == null && discoveredAddress == null) {
            return true;
        }
        if (paperAddress == null || discoveredAddress==null) {
            return false;
        }
        return  Objects.equals(paperAddress.getName(), discoveredAddress.getName())
                && Objects.equals(paperAddress.getNameRow2(), discoveredAddress.getNameRow2())
                && Objects.equals(paperAddress.getAddress(), discoveredAddress.getAddress())
                && Objects.equals(paperAddress.getAddressRow2(), discoveredAddress.getAddressRow2())
                && Objects.equals(paperAddress.getCap(), discoveredAddress.getCap())
                && Objects.equals(paperAddress.getCity(), discoveredAddress.getCity())
                && Objects.equals(paperAddress.getCity2(), discoveredAddress.getCity2())
                && Objects.equals(paperAddress.getPr(), discoveredAddress.getPr())
                && Objects.equals(paperAddress.getCountry(), discoveredAddress.getCountry());
    }
}
