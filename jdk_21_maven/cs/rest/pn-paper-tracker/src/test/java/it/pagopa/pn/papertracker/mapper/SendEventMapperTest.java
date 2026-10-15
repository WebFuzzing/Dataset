package it.pagopa.pn.papertracker.mapper;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.AttachmentDetails;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;

import java.time.OffsetDateTime;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class SendEventMapperTest {

    @Test
    void createSendEventsFromPaperProgressStatusEvent_WithAttachments_ReturnsSendEvent() {
        // Arrange
        AttachmentDetails attachment = new AttachmentDetails();
        attachment.setId("attch_001");
        attachment.setDocumentType("23L");
        attachment.setDate(OffsetDateTime.now());
        attachment.setUri("http://example.com/document.pdf");
        PaperProgressStatusEvent event = getPaperProgressStatusEvent("RECRN002F", List.of(attachment));

        // Act & Assert
        StepVerifier.create(SendEventMapper.createSendEventsFromPaperProgressStatusEvent(event))
                .assertNext(sendEvent -> {
                    Assertions.assertEquals("RECRN002F", sendEvent.getStatusDetail());
                    Assertions.assertEquals(1, sendEvent.getAttachments().size());
                    Assertions.assertEquals("attch_001", sendEvent.getAttachments().getFirst().getId());
                    Assertions.assertEquals("23L", sendEvent.getAttachments().getFirst().getDocumentType());
                })
                .verifyComplete();
    }

    @Test
    void createSendEventsFromPaperProgressStatusEvent_NoAttachmentsAndNotFinalDemat_ReturnsSendEvent() {
        // Arrange
        PaperProgressStatusEvent event = getPaperProgressStatusEvent("RECRN001C", null);

        // Act & Assert
        StepVerifier.create(SendEventMapper.createSendEventsFromPaperProgressStatusEvent(event))
                .assertNext(sendEvent -> {
                    Assertions.assertEquals("RECRN001C", sendEvent.getStatusDetail());
                    Assertions.assertNull(sendEvent.getAttachments());
                })
                .verifyComplete();
    }

    @Test
    void createSendEventsFromPaperProgressStatusEvent_NoAttachmentsAndIsFinalDemat_ReturnsEmpty() {
        // Arrange
        PaperProgressStatusEvent event = getPaperProgressStatusEvent("RECRN001B", null);

        // Act & Assert
        StepVerifier.create(SendEventMapper.createSendEventsFromPaperProgressStatusEvent(event))
                .expectNextCount(0)
                .verifyComplete();
    }

    private PaperProgressStatusEvent getPaperProgressStatusEvent(String statusCode, List<AttachmentDetails> attachments) {
        PaperProgressStatusEvent event = new PaperProgressStatusEvent();
        event.setRequestId("req_123");
        event.setStatusCode(statusCode);
        event.setAttachments(attachments);
        return event;
    }

}