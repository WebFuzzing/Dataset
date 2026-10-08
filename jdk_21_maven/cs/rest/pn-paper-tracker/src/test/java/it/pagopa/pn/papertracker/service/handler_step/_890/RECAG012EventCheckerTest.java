package it.pagopa.pn.papertracker.service.handler_step._890;

import com.sngular.apigenerator.asyncapi.business_model.model.event.DataDTO;
import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataPayloadDTO;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.middleware.msclient.SafeStorageClient;
import it.pagopa.pn.papertracker.middleware.queue.model.OcrEvent;
import it.pagopa.pn.papertracker.middleware.queue.producer.OcrMomProducer;
import it.pagopa.pn.papertracker.model.DocumentTypeEnum;
import it.pagopa.pn.papertracker.model.FileType;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import it.pagopa.pn.papertracker.utils.OcrUtility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RECAG012EventCheckerTest {

    @Mock
    PaperTrackingsDAO paperTrackingsDAO;
    @Mock
    PnPaperTrackerConfigs cfg;
    @Mock
    OcrMomProducer ocrMomProducer;
    @Mock
    SafeStorageClient safeStorageClient;

    @InjectMocks
    private RECAG012EventChecker recag012EventChecker;

    private HandlerContext context;

    @BeforeEach
    void setUp() {
        OcrUtility ocrUtility = new OcrUtility(ocrMomProducer, safeStorageClient, cfg, paperTrackingsDAO);
        recag012EventChecker = new RECAG012EventChecker(ocrUtility);
        context = new HandlerContext();
        context.setTrackingId("trackingId");
        PaperTrackings paperTrackings = new PaperTrackings();
        paperTrackings.setTrackingId("trackingId");
        paperTrackings.setProductType(ProductType._890.getValue());
        paperTrackings.setValidationConfig(new ValidationConfig());
        PaperStatus paperStatus = new PaperStatus();
        paperStatus.setRegisteredLetterCode("RL123");
        paperTrackings.setPaperStatus(paperStatus);
        context.setPaperTrackings(paperTrackings);
    }

    @Test
    void execute_CurrentEventIsStockIntermediate_RequiredAttachmentsAndRecag012Present_OcrEnabled() {
        //Arrange
        context.getPaperTrackings().getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        when(safeStorageClient.getSafeStoragePresignedUrl("uri.pdf")).thenReturn(Mono.just("presigned-url"));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(context.getPaperTrackings()));
        context.getPaperTrackings().getValidationConfig().setRequiredAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue()));
        context.getPaperTrackings().getValidationConfig().setSendOcrAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue()));
        context.getPaperTrackings().getValidationConfig().setOcrFileTypes(List.of(FileType.PDF.getValue()));
        context.getPaperTrackings().setEvents(List.of(
                getEvent("RECAG005B", DocumentTypeEnum._23L.getValue(), "eventIdRECAG005B"),
                getEvent("RECAG012", null, "eventIdRECAG012")
        ));

        //Act
        StepVerifier.create(recag012EventChecker.execute(context))
                .verifyComplete();

        //Assert
        verify(safeStorageClient, times(1)).getSafeStoragePresignedUrl("uri.pdf");

        ArgumentCaptor<OcrEvent> ocrEventArgumentCaptor = ArgumentCaptor.forClass(OcrEvent.class);
        verify(ocrMomProducer, times(1)).push(ocrEventArgumentCaptor.capture());
        OcrDataPayloadDTO pushedEventPayload = ocrEventArgumentCaptor.getValue().getPayload();
        assertEquals("trackingId#eventIdRECAG012#23L", pushedEventPayload.getCommandId());
        assertEquals(DataDTO.ProductType._890, pushedEventPayload.getData().getProductType());
        assertEquals(DataDTO.DocumentType._23L, pushedEventPayload.getData().getDocumentType());
        assertNull(pushedEventPayload.getData().getUnifiedDeliveryDriver());
        assertEquals("presigned-url", pushedEventPayload.getData().getDetails().getAttachment());
        assertEquals("RL123", pushedEventPayload.getData().getDetails().getRegisteredLetterCode());
        assertNotNull(pushedEventPayload.getData().getDetails().getNotificationDate());
        assertNull(pushedEventPayload.getData().getDetails().getDeliveryFailureCause());
        assertEquals("RECAG012", pushedEventPayload.getData().getDetails().getDeliveryDetailCode());
        assertNull(pushedEventPayload.getData().getDetails().getDeliveryAttemptDate());

        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTracking = paperTrackingsArgumentCaptor.getValue();
        assertEquals(OcrStatusEnum.RUN, updatedPaperTracking.getValidationConfig().getOcrEnabled());
        assertEquals(PaperTrackingsState.AWAITING_OCR, updatedPaperTracking.getState());
        assertEquals(1, updatedPaperTracking.getValidationFlow().getOcrRequests().size());
        assertEquals("eventIdRECAG005B", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getAttachmentEventId());
        assertEquals("eventIdRECAG012", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getFinalEventId());
        assertEquals("uri.pdf", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getUri());
        assertEquals("23L", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getDocumentType());
        assertNotNull(updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getRequestTimestamp());

        assertFalse(context.isNeedToSendRECAG012A());
    }

    @Test
    void execute_CurrentEventIsStockIntermediate_RequiredAttachmentsAndRecag012Present_OcrDisabled() {
        //Arrange
        context.getPaperTrackings().getValidationConfig().setOcrEnabled(OcrStatusEnum.DISABLED);
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(context.getPaperTrackings()));
        context.getPaperTrackings().getValidationConfig().setRequiredAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue()));
        context.getPaperTrackings().getValidationConfig().setSendOcrAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue()));
        context.getPaperTrackings().setEvents(List.of(
                getEvent("RECAG005B", DocumentTypeEnum._23L.getValue(), "eventIdRECAG005B"),
                getEvent("RECAG012", null, "eventIdRECAG012")
        ));

        //Act
        StepVerifier.create(recag012EventChecker.execute(context))
                .verifyComplete();

        //Assert
        verifyNoInteractions(safeStorageClient);
        verifyNoInteractions(ocrMomProducer);

        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTracking = paperTrackingsArgumentCaptor.getValue();
        assertEquals(OcrStatusEnum.DISABLED, updatedPaperTracking.getValidationConfig().getOcrEnabled());
        assertTrue(updatedPaperTracking.getValidationFlow().getOcrRequests().isEmpty());
        assertNotNull(updatedPaperTracking.getValidationFlow().getRefinementDematValidationTimestamp());

        assertFalse(context.isNeedToSendRECAG012A());
    }

    @Test
    void execute_CurrentEventIsStockIntermediate_RequiredAttachmentsMissingAndRecag012Present() {
        //Arrange
        context.getPaperTrackings().getValidationConfig().setRequiredAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue(), DocumentTypeEnum.CAD.getValue()));
        context.getPaperTrackings().setEvents(List.of(
                getEvent("RECAG005B", DocumentTypeEnum._23L.getValue(), "eventIdRECAG005B"),
                getEvent("RECAG012", null, "eventIdRECAG012")
        ));

        //Act
        StepVerifier.create(recag012EventChecker.execute(context))
                .verifyComplete();

        //Assert
        verifyNoInteractions(safeStorageClient);
        verifyNoInteractions(ocrMomProducer);
        verifyNoInteractions(paperTrackingsDAO);

        assertTrue(context.isNeedToSendRECAG012A());
    }

    @Test
    void execute_CurrentEventIsStockIntermediate_RequiredAttachmentsPresentAndRecag012Missing() {
        //Arrange
        context.getPaperTrackings().getValidationConfig().setRequiredAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue()));
        context.getPaperTrackings().setEvents(List.of(
                getEvent("RECAG005B", DocumentTypeEnum._23L.getValue(), "eventIdRECAG005B"),
                getEvent("RECAG005A", null, "eventIdRECAG005A")
        ));

        //Act
        StepVerifier.create(recag012EventChecker.execute(context))
                .verifyComplete();

        //Assert
        verifyNoInteractions(safeStorageClient);
        verifyNoInteractions(ocrMomProducer);
        verifyNoInteractions(paperTrackingsDAO);

        assertFalse(context.isNeedToSendRECAG012A());
    }

    @Test
    void execute_CurrentEventIsRECAG012_RequiredAttachmentsPresent_OcrEnabled() {
        //Arrange
        context.getPaperTrackings().getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        when(safeStorageClient.getSafeStoragePresignedUrl("uri.pdf")).thenReturn(Mono.just("presigned-url"));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(context.getPaperTrackings()));
        context.getPaperTrackings().getValidationConfig().setRequiredAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue()));
        context.getPaperTrackings().getValidationConfig().setSendOcrAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue()));
        context.getPaperTrackings().getValidationConfig().setOcrFileTypes(List.of(FileType.PDF.getValue()));
        context.getPaperTrackings().setEvents(List.of(
                getEvent("RECAG012", null, "eventIdRECAG012"),
                getEvent("RECAG007B", DocumentTypeEnum._23L.getValue(), "eventIdRECAG007B")
        ));

        //Act
        StepVerifier.create(recag012EventChecker.execute(context))
                .verifyComplete();

        //Assert
        verify(safeStorageClient, times(1)).getSafeStoragePresignedUrl("uri.pdf");

        ArgumentCaptor<OcrEvent> ocrEventArgumentCaptor = ArgumentCaptor.forClass(OcrEvent.class);
        verify(ocrMomProducer, times(1)).push(ocrEventArgumentCaptor.capture());
        OcrDataPayloadDTO pushedEventPayload = ocrEventArgumentCaptor.getValue().getPayload();
        assertEquals("trackingId#eventIdRECAG012#23L", pushedEventPayload.getCommandId());
        assertEquals(DataDTO.ProductType._890, pushedEventPayload.getData().getProductType());
        assertEquals(DataDTO.DocumentType._23L, pushedEventPayload.getData().getDocumentType());
        assertNull(pushedEventPayload.getData().getUnifiedDeliveryDriver());
        assertEquals("presigned-url", pushedEventPayload.getData().getDetails().getAttachment());
        assertEquals("RL123", pushedEventPayload.getData().getDetails().getRegisteredLetterCode());
        assertNotNull(pushedEventPayload.getData().getDetails().getNotificationDate());
        assertNull(pushedEventPayload.getData().getDetails().getDeliveryFailureCause());
        assertEquals("RECAG012", pushedEventPayload.getData().getDetails().getDeliveryDetailCode());
        assertNull(pushedEventPayload.getData().getDetails().getDeliveryAttemptDate());

        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTracking = paperTrackingsArgumentCaptor.getValue();
        assertEquals(OcrStatusEnum.RUN, updatedPaperTracking.getValidationConfig().getOcrEnabled());
        assertEquals(PaperTrackingsState.AWAITING_OCR, updatedPaperTracking.getState());
        assertEquals(1, updatedPaperTracking.getValidationFlow().getOcrRequests().size());
        assertEquals("eventIdRECAG007B", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getAttachmentEventId());
        assertEquals("eventIdRECAG012", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getFinalEventId());
        assertEquals("uri.pdf", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getUri());
        assertEquals("23L", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getDocumentType());
        assertNotNull(updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getRequestTimestamp());

        assertFalse(context.isNeedToSendRECAG012A());
    }

    @Test
    void execute_CurrentEventIsRECAG012_TwoRequiredAttachmentsPresent_OcrEnabled() {
        //Arrange
        context.getPaperTrackings().getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        when(safeStorageClient.getSafeStoragePresignedUrl("uri.pdf")).thenReturn(Mono.just("presigned-url-1"));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(context.getPaperTrackings()));
        context.getPaperTrackings().getValidationConfig().setRequiredAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue(), DocumentTypeEnum.ARCAD.getValue(), DocumentTypeEnum.CAD.getValue()));
        context.getPaperTrackings().getValidationConfig().setSendOcrAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue(), DocumentTypeEnum.ARCAD.getValue()));
        context.getPaperTrackings().getValidationConfig().setOcrFileTypes(List.of(FileType.PDF.getValue()));
        context.getPaperTrackings().setEvents(List.of(
                getEvent("RECAG012", null, "eventIdRECAG012"),
                getEvent("RECAG007B", DocumentTypeEnum._23L.getValue(), "eventIdRECAG007B")
        ));

        //Act
        StepVerifier.create(recag012EventChecker.execute(context))
                .verifyComplete();

        //Assert
        verify(safeStorageClient, times(2)).getSafeStoragePresignedUrl("uri.pdf");

        ArgumentCaptor<OcrEvent> ocrEventArgumentCaptor = ArgumentCaptor.forClass(OcrEvent.class);
        verify(ocrMomProducer, times(2)).push(ocrEventArgumentCaptor.capture());
        List<OcrEvent> pushedEvents = ocrEventArgumentCaptor.getAllValues();
        OcrDataPayloadDTO secondPushedEventPayload = pushedEvents.getFirst().getPayload();
        OcrDataPayloadDTO firstPushedEventPayload = pushedEvents.getLast().getPayload();
        assertEquals("trackingId#eventIdRECAG012#ARCAD", firstPushedEventPayload.getCommandId());
        assertEquals("presigned-url-1", firstPushedEventPayload.getData().getDetails().getAttachment());
        assertEquals("RL123", firstPushedEventPayload.getData().getDetails().getRegisteredLetterCode());
        assertNotNull(firstPushedEventPayload.getData().getDetails().getNotificationDate());
        assertNull(firstPushedEventPayload.getData().getDetails().getDeliveryFailureCause());
        assertEquals("RECAG012", firstPushedEventPayload.getData().getDetails().getDeliveryDetailCode());
        assertNull(firstPushedEventPayload.getData().getDetails().getDeliveryAttemptDate());

        assertEquals("trackingId#eventIdRECAG012#23L", secondPushedEventPayload.getCommandId());
        assertEquals("presigned-url-1", secondPushedEventPayload.getData().getDetails().getAttachment());
        assertEquals("RL123", secondPushedEventPayload.getData().getDetails().getRegisteredLetterCode());
        assertNotNull(secondPushedEventPayload.getData().getDetails().getNotificationDate());
        assertNull(secondPushedEventPayload.getData().getDetails().getDeliveryFailureCause());
        assertEquals("RECAG012", secondPushedEventPayload.getData().getDetails().getDeliveryDetailCode());
        assertNull(secondPushedEventPayload.getData().getDetails().getDeliveryAttemptDate());

        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTracking = paperTrackingsArgumentCaptor.getValue();
        assertEquals(OcrStatusEnum.RUN, updatedPaperTracking.getValidationConfig().getOcrEnabled());
        assertEquals(PaperTrackingsState.AWAITING_OCR, updatedPaperTracking.getState());
        assertEquals(2, updatedPaperTracking.getValidationFlow().getOcrRequests().size());
        assertEquals("eventIdRECAG007B", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getAttachmentEventId());
        assertEquals("eventIdRECAG012", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getFinalEventId());
        assertEquals("uri.pdf", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getUri());
        assertEquals("23L", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getDocumentType());
        assertNotNull(updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getRequestTimestamp());

        assertEquals("eventIdRECAG007B", updatedPaperTracking.getValidationFlow().getOcrRequests().getLast().getAttachmentEventId());
        assertEquals("eventIdRECAG012", updatedPaperTracking.getValidationFlow().getOcrRequests().getLast().getFinalEventId());
        assertEquals("uri.pdf", updatedPaperTracking.getValidationFlow().getOcrRequests().getLast().getUri());
        assertEquals("ARCAD", updatedPaperTracking.getValidationFlow().getOcrRequests().getLast().getDocumentType());
        assertNotNull(updatedPaperTracking.getValidationFlow().getOcrRequests().getLast().getRequestTimestamp());

        assertFalse(context.isNeedToSendRECAG012A());
    }

    @Test
    void execute_CurrentEventIsRECAG012_Only23LPresentForOcr_OcrEnabled() {
        //Arrange
        context.getPaperTrackings().getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        when(safeStorageClient.getSafeStoragePresignedUrl("uri.pdf")).thenReturn(Mono.just("presigned-url-1"));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(context.getPaperTrackings()));
        context.getPaperTrackings().getValidationConfig().setRequiredAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue(), DocumentTypeEnum.ARCAD.getValue(), DocumentTypeEnum.CAD.getValue()));
        context.getPaperTrackings().getValidationConfig().setSendOcrAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue()));
        context.getPaperTrackings().getValidationConfig().setOcrFileTypes(List.of(FileType.PDF.getValue()));
        context.getPaperTrackings().setEvents(List.of(
                getEvent("RECAG012", null, "eventIdRECAG012"),
                getEvent("RECAG007B", DocumentTypeEnum._23L.getValue(), "eventIdRECAG007B")
        ));

        //Act
        StepVerifier.create(recag012EventChecker.execute(context))
                .verifyComplete();

        //Assert
        verify(safeStorageClient, times(1)).getSafeStoragePresignedUrl("uri.pdf");

        ArgumentCaptor<OcrEvent> ocrEventArgumentCaptor = ArgumentCaptor.forClass(OcrEvent.class);
        verify(ocrMomProducer, times(1)).push(ocrEventArgumentCaptor.capture());
        OcrDataPayloadDTO pushedEventPayload = ocrEventArgumentCaptor.getValue().getPayload();
        assertEquals("trackingId#eventIdRECAG012#23L", pushedEventPayload.getCommandId());
        assertEquals("presigned-url-1", pushedEventPayload.getData().getDetails().getAttachment());
        assertEquals("RL123", pushedEventPayload.getData().getDetails().getRegisteredLetterCode());
        assertNotNull(pushedEventPayload.getData().getDetails().getNotificationDate());
        assertNull(pushedEventPayload.getData().getDetails().getDeliveryFailureCause());
        assertEquals("RECAG012", pushedEventPayload.getData().getDetails().getDeliveryDetailCode());
        assertNull(pushedEventPayload.getData().getDetails().getDeliveryAttemptDate());

        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTracking = paperTrackingsArgumentCaptor.getValue();
        assertEquals(OcrStatusEnum.RUN, updatedPaperTracking.getValidationConfig().getOcrEnabled());
        assertEquals(PaperTrackingsState.AWAITING_OCR, updatedPaperTracking.getState());
        assertEquals(1, updatedPaperTracking.getValidationFlow().getOcrRequests().size());

        assertEquals("eventIdRECAG007B", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getAttachmentEventId());
        assertEquals("eventIdRECAG012", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getFinalEventId());
        assertEquals("uri.pdf", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getUri());
        assertEquals("23L", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getDocumentType());
        assertNotNull(updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getRequestTimestamp());

        assertFalse(context.isNeedToSendRECAG012A());
    }

    @Test
    void notSendToOCRForInvalidStateWhenReceiveIntermediateStockEvent() {
        //Arrange
        context.getPaperTrackings().getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        context.getPaperTrackings().setState(PaperTrackingsState.DONE);
        context.getPaperTrackings().getValidationConfig().setRequiredAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue(), DocumentTypeEnum.ARCAD.getValue(), DocumentTypeEnum.CAD.getValue()));
        context.getPaperTrackings().getValidationConfig().setSendOcrAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue()));
        context.getPaperTrackings().setEvents(List.of(
                getEvent("RECAG012", null, "eventIdRECAG012"),
                getEvent("RECAG007B", DocumentTypeEnum._23L.getValue(), "eventIdRECAG007B")
        ));
        context.setEventId("eventIdRECAG007B");
        //Act
        StepVerifier.create(recag012EventChecker.execute(context))
                .verifyComplete();

        //Assert
        verifyNoInteractions(safeStorageClient);
        verifyNoInteractions(ocrMomProducer);
        assertFalse(context.isNeedToSendRECAG012A());
    }

    @Test
    void notSendToOCRForInvalidStateWhenReceiveRECAG012Event() {
        //Arrange
        context.getPaperTrackings().getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        context.getPaperTrackings().setState(PaperTrackingsState.AWAITING_OCR);
        context.getPaperTrackings().getValidationConfig().setRequiredAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue(), DocumentTypeEnum.ARCAD.getValue(), DocumentTypeEnum.CAD.getValue()));
        context.getPaperTrackings().getValidationConfig().setSendOcrAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue()));
        context.getPaperTrackings().setEvents(List.of(
                getEvent("RECAG012", null, "eventIdRECAG012"),
                getEvent("RECAG007B", DocumentTypeEnum._23L.getValue(), "eventIdRECAG007B")
        ));
        context.setEventId("eventIdRECAG012");
        //Act
        StepVerifier.create(recag012EventChecker.execute(context))
                .verifyComplete();

        //Assert
        verifyNoInteractions(safeStorageClient);
        verifyNoInteractions(ocrMomProducer);
        assertFalse(context.isNeedToSendRECAG012A());
    }

    @Test
    void sendToOCROnlyLastAttachmentsForDocumentType() {
        //Arrange
        context.getPaperTrackings().getValidationConfig().setOcrEnabled(OcrStatusEnum.RUN);
        when(safeStorageClient.getSafeStoragePresignedUrl("uri.pdf")).thenReturn(Mono.just("presigned-url-1"));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(context.getPaperTrackings()));
        context.getPaperTrackings().getValidationConfig().setRequiredAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue(), DocumentTypeEnum.ARCAD.getValue(), DocumentTypeEnum.CAD.getValue(), DocumentTypeEnum.PLICO.getValue()));
        context.getPaperTrackings().getValidationConfig().setSendOcrAttachmentsRefinementStock890(List.of(DocumentTypeEnum._23L.getValue(), DocumentTypeEnum.ARCAD.getValue(), DocumentTypeEnum.PLICO.getValue()));
        context.getPaperTrackings().getValidationConfig().setOcrFileTypes(List.of(FileType.PDF.getValue()));
        context.getPaperTrackings().setEvents(List.of(
                getEvent("RECAG012", null, "eventIdRECAG012"),
                getEvent("RECAG007B", DocumentTypeEnum._23L.getValue(), "eventIdRECAG007B"),
                getEvent("RECAG011B", DocumentTypeEnum.PLICO.getValue(), "eventIdRECAG011B"),
                getEvent("RECAG007B", DocumentTypeEnum.INDAGINE.getValue(), "eventIdRECAG007B2")
        ));
        context.setEventId("eventIdRECAG011B2");

        //Act
        StepVerifier.create(recag012EventChecker.execute(context))
                .verifyComplete();

        //Assert
        verify(safeStorageClient, times(3)).getSafeStoragePresignedUrl("uri.pdf");

        verify(ocrMomProducer, times(3)).push(any(OcrEvent.class));

        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        PaperTrackings updatedPaperTracking = paperTrackingsArgumentCaptor.getValue();
        assertEquals(OcrStatusEnum.RUN, updatedPaperTracking.getValidationConfig().getOcrEnabled());
        assertEquals(PaperTrackingsState.AWAITING_OCR, updatedPaperTracking.getState());
        assertEquals(3, updatedPaperTracking.getValidationFlow().getOcrRequests().size());

        assertEquals("eventIdRECAG007B", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getAttachmentEventId());
        assertEquals("eventIdRECAG012", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getFinalEventId());
        assertEquals("23L", updatedPaperTracking.getValidationFlow().getOcrRequests().getFirst().getDocumentType());
        assertEquals("eventIdRECAG011B", updatedPaperTracking.getValidationFlow().getOcrRequests().get(1).getAttachmentEventId());
        assertEquals("eventIdRECAG012", updatedPaperTracking.getValidationFlow().getOcrRequests().get(1).getFinalEventId());
        assertEquals("Plico", updatedPaperTracking.getValidationFlow().getOcrRequests().get(1).getDocumentType());
        assertEquals("eventIdRECAG007B2", updatedPaperTracking.getValidationFlow().getOcrRequests().getLast().getAttachmentEventId());
        assertEquals("eventIdRECAG012", updatedPaperTracking.getValidationFlow().getOcrRequests().getLast().getFinalEventId());
        assertEquals("ARCAD", updatedPaperTracking.getValidationFlow().getOcrRequests().getLast().getDocumentType());

        assertFalse(context.isNeedToSendRECAG012A());
    }

    private Event getEvent(String statusCode, String documentType, String eventId) {
        Event event = new Event();
        event.setStatusCode(statusCode);
        event.setProductType(ProductType._890.getValue());
        event.setId(eventId);
        event.setRegisteredLetterCode("RL123");
        event.setCreatedAt(Instant.now());
        event.setStatusTimestamp(Instant.now());
        if (StringUtils.hasText(documentType)) {
            Attachment attachment = new Attachment();
            attachment.setUri("uri.pdf");
            attachment.setDocumentType("ARCAD");
            Attachment attachment1 = new Attachment();
            attachment1.setUri("uri.pdf");
            attachment1.setDocumentType(documentType);
            event.setAttachments(List.of(attachment, attachment1));
        }
        return event;
    }
}
