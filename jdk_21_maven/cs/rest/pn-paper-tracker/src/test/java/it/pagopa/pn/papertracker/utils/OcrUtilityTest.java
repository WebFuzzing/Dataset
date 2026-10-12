package it.pagopa.pn.papertracker.utils;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.papertracker.middleware.queue.model.OcrEvent;
import it.pagopa.pn.papertracker.middleware.queue.producer.OcrMomProducer;
import it.pagopa.pn.papertracker.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECAG010A;
import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECAG012;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrUtilityTest {
    @Mock
    OcrMomProducer ocrMomProducer;
    @Mock
    SafeStorageClient safeStorageClient;
    @Mock
    PnPaperTrackerConfigs cfg;
    @Mock
    PaperTrackingsDAO paperTrackingsDAO;

    @InjectMocks
    OcrUtility ocrUtility;

    private HandlerContext context;
    private PaperTrackings paperTrackings;

    @BeforeEach
    void setUp() {
        ocrUtility = new OcrUtility(ocrMomProducer, safeStorageClient, cfg, paperTrackingsDAO);
        context = new HandlerContext();
        paperTrackings = new PaperTrackings();
        paperTrackings.setTrackingId("trackingId");
        paperTrackings.setProductType(ProductType.AR.getValue());
        paperTrackings.setValidationConfig(new ValidationConfig());
        paperTrackings.setPaperStatus(new PaperStatus());
        paperTrackings.setEvents(new ArrayList<>());
        context.setPaperTrackings(paperTrackings);
    }

    @Test
    void checkAndSendToOcr_OcrEnabled_ValidAttachments() {
        // Arrange
        paperTrackings.getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        paperTrackings.getValidationConfig().setOcrFileTypes(List.of(FileType.PDF.getValue()));
        Map<String, List<Attachment>> attachments = new HashMap<>();
        Attachment att = new Attachment();
        att.setUri("uri.pdf");
        att.setDocumentType("ARCAD");
        attachments.put("attachmentEventId", List.of(att));
        Event event = new Event();
        event.setId("finalEventId");
        event.setStatusCode(RECAG012.name());
        event.setStatusTimestamp(Instant.now());
        when(safeStorageClient.getSafeStoragePresignedUrl("uri.pdf")).thenReturn(Mono.just("presigned-url-1"));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(ocrUtility.checkAndSendToOcr(event, attachments, context))
                .expectNext(true)
                .verifyComplete();

        // Assert
        verify(ocrMomProducer, times(1)).push(any(OcrEvent.class));
        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTrackings = paperTrackingsArgumentCaptor.getValue();
        assertEquals(1, updatedPaperTrackings.getValidationFlow().getOcrRequests().size());
        assertEquals("attachmentEventId", updatedPaperTrackings.getValidationFlow().getOcrRequests().getFirst().getAttachmentEventId());
        assertEquals("finalEventId", updatedPaperTrackings.getValidationFlow().getOcrRequests().getFirst().getFinalEventId());
        assertEquals("ARCAD", updatedPaperTrackings.getValidationFlow().getOcrRequests().getFirst().getDocumentType());
    }

    @Test
    void checkAndSendToOcr_OcrDisabled() {
        // Arrange
        paperTrackings.getValidationConfig().setOcrEnabled(OcrStatusEnum.DISABLED);
        Map<String, List<Attachment>> attachments = new HashMap<>();
        Event event = new Event();
        event.setStatusCode(RECAG012.name());
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(ocrUtility.checkAndSendToOcr(event, attachments, context))
                .expectNext(false)
                .verifyComplete();

        // Assert
        verifyNoInteractions(ocrMomProducer);
        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTrackings = paperTrackingsArgumentCaptor.getValue();
        assertTrue(updatedPaperTrackings.getValidationFlow().getOcrRequests().isEmpty());
    }

    @Test
    void checkAndSendToOcr_OcrEnabled_NoValidAttachments() {
        // Arrange
        paperTrackings.getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        paperTrackings.getValidationConfig().setOcrFileTypes(List.of(FileType.PDF.getValue()));
        Map<String, List<Attachment>> attachments = new HashMap<>();
        Attachment att = new Attachment();
        att.setUri("uri.txt");
        att.setDocumentType("ARCAD");
        attachments.put("eventId", List.of(att));
        Event event = new Event();
        event.setStatusCode(RECAG012.name());
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(ocrUtility.checkAndSendToOcr(event, attachments, context))
                .expectNext(false)
                .verifyComplete();

        // Assert
        verifyNoInteractions(ocrMomProducer);
        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTrackings = paperTrackingsArgumentCaptor.getValue();
        assertTrue(updatedPaperTrackings.getValidationFlow().getOcrRequests().isEmpty());
    }

    @Test
    void checkAndSendToOcr_OcrEnabled_EmptyAttachments() {
        // Arrange
        paperTrackings.getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        Event event = new Event();
        event.setStatusCode(RECAG012.name());
        Map<String, List<Attachment>> attachments = new HashMap<>();
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(ocrUtility.checkAndSendToOcr(event, attachments, context))
                .expectNext(false)
                .verifyComplete();

        // Assert
        verifyNoInteractions(ocrMomProducer);
        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTrackings = paperTrackingsArgumentCaptor.getValue();
        assertTrue(updatedPaperTrackings.getValidationFlow().getOcrRequests().isEmpty());
    }

    @Test
    void checkAndSendToOcr_OcrDry_ValidAttachments() {
        // Arrange
        paperTrackings.getValidationConfig().setOcrEnabled(OcrStatusEnum.DRY);
        paperTrackings.getValidationConfig().setOcrFileTypes(List.of(FileType.PDF.getValue()));
        Map<String, List<Attachment>> attachments = new HashMap<>();
        Attachment att = new Attachment();
        att.setUri("uri.pdf");
        att.setDocumentType("ARCAD");
        attachments.put("attachmentEventId", List.of(att));
        Event event = new Event();
        event.setId("finalEventId");
        event.setStatusCode(RECAG012.name());
        event.setStatusTimestamp(Instant.now());
        when(safeStorageClient.getSafeStoragePresignedUrl("uri.pdf")).thenReturn(Mono.just("presigned-url-1"));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(ocrUtility.checkAndSendToOcr(event, attachments, context))
                .expectNext(false)
                .verifyComplete();

        // Assert
        verify(ocrMomProducer, times(1)).push(any(OcrEvent.class));
        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTrackings = paperTrackingsArgumentCaptor.getValue();
        assertEquals(1, updatedPaperTrackings.getValidationFlow().getOcrRequests().size());
        assertEquals("attachmentEventId", updatedPaperTrackings.getValidationFlow().getOcrRequests().getFirst().getAttachmentEventId());
        assertEquals("finalEventId", updatedPaperTrackings.getValidationFlow().getOcrRequests().getFirst().getFinalEventId());
        assertEquals("ARCAD", updatedPaperTrackings.getValidationFlow().getOcrRequests().getFirst().getDocumentType());
    }

    @Test
    void checkAndSendToOcr_OcrEnabled_MixedAttachments_OnlyValidAreProcessed() {
        // Arrange
        paperTrackings.getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        paperTrackings.getValidationConfig().setOcrFileTypes(List.of(FileType.PDF.getValue()));

        Map<String, List<Attachment>> attachments = new HashMap<>();
        attachments.put("eventId1", List.of(buildAttachment("valid.pdf", "ARCAD", null, null)));
        attachments.put("eventId2", List.of(buildAttachment("valid2.PDF", "ARCAD", SourceType.SCANNED.name(), OriginType.ORIGINAL.name())));
        attachments.put("eventId3", List.of(buildAttachment("valid3.pdf", "ARCAD", SourceType.SCANNED.name(), null)));
        attachments.put("eventId4", List.of(buildAttachment("invalid-source.pdf", "ARCAD", SourceType.DIGITAL.name(), OriginType.ORIGINAL.name())));
        attachments.put("eventId5", List.of(buildAttachment("invalid-origin.pdf", "ARCAD", SourceType.SCANNED.name(), OriginType.DUPLICATED.name())));
        attachments.put("eventId6", List.of(buildAttachment("invalid.txt", "ARCAD", null, null)));

        Event event = buildEvent();

        when(safeStorageClient.getSafeStoragePresignedUrl("valid.pdf")).thenReturn(Mono.just("presigned-valid"));
        when(safeStorageClient.getSafeStoragePresignedUrl("valid2.PDF")).thenReturn(Mono.just("presigned-valid2"));
        when(safeStorageClient.getSafeStoragePresignedUrl("valid3.pdf")).thenReturn(Mono.just("presigned-valid3"));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(ocrUtility.checkAndSendToOcr(event, attachments, context))
                .expectNext(true)
                .verifyComplete();

        // Assert
        verify(safeStorageClient, times(1)).getSafeStoragePresignedUrl("valid.pdf");
        verify(safeStorageClient, times(1)).getSafeStoragePresignedUrl("valid2.PDF");
        verify(safeStorageClient, times(1)).getSafeStoragePresignedUrl("valid3.pdf");
        verify(safeStorageClient, never()).getSafeStoragePresignedUrl("invalid-source.pdf");
        verify(safeStorageClient, never()).getSafeStoragePresignedUrl("invalid-origin.pdf");
        verify(safeStorageClient, never()).getSafeStoragePresignedUrl("invalid.txt");

        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTrackings = paperTrackingsArgumentCaptor.getValue();

        assertEquals(OcrStatusEnum.RUN, updatedPaperTrackings.getValidationConfig().getOcrEnabled());
        assertEquals(3, updatedPaperTrackings.getValidationFlow().getOcrRequests().size());
        assertTrue(updatedPaperTrackings.getValidationFlow().getOcrRequests().stream()
                .map(OcrRequest::getUri)
                .toList()
                .containsAll(List.of("valid.pdf", "valid2.PDF", "valid3.pdf")));
    }

    @Test
    void checkAndSendToOcr_OcrEnabled_DeliveryAttemptDate() {
        // Arrange
        paperTrackings.getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        paperTrackings.getValidationConfig().setOcrFileTypes(List.of(FileType.PDF.getValue()));

        Event recag010aEvent = new Event();
        recag010aEvent.setStatusCode(RECAG010A.name());
        recag010aEvent.setRequestTimestamp(Instant.parse("2026-01-10T11:00:00Z"));
        recag010aEvent.setStatusTimestamp(Instant.parse("2026-01-10T12:00:00Z"));

        Event ignoredEvent = new Event();
        ignoredEvent.setStatusCode(RECAG012.name());
        ignoredEvent.setRequestTimestamp(Instant.parse("2026-01-10T13:00:00Z"));
        ignoredEvent.setStatusTimestamp(Instant.parse("2026-01-10T14:00:00Z"));

        paperTrackings.setEvents(List.of(recag010aEvent, ignoredEvent));

        Map<String, List<Attachment>> attachments = new HashMap<>();
        Attachment attachment = new Attachment();
        attachment.setUri("uri.pdf");
        attachment.setDocumentType("ARCAD");
        attachments.put("attachmentEventId", List.of(attachment));

        Event finalEvent = new Event();
        finalEvent.setId("finalEventId");
        finalEvent.setStatusCode(RECAG012.name());
        finalEvent.setStatusTimestamp(Instant.parse("2026-01-10T15:00:00Z"));

        when(safeStorageClient.getSafeStoragePresignedUrl("uri.pdf")).thenReturn(Mono.just("presigned-url-1"));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(ocrUtility.checkAndSendToOcr(finalEvent, attachments, context))
                .expectNext(true)
                .verifyComplete();

        // Assert
        ArgumentCaptor<OcrEvent> ocrEventArgumentCaptor = ArgumentCaptor.forClass(OcrEvent.class);
        verify(ocrMomProducer, times(1)).push(ocrEventArgumentCaptor.capture());

        LocalDateTime expectedDeliveryAttemptDate = LocalDateTime.ofInstant(recag010aEvent.getStatusTimestamp(), ZoneOffset.UTC);
        LocalDateTime actualDeliveryAttemptDate = ocrEventArgumentCaptor.getValue()
                .getPayload()
                .getData()
                .getDetails()
                .getDeliveryAttemptDate();

        assertEquals(expectedDeliveryAttemptDate, actualDeliveryAttemptDate);
    }

    private Event buildEvent() {
        Event event = new Event();
        event.setId("finalEventId");
        event.setStatusCode(RECAG012.name());
        event.setStatusTimestamp(Instant.now());
        return event;
    }

    private Attachment buildAttachment(String uri, String documentType, String sourceType, String originType) {
        Attachment attachment = new Attachment();
        attachment.setUri(uri);
        attachment.setDocumentType(documentType);
        attachment.setSourceType(sourceType);
        attachment.setOriginType(originType);
        return attachment;
    }

}