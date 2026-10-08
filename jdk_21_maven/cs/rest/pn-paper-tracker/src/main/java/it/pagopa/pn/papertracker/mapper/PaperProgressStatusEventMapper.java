package it.pagopa.pn.papertracker.mapper;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.AttachmentDetails;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.NONE)
public class PaperProgressStatusEventMapper {

    /**
     * Rimappa l'oggetto PaperProgressStatusEvent nell'entity PaperTrackings in modo da effettuare l'upsert
     *
     * @param paperProgressStatusEvent evento da rimappare
     * @param anonymizedDiscoveredAddressId l'id dell'indirizzo anonimizzato
     * @return PaperTrackings contenente il nuovo evento
     */
    public static PaperTrackings toPaperTrackings(String reworkId, PaperProgressStatusEvent paperProgressStatusEvent,
                                                        String anonymizedDiscoveredAddressId,
                                                        String eventId, boolean dryRunEnabled,
                                                        boolean isFinalDemat, boolean isP000event,
                                                        boolean isInternalEvent, boolean isRecag012event) {
        Instant now = Instant.now();
        PaperTrackings paperTrackings = new PaperTrackings();
        Event event = new Event();
        event.setId(eventId);
        event.setIun(paperProgressStatusEvent.getIun());
        if (!CollectionUtils.isEmpty(paperProgressStatusEvent.getAttachments())) {
            event.setAttachments(paperProgressStatusEvent.getAttachments().stream()
                    .map(PaperProgressStatusEventMapper::buildAttachmentFromAttachmentDetail)
                    .toList());
        }else{
            event.setAttachments(new ArrayList<>());
        }

        if(isFinalDemat) {
            PaperStatus paperStatus = new PaperStatus();
            paperStatus.setFinalDematFound(true);
            paperTrackings.setPaperStatus(paperStatus);
        }

        if (isP000event) {
            PaperStatus paperStatus = new PaperStatus();
            paperStatus.setPaperDeliveryTimestamp(paperProgressStatusEvent.getStatusDateTime().toInstant());
            paperTrackings.setPaperStatus(paperStatus);
        }

        if (!isInternalEvent){
            event.setRequestTimestamp(paperProgressStatusEvent.getClientRequestTimeStamp().toInstant());
        }

        if(isRecag012event){
            ValidationFlow validationFlow = new ValidationFlow();
            validationFlow.setRecag012StatusTimestamp(now);
            paperTrackings.setValidationFlow(validationFlow);
        }

        event.setStatusCode(paperProgressStatusEvent.getStatusCode());
        event.setStatusTimestamp(paperProgressStatusEvent.getStatusDateTime().toInstant());

        event.setDeliveryFailureCause(paperProgressStatusEvent.getDeliveryFailureCause());
        event.setRegisteredLetterCode(paperProgressStatusEvent.getRegisteredLetterCode());
        event.setNotificationReworkId(reworkId);
        if(StringUtils.hasText(paperProgressStatusEvent.getProductType())) {
            event.setProductType(paperProgressStatusEvent.getProductType());
        }
        event.setAnonymizedDiscoveredAddressId(anonymizedDiscoveredAddressId);
        event.setDryRun(dryRunEnabled);
        event.setStatusDescription(paperProgressStatusEvent.getStatusDescription());
        event.setCreatedAt(now);
        event.setPrinter(paperProgressStatusEvent.getPrinter());
        event.setDu(paperProgressStatusEvent.getDu());

        paperTrackings.setEvents(List.of(event));
        return paperTrackings;
    }

    public static Attachment buildAttachmentFromAttachmentDetail(AttachmentDetails attachmentDetails) {
        Attachment attachment = new Attachment();
        attachment.setDate(attachmentDetails.getDate().toInstant());
        attachment.setId(attachmentDetails.getId());
        attachment.setDocumentType(attachmentDetails.getDocumentType());
        attachment.setUri(attachmentDetails.getUri());
        attachment.setSha256(attachmentDetails.getSha256());
        attachment.setSourceType(attachmentDetails.getSourceType());
        attachment.setOriginType(attachmentDetails.getOriginType());
        return attachment;
    }

}
