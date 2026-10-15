package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.AttachmentDetails;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.DiscoveredAddress;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.pndatavault.model.PaperAddress;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.model.HandlerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DuplicatedEventFilteringTest {

    @InjectMocks
    private DuplicatedEventFiltering duplicatedEventFiltering;

    @Mock
    private DataVaultClient dataVaultClient;

    private HandlerContext context;
    private OffsetDateTime offsetDateTime = OffsetDateTime.now();

    @BeforeEach
    void setup(){
        context = new HandlerContext();
        PaperTrackings paperTrackings = new PaperTrackings();
        paperTrackings.setState(PaperTrackingsState.DONE);
        Attachment attachment1 = new Attachment();
        attachment1.setId("attachment-id-1");
        attachment1.setDocumentType("DOCUMENT_TYPE");
        attachment1.setSha256("sha256hash");
        attachment1.setDate(offsetDateTime.toInstant());
        Event event = new Event();
        event.setId("event-id-1");
        event.setRequestTimestamp(Instant.now());
        event.setStatusCode("RECRN003C");
        event.setStatusTimestamp(Instant.now());
        event.setProductType(ProductType.AR.getValue());
        event.setAttachments(new ArrayList<>());
        event.setAnonymizedDiscoveredAddressId("anonymized_addr_377a1b51-d241-4ce3-b1c8-0802650e48f4");
        Event event1 = new Event();
        event1.setId("event-id-1");
        event1.setRequestTimestamp(Instant.now());
        event1.setStatusCode("RECRN004C");
        event1.setStatusTimestamp(Instant.now());
        event1.setProductType(ProductType.AR.getValue());
        event1.setAttachments(List.of(attachment1));
        event1.setAnonymizedDiscoveredAddressId("anonymized_addr_377a1b51-d241-4ce3-b1c8-0802650e48f4");
        Attachment attachment2 = new Attachment();
        attachment2.setId("attachment-id-2");
        attachment2.setDocumentType("DOCUMENT_TYPE2");
        attachment2.setDate(offsetDateTime.toInstant());
        Event event2 = new Event();
        event2.setId("event-id-2");
        event2.setRequestTimestamp(Instant.now());
        event2.setStatusCode("RECRN001B");
        event2.setStatusTimestamp(Instant.now());
        event2.setProductType(ProductType.AR.getValue());
        event2.setAttachments(List.of(attachment2));
        Attachment attachment3 = new Attachment();
        attachment3.setId("attachment-id-3");
        attachment3.setDocumentType("DOCUMENT_TYPE3");
        attachment3.setDate(offsetDateTime.toInstant());
        Event event3 = new Event();
        event3.setId("event-id-3");
        event3.setRequestTimestamp(Instant.now());
        event3.setStatusCode("RECRN002A");
        event3.setStatusTimestamp(Instant.now());
        event3.setProductType(ProductType.AR.getValue());
        event3.setAttachments(List.of(attachment3));
        paperTrackings.setEvents(List.of(event, event1, event2, event3));
        context.setPaperTrackings(paperTrackings);
    }

    @Test
    void duplicateEvent() {
        PaperProgressStatusEvent newEvent = new PaperProgressStatusEvent();
        newEvent.setRequestId("request-id");
        newEvent.setStatusCode("RECRN004C");
        newEvent.setStatusDateTime(context.getPaperTrackings().getEvents().getFirst().getStatusTimestamp().atOffset(java.time.ZoneOffset.UTC));
        newEvent.setProductType(ProductType.AR.getValue());
        AttachmentDetails attachmentDetails = new AttachmentDetails();
        attachmentDetails.setId("attachment-id-1");
        attachmentDetails.setDocumentType("DOCUMENT_TYPE");
        attachmentDetails.setUri("http://document-uri"); //verifico che l'uri non venga considerato nel confronto
        attachmentDetails.setSha256("sha256hash");
        attachmentDetails.setDate(offsetDateTime);
        newEvent.setAttachments(List.of(attachmentDetails));
        DiscoveredAddress discoveredAddress = new DiscoveredAddress();
        discoveredAddress.setAddress("Via Roma");
        discoveredAddress.setAddressRow2("Civico 1");
        discoveredAddress.setCap("00100");
        discoveredAddress.setCity("Roma");
        discoveredAddress.setPr("RM");
        discoveredAddress.setCountry("IT");
        newEvent.setDiscoveredAddress(discoveredAddress);
        context.setPaperProgressStatusEvent(newEvent);
        context.setEventId("event-id-4");

        when(dataVaultClient.deAnonymizeDiscoveredAddress(newEvent.getRequestId(), context.getPaperTrackings().getEvents().getFirst().getAnonymizedDiscoveredAddressId()))
                .thenReturn(Mono.just(getPaperAddress()));

        StepVerifier.create(duplicatedEventFiltering.execute(context))
                .verifyError(PnPaperTrackerValidationException.class);
    }

    @Test
    void sameEventWithDifferentField() {
        PaperProgressStatusEvent newEvent = new PaperProgressStatusEvent();
        newEvent.setStatusCode("RECRN004C");
        newEvent.setStatusDateTime(context.getPaperTrackings().getEvents().getFirst().getStatusTimestamp().atOffset(java.time.ZoneOffset.UTC));
        newEvent.setProductType(ProductType.AR.getValue());
        AttachmentDetails attachmentDetails = new AttachmentDetails();
        attachmentDetails.setId("attachment-id-1");
        attachmentDetails.setDocumentType("DOCUMENT_TYPE2");
        attachmentDetails.setDate(offsetDateTime);
        newEvent.setAttachments(List.of(attachmentDetails));
        context.setPaperProgressStatusEvent(newEvent);
        context.setEventId("event-id-4");

        StepVerifier.create(duplicatedEventFiltering.execute(context))
                .verifyComplete();
        verifyNoInteractions(dataVaultClient);
    }

    @Test
    void sameEventWithDifferentNoAttachments() {
        PaperProgressStatusEvent newEvent = new PaperProgressStatusEvent();
        newEvent.setStatusCode("RECRN003C");
        newEvent.setStatusDateTime(context.getPaperTrackings().getEvents().getFirst().getStatusTimestamp().atOffset(java.time.ZoneOffset.UTC));
        newEvent.setProductType(ProductType.AR.getValue());
        newEvent.setAttachments(Collections.emptyList());
        context.setPaperProgressStatusEvent(newEvent);
        context.setEventId("event-id");

        StepVerifier.create(duplicatedEventFiltering.execute(context))
                .verifyComplete();
        verifyNoInteractions(dataVaultClient);
    }

    @Test
    void newEvent() {
        PaperProgressStatusEvent newEvent = new PaperProgressStatusEvent();
        newEvent.setStatusCode("RECRN004B");
        newEvent.setStatusDateTime(context.getPaperTrackings().getEvents().getFirst().getStatusTimestamp().atOffset(java.time.ZoneOffset.UTC));
        newEvent.setProductType(ProductType.AR.getValue());
        AttachmentDetails attachmentDetails = new AttachmentDetails();
        attachmentDetails.setId("attachment-id-1");
        attachmentDetails.setDocumentType("DOCUMENT_TYPE2");
        newEvent.setAttachments(List.of(attachmentDetails));
        context.setPaperProgressStatusEvent(newEvent);
        context.setEventId("event-id-4");

        StepVerifier.create(duplicatedEventFiltering.execute(context))
                .verifyComplete();
        verifyNoInteractions(dataVaultClient);
    }

    @Test
    void sameEventWithAnonymizedDiscoveredAddressIdNull() {
        PaperProgressStatusEvent newEvent = new PaperProgressStatusEvent();
        newEvent.setStatusCode("RECRN001B");
        newEvent.setStatusDateTime(context.getPaperTrackings().getEvents().get(1).getStatusTimestamp().atOffset(java.time.ZoneOffset.UTC));
        newEvent.setProductType(ProductType.AR.getValue());
        AttachmentDetails attachmentDetails = new AttachmentDetails();
        attachmentDetails.setId("attachment-id-2");
        attachmentDetails.setDocumentType("DOCUMENT_TYPE2");
        attachmentDetails.setDate(offsetDateTime);
        newEvent.setAttachments(List.of(attachmentDetails));
        newEvent.setDiscoveredAddress(new DiscoveredAddress());
        context.setPaperProgressStatusEvent(newEvent);
        context.setEventId("event-id-4");

        StepVerifier.create(duplicatedEventFiltering.execute(context))
                .verifyComplete();
        verifyNoInteractions(dataVaultClient);
    }

    @Test
    void retryEventWithSameEventId() {
        PaperProgressStatusEvent newEvent = new PaperProgressStatusEvent();
        newEvent.setStatusCode("RECRN002A");
        newEvent.setStatusDateTime(context.getPaperTrackings().getEvents().getLast().getStatusTimestamp().atOffset(java.time.ZoneOffset.UTC));
        newEvent.setProductType(ProductType.AR.getValue());
        AttachmentDetails attachmentDetails = new AttachmentDetails();
        attachmentDetails.setId("attachment-id-3");
        attachmentDetails.setDocumentType("DOCUMENT_TYPE3");
        newEvent.setAttachments(List.of(attachmentDetails));
        context.setPaperProgressStatusEvent(newEvent);
        context.setEventId("event-id-3");

        StepVerifier.create(duplicatedEventFiltering.execute(context))
                .verifyComplete();
        verifyNoInteractions(dataVaultClient);
    }

    @Test
    void executeWithIsRedriveTrue() {
        context.setTrackingId("tracking-id-1");
        context.setRedrive(true);

        StepVerifier.create(duplicatedEventFiltering.execute(context))
                .verifyComplete();
        verifyNoInteractions(dataVaultClient);
    }

    private PaperAddress getPaperAddress() {
        PaperAddress paperAddress = new PaperAddress();
        paperAddress.setAddress("Via Roma");
        paperAddress.setAddressRow2("Civico 1");
        paperAddress.setCap("00100");
        paperAddress.setCity("Roma");
        paperAddress.setPr("RM");
        paperAddress.setCountry("IT");
        return paperAddress;
    }

}
