package it.pagopa.pn.papertracker.mapper;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.DiscoveredAddress;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.AnalogAddress;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.AttachmentDetails;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.SendEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.papertracker.generated.openapi.msclient.pndatavault.model.PaperAddress;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Attachment;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Event;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor(access = AccessLevel.NONE)
@Slf4j
public class SendEventMapper {

    /**
     * Crea l'evento dalla classe PaperProgressStatusEvent e lo inserisce dentro SendEvent per l'invio a delivery-push.
     * Se si tratta di un evento intermedio senza allegati ed è un final demat, non viene inviato a delivery-push.
     *
     * @param paperProgressStatusEvent evento intermedio ricevuto
     * @return SendEvent contenente l'evento rimappato da inviare a delivery-push
     */
    public static Flux<SendEvent> createSendEventsFromPaperProgressStatusEvent(PaperProgressStatusEvent paperProgressStatusEvent) {
        boolean isFinalDemat = TrackerUtility.checkIfIsFinalDemat(paperProgressStatusEvent.getStatusCode());
        return Mono.just(paperProgressStatusEvent)
                .flatMapMany(progressEvent -> {
                            if (!CollectionUtils.isEmpty(progressEvent.getAttachments())) {
                                return Flux.fromIterable(progressEvent.getAttachments().stream()
                                        .map(attachmentDetails -> buildSendEvent(progressEvent, attachmentDetails))
                                        .collect(Collectors.toList()));
                            } else if(!isFinalDemat)
                                return Flux.just(buildSendEvent(progressEvent, null));

                            log.info("Event with statusCode {} is a final demat without attachments, skipping sending to delivery-push", progressEvent.getStatusCode());
                            return Flux.empty();
                        }
                );
    }

    public static SendEvent buildSendEvent(PaperProgressStatusEvent progressEvent, it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.AttachmentDetails attachmentDetails) {
        SendEvent.SendEventBuilder builder = SendEvent.builder()
                .requestId(progressEvent.getRequestId())
                .statusCode(StatusCodeEnum.valueOf(EventStatusCodeEnum.valueOf(progressEvent.getStatusCode()).getStatus().name()))
                .statusDetail(progressEvent.getStatusCode())
                .statusDescription(progressEvent.getProductType() + " - " + progressEvent.getStatusCode() + " - " + progressEvent.getStatusDescription())
                .deliveryFailureCause(progressEvent.getDeliveryFailureCause())
                .registeredLetterCode(progressEvent.getRegisteredLetterCode())
                .statusDateTime(progressEvent.getStatusDateTime())
                .clientRequestTimeStamp(progressEvent.getClientRequestTimeStamp());

        if(Objects.nonNull(progressEvent.getDiscoveredAddress())){
            builder.discoveredAddress(toAnalogAddress(progressEvent.getDiscoveredAddress()));
        }
        if(Objects.nonNull(attachmentDetails)){
            builder.attachments(buildPaperAttachmentDetailsFromExternalChannels(List.of(attachmentDetails)));
        }

        return builder.build();
    }

    public static Flux<SendEvent> createSendEventsFromEventEntity(String requestId, Event event, StatusCodeEnum statusCode, String statusDetail, OffsetDateTime statusDateTime) {
        return Mono.just(event)
                .flatMapMany(progressEvent -> {
                            if (!CollectionUtils.isEmpty(progressEvent.getAttachments())) {
                                return Flux.fromIterable(progressEvent.getAttachments().stream()
                                        .map(attachmentDetails -> toSendEvent(requestId, event, statusCode, statusDetail, statusDateTime, attachmentDetails))
                                        .collect(Collectors.toList()));
                            }
                            return Flux.just(toSendEvent(requestId, event, statusCode, statusDetail, statusDateTime, null));
                        }
                );
    }

    public static SendEvent toSendEvent(String requestId, Event event, StatusCodeEnum statusCode, String statusDetail, OffsetDateTime statusDateTime, Attachment attachmentDetails) {
        SendEvent.SendEventBuilder builder = SendEvent.builder()
                .requestId(requestId)
                .statusCode(statusCode)
                .statusDetail(statusDetail)
                .statusDescription(event.getProductType() + " - " + statusDetail + " - " + event.getStatusDescription())
                .deliveryFailureCause(event.getDeliveryFailureCause())
                .registeredLetterCode(event.getRegisteredLetterCode())
                .statusDateTime(statusDateTime)
                .clientRequestTimeStamp(event.getRequestTimestamp().atOffset(ZoneOffset.UTC));

        if(Objects.nonNull(attachmentDetails)){
            builder.attachments(buildAttachmentDetails(event.getAttachments()));
        }

        return builder.build();
    }

    private static List<AttachmentDetails> buildAttachmentDetails(List<Attachment> attachments) {
        return attachments.stream().map(attachment -> AttachmentDetails.builder()
                .documentType(attachment.getDocumentType())
                .date(attachment.getDate().atOffset(ZoneOffset.UTC))
                .id(attachment.getId())
                .url(attachment.getUri())
                .build()).toList();
    }

    private static List<it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.AttachmentDetails> buildPaperAttachmentDetailsFromExternalChannels(List<it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.AttachmentDetails> attachments) {
        return attachments.stream().map(attachment -> AttachmentDetails.builder()
                .documentType(attachment.getDocumentType())
                .date(attachment.getDate())
                .id(attachment.getId())
                .url(attachment.getUri())
                .build()).toList();
    }

    public static AnalogAddress toAnalogAddress(DiscoveredAddress discoveredAddress) {
        return AnalogAddress.builder()
                .address(discoveredAddress.getAddress())
                .addressRow2(discoveredAddress.getAddressRow2())
                .cap(discoveredAddress.getCap())
                .pr(discoveredAddress.getPr())
                .city(discoveredAddress.getCity())
                .city2(discoveredAddress.getCity2())
                .country(discoveredAddress.getCountry())
                .fullname(discoveredAddress.getName())
                .nameRow2(discoveredAddress.getNameRow2())
                .build();
    }

    public static AnalogAddress toAnalogAddress(PaperAddress paperAddress) {
        return AnalogAddress.builder()
                .address(paperAddress.getAddress())
                .addressRow2(paperAddress.getAddressRow2())
                .cap(paperAddress.getCap())
                .pr(paperAddress.getPr())
                .city(paperAddress.getCity())
                .city2(paperAddress.getCity2())
                .country(paperAddress.getCountry())
                .fullname(paperAddress.getName())
                .nameRow2(paperAddress.getNameRow2())
                .build();
    }

}
