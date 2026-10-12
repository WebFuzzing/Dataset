package it.pagopa.pn.papertracker.service.handler_step._890;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.DocumentTypeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsState.DONE;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SequenceValidator890Test {

    @Mock
    private PaperTrackingsDAO paperTrackingsDAO;

    @Mock
    private PaperTrackerErrorService paperTrackerErrorService;

    private SequenceValidator890 sequenceValidator890;

    private HandlerContext context;

    @BeforeEach
    void setUp() {
        sequenceValidator890 = new SequenceValidator890(paperTrackingsDAO, paperTrackerErrorService);
        context = new HandlerContext();
        PaperProgressStatusEvent paperProgressStatusEvent = new PaperProgressStatusEvent();
        paperProgressStatusEvent.setStatusDateTime(OffsetDateTime.now());
        context.setPaperProgressStatusEvent(paperProgressStatusEvent);
    }

    @Test
    void executeWithStockStatusFalse() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        PaperTrackings paperTrackings = getPaperTrackings(eventId);
        Attachment attach = new Attachment();
        attach.setDocumentType("23L");
        paperTrackings.setState(DONE);
        paperTrackings.getEvents().forEach(event -> event.setStatusCode(event.getStatusCode().replaceAll("RECAG005", "RECAG002")));
        context.getPaperProgressStatusEvent().setStatusCode("RECAG002C");
        context.setPaperTrackings(paperTrackings);
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.empty());

        context.setEventId(eventId);
        // Act
        StepVerifier.create(sequenceValidator890.execute(context))
                .verifyComplete();

        // Assert
        verify(paperTrackingsDAO, times(1)).updateItem(any(), any());
    }

    @Test
    void executeWithStockStatusTrueAndStateDONE() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        PaperTrackings paperTrackings = getPaperTrackings(eventId);
        paperTrackings.setState(DONE);
        Attachment attach = new Attachment();
        attach.setDocumentType("23L");
        Attachment attach2 = new Attachment();
        attach2.setDocumentType("ARCAD");
        paperTrackings.getEvents().stream().filter(event -> event.getStatusCode().equalsIgnoreCase("RECAG005B"))
                .findFirst()
                .map(event -> {
                    event.setAttachments(List.of(attach, attach2));
                    return event;
                });
        Event event1 = buildEvent(UUID.randomUUID().toString(), "RECAG010", Instant.now(), Instant.now(), null);
        Event event2 = buildEvent(UUID.randomUUID().toString(), "RECAG011A", Instant.now(), Instant.now(), null);
        Event event3 = buildEvent(UUID.randomUUID().toString(), "RECAG012", Instant.now(), Instant.now(), null);

        List<Event> tmpList = new ArrayList<>(paperTrackings.getEvents());
        tmpList.add(event1);
        tmpList.add(event2);
        tmpList.add(event3);
        paperTrackings.setEvents(tmpList);
        context.getPaperProgressStatusEvent().setStatusCode("RECAG005C");
        context.setEventId(eventId);
        context.setPaperTrackings(paperTrackings);
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.empty());
        context.setEventId(eventId);
        // Act
        StepVerifier.create(sequenceValidator890.execute(context))
                .verifyComplete();

        // Assert
        verify(paperTrackingsDAO, times(1)).updateItem(any(), any());
    }

    @Test
    void executeWithStockStatusTrueAndStateAWAITING_REFINEMENT() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        PaperTrackings paperTrackings = getPaperTrackings(eventId);
        paperTrackings.setState(PaperTrackingsState.AWAITING_REFINEMENT);
        context.getPaperProgressStatusEvent().setStatusCode("RECAG005C");
        context.setPaperTrackings(paperTrackings);
        context.setEventId(eventId);
        // Act
        StepVerifier.create(sequenceValidator890.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException)
                .verify();

        // Assert
        verify(paperTrackingsDAO, never()).updateItem(any(), any());
    }

    @Test
    void executeWithStockStatusTrueAndStateAWAITING_OCR() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        PaperTrackings paperTrackings = getPaperTrackings(eventId);
        paperTrackings.setState(PaperTrackingsState.AWAITING_OCR);
        context.getPaperProgressStatusEvent().setStatusCode("RECAG005C");
        context.setPaperTrackings(paperTrackings);
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(new PaperTrackings()));
        context.setEventId(eventId);
        // Act
        StepVerifier.create(sequenceValidator890.execute(context))
                .verifyComplete();

        // Assert
        ArgumentCaptor<PaperTrackings> captor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), captor.capture());
        assertEquals(BusinessState.AWAITING_REFINEMENT_OCR, captor.getValue().getBusinessState());
        assertTrue(context.isStopExecution());
    }

    @Test
    void executeWithStockStatusTrueAndStateKO() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        PaperTrackings paperTrackings = getPaperTrackings(eventId);
        paperTrackings.setState(PaperTrackingsState.KO);
        context.getPaperProgressStatusEvent().setStatusCode("RECAG005C");
        context.setPaperTrackings(paperTrackings);
        context.setEventId(eventId);
        // Act
        StepVerifier.create(sequenceValidator890.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException)
                .verify();

        // Assert
        verify(paperTrackingsDAO, never()).updateItem(any(), any());
    }

    @Test
    void executeWithStockStatusFalseStrictFinalValidationStock890FalseSaveFourErrors() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        PaperTrackings paperTrackings = getPaperTrackings(eventId);
        paperTrackings.getValidationConfig().setStrictFinalValidationStock890(Boolean.FALSE);
        paperTrackings.setState(DONE);
        paperTrackings.setEvents(paperTrackings.getEvents()
                .stream()
                .filter(event -> !event.getStatusCode().equalsIgnoreCase("RECAG005B"))
                .collect(Collectors.toList()));
        paperTrackings.getEvents().getFirst().setStatusTimestamp(Instant.now().plusSeconds(10));
        paperTrackings.getEvents().getFirst().setRegisteredLetterCode("REG999");
        context.getPaperProgressStatusEvent().setStatusCode("RECAG005C");
        context.setPaperTrackings(paperTrackings);
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.empty());
        when(paperTrackerErrorService.insertPaperTrackingsError(any())).thenReturn(Mono.just(new PaperTrackingsErrors()));
        context.setEventId(eventId);
        // Act
        StepVerifier.create(sequenceValidator890.execute(context))
                .verifyComplete();

        // Assert
        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackerErrorService, times(4)).insertPaperTrackingsError(any());
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        assertNull(paperTrackingsArgumentCaptor.getValue().getPaperStatus().getValidatedSequenceTimestamp());
        assertNull(paperTrackingsArgumentCaptor.getValue().getPaperStatus().getRegisteredLetterCode());
        assertEquals(2, paperTrackingsArgumentCaptor.getValue().getPaperStatus().getValidatedEvents().size());
        assertNull(paperTrackingsArgumentCaptor.getValue().getPaperStatus().getDeliveryFailureCause());
        assertNotNull(paperTrackingsArgumentCaptor.getValue().getValidationFlow().getSequencesValidationTimestamp());
    }

    @Test
    void executeWithDifferentRegisteredLetterCodeForRECAG012() {
        // Arrange
        String eventId = UUID.randomUUID().toString();
        PaperTrackings paperTrackings = getPaperTrackings(eventId);
        paperTrackings.setState(DONE);
        Attachment attach = new Attachment();
        attach.setDocumentType("ARCAD");
        paperTrackings.getEvents().stream().filter(event -> event.getStatusCode().equalsIgnoreCase("RECAG005B"))
                .findFirst()
                .map(event -> {
                    event.getAttachments().add(attach);
                    return event;
                });
        Event event1 = buildEvent(UUID.randomUUID().toString(), "RECAG010", Instant.now(), Instant.now(), null);
        Event event2 = buildEvent(UUID.randomUUID().toString(), "RECAG011A", Instant.now(), Instant.now(), null);
        Event event3 = buildEvent(UUID.randomUUID().toString(), "RECAG012", Instant.now(), Instant.now(), null);
        event3.setRegisteredLetterCode("REG999");

        List<Event> tmpList = new ArrayList<>(paperTrackings.getEvents());
        tmpList.add(event1);
        tmpList.add(event2);
        tmpList.add(event3);
        paperTrackings.setEvents(tmpList);
        context.getPaperProgressStatusEvent().setStatusCode("RECAG005C");
        context.setPaperTrackings(paperTrackings);
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.empty());
        context.setEventId(eventId);
        // Act
        StepVerifier.create(sequenceValidator890.execute(context))
                .verifyComplete();

        // Assert
        ArgumentCaptor<PaperTrackings> paperTrackingsArgumentCaptor = ArgumentCaptor.forClass(PaperTrackings.class);
        verify(paperTrackingsDAO, times(1)).updateItem(any(), paperTrackingsArgumentCaptor.capture());
        assertEquals("REG123", paperTrackingsArgumentCaptor.getValue().getPaperStatus().getRegisteredLetterCode());
    }

    private PaperTrackings getPaperTrackings(String eventId) {
        Instant timestamp = Instant.now();
        Instant businessTimestamp = Instant.now();
        PaperTrackings paperTrackings = new PaperTrackings();
        paperTrackings.setPaperStatus(new PaperStatus());
        paperTrackings.setValidationFlow(new ValidationFlow());
        paperTrackings.setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECAG005A", timestamp, businessTimestamp, null),
                buildEvent(UUID.randomUUID().toString(), "RECAG005B", timestamp, businessTimestamp.plusSeconds(1), List.of(DocumentTypeEnum._23L.getValue())),
                buildEvent(eventId, "RECAG005C", timestamp, businessTimestamp.plusSeconds(2), null)
        ));
        ValidationConfig validationConfig = new ValidationConfig();
        validationConfig.setStrictFinalValidationStock890(Boolean.TRUE);
        validationConfig.setSendOcrAttachmentsFinalValidationStock890(List.of("ARCAD", "CAD"));
        validationConfig.setSendOcrAttachmentsFinalValidation(List.of("Plico", "AR", "23L"));
        validationConfig.setRequiredAttachmentsRefinementStock890(List.of("23L"));
        validationConfig.setOcrEnabled(OcrStatusEnum.DISABLED);
        paperTrackings.setValidationConfig(validationConfig);
        return paperTrackings;
    }

    private Event buildEvent(String id, String statusCode, Instant statusTimestamp, Instant requestTimestamp, List<String> attachmentTypes) {
        Event event = new Event();
        event.setAttachments(new ArrayList<>());
        event.setStatusCode(statusCode);
        event.setId(id);
        event.setRegisteredLetterCode("REG123");
        event.setStatusTimestamp(statusTimestamp);
        event.setRequestTimestamp(requestTimestamp);
        event.setDeliveryFailureCause("");

        if (!CollectionUtils.isEmpty(attachmentTypes)) {
            for (String type : attachmentTypes) {
                Attachment attachment = new Attachment();
                attachment.setDocumentType(type);
                event.getAttachments().add(attachment);
            }
        }

        return event;
    }
}
