package it.pagopa.pn.papertracker.service.handler_step.RIR;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.DocumentTypeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;

class SequenceValidatorRirTest {

    @Mock
    private PaperTrackingsDAO paperTrackingsDAO;

    @Mock
    private PaperTrackerErrorService paperTrackerErrorService;

    private SequenceValidatorRir sequenceValidatorRir;

    @InjectMocks
    private HandlerContext context;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        PaperProgressStatusEvent paperProgressStatusEvent = new PaperProgressStatusEvent();
        paperProgressStatusEvent.setStatusCode("RECRI003C");
        PaperTrackings paperTrackings = getPaperTrackings();
        context.setPaperTrackings(paperTrackings);
        context.setPaperProgressStatusEvent(paperProgressStatusEvent);
        sequenceValidatorRir = new SequenceValidatorRir(paperTrackingsDAO, paperTrackerErrorService);
    }

    private PaperTrackings getPaperTrackings() {
        PaperTrackings paperTrackings = new PaperTrackings();
        paperTrackings.setPaperStatus(new PaperStatus());
        paperTrackings.setValidationFlow(new ValidationFlow());
        paperTrackings.setValidationConfig(new ValidationConfig());
        return paperTrackings;
    }

    @Test
    void validateSequenceValidMultipleFlow1() {

        //Arrange
        Instant timestamp = Instant.now();
        Instant businessTimestamp = Instant.now();
        String finalEventId = UUID.randomUUID().toString();
        context.getPaperTrackings().setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECRI001", timestamp.plusSeconds(2), businessTimestamp.plusSeconds(1), "REG1", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI002", timestamp.plusSeconds(3), businessTimestamp.plusSeconds(2), "REG1", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI001", timestamp.plusSeconds(4), businessTimestamp.plusSeconds(3), "REG1", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI002", timestamp.plusSeconds(4), businessTimestamp.plusSeconds(4), "REG1", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003A", timestamp.plusSeconds(4), businessTimestamp.plusSeconds(5), "REG1", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003B", timestamp.plusSeconds(4), businessTimestamp.plusSeconds(6), "REG1", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003A", timestamp.plusSeconds(4), businessTimestamp.plusSeconds(7), "REG1", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003A", timestamp.plusSeconds(5), businessTimestamp.plusSeconds(8), "REG1", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003B", timestamp.plusSeconds(5), businessTimestamp.plusSeconds(9), "REG1", "", List.of(DocumentTypeEnum.AR.getValue())),
                buildEvent(finalEventId, "RECRI003C", timestamp.plusSeconds(5), businessTimestamp.plusSeconds(11), "REG1", "", null)
        ));

        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.empty());

        // Act
       context.setEventId(finalEventId);
        StepVerifier.create(sequenceValidatorRir.execute(context))
                .verifyComplete();

        // Assert
        verify(paperTrackingsDAO, times(1)).updateItem(any(), any());
    }

    @Test
    void validateSequenceValidFlow003() {
        //Arrange
        Instant timestamp = Instant.now();
        Instant businessTimestamp = Instant.now();
        String finalEventId = UUID.randomUUID().toString();
        context.getPaperTrackings().setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECRI001", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI002", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003A", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003B", timestamp, businessTimestamp.plusSeconds(1), "REG123", "", List.of(DocumentTypeEnum.AR.getValue())),
                buildEvent(finalEventId, "RECRI003C", timestamp, businessTimestamp.plusSeconds(2), "REG123", "", null)
        ));

        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.empty());

        // Act
       context.setEventId(finalEventId);
        StepVerifier.create(sequenceValidatorRir.execute(context))
                .verifyComplete();

        // Assert
        verify(paperTrackingsDAO, times(1)).updateItem(any(), any());
    }


    @Test
    void validateSequenceValidFlow004() {
        // Arrange
        context.getPaperProgressStatusEvent().setStatusCode("RECRI004C");
        Instant timestamp = Instant.now();
        Instant businessTimestamp = Instant.now();
        String finalEventId = UUID.randomUUID().toString();
        context.getPaperTrackings().setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECRI001", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI002", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI004A", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI004B", timestamp, businessTimestamp.plusSeconds(1), "REG123", "", List.of(DocumentTypeEnum.PLICO.getValue())),
                buildEvent(finalEventId, "RECRI004C", timestamp, businessTimestamp.plusSeconds(2), "REG123", "", null)
        ));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.empty());

        // Act
       context.setEventId(finalEventId);
        StepVerifier.create(sequenceValidatorRir.execute(context))
                .verifyComplete();

        // Assert
        verify(paperTrackingsDAO, times(1)).updateItem(any(), any());
    }

    @Test
    void validateSequenceValidFlow004MultiDocumentFailure() {
        // Arrange
        context.getPaperProgressStatusEvent().setStatusCode("RECRI004C");

        Instant timestamp = Instant.now();
        Instant businessTimestamp = Instant.now();

        String finalEventId = UUID.randomUUID().toString();
        context.getPaperTrackings().setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECRI001", timestamp, businessTimestamp, "", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI002", timestamp, businessTimestamp, "", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI004A", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI004B", timestamp, businessTimestamp.plusSeconds(1), "REG123", "F01", List.of(DocumentTypeEnum.INDAGINE.getValue())),
                buildEvent(finalEventId, "RECRI004C", timestamp, businessTimestamp.plusSeconds(2), "REG123", "", null)
        ));

        // Act & Assert
       context.setEventId(finalEventId);
        StepVerifier.create(sequenceValidatorRir.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().contains("Missed required attachments for the sequence validation: [Plico]"))
                .verify();
    }


    @Test
    void validateSequenceInvalidEventCount() {
        // Arrange
        String finalEventId = UUID.randomUUID().toString();
        context.getPaperTrackings().setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECRI003A", Instant.now(), Instant.now(), "REG123", "", null)
        ));

        // Act & Assert
       context.setEventId(finalEventId);
        StepVerifier.create(sequenceValidatorRir.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().contains("Necessary status code not found in events"))
                .verify();
    }

    @Test
    void invalidBusinessTimestamp() {
        // Arrange
        Instant timestamp = Instant.now();
        Instant businessTimestamp = Instant.now();

        String finalEventId = UUID.randomUUID().toString();
        context.getPaperTrackings().setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECRI001", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI002", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003A", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003B", timestamp.plusSeconds(1), businessTimestamp.plusSeconds(1), "REG123", "", List.of(DocumentTypeEnum.AR.getValue())),
                buildEvent(finalEventId, "RECRI003C", timestamp.plusSeconds(2), businessTimestamp.plusSeconds(2), "REG123", "", null)
        ));

        // Act & Assert
       context.setEventId(finalEventId);
        StepVerifier.create(sequenceValidatorRir.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().contains("Invalid business timestamps"))
                .verify();
    }

    @Test
    void validateSequenceInvalidSequenceSize() {
        // Arrange
        context.getPaperProgressStatusEvent().setStatusCode("RECRI004C");

        Instant timestamp = Instant.now();
        Instant businessTimestamp = Instant.now();

        String finalEventId = UUID.randomUUID().toString();
        context.getPaperTrackings().setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECRI003A", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003B", timestamp, businessTimestamp, "REG123", "", List.of(DocumentTypeEnum.AR.getValue())),
                buildEvent(finalEventId, "RECRI004C", timestamp, businessTimestamp, "REG123", "", null)
        ));

        // Act & Assert
       context.setEventId(finalEventId);
        StepVerifier.create(sequenceValidatorRir.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().contains("Necessary status code not found in events"))
                .verify();
    }


    @Test
    void validateSequenceInvalidRegisteredLetterCode() {
        // Arrange
        Instant timestamp = Instant.now();
        Instant businessTimestamp = Instant.now();

        String finalEventId = UUID.randomUUID().toString();
        context.getPaperTrackings().setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECRI001", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI002", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003A", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI003B", timestamp, businessTimestamp.plusSeconds(1), "REG444", "", List.of(DocumentTypeEnum.AR.getValue())),
                buildEvent(finalEventId, "RECRI003C", timestamp, businessTimestamp.plusSeconds(2), "REG123", "", null)
        ));

        // Act & Assert
       context.setEventId(finalEventId);
        StepVerifier.create(sequenceValidatorRir.execute(context))
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().contains("Registered letter codes do not match in sequence"))
                .verify();
    }

    @Test
    void validateSequenceValidRECRI004A() {
        // Arrange
        context.getPaperProgressStatusEvent().setStatusCode("RECRI004C");
        Instant timestamp = Instant.now();
        Instant businessTimestamp = Instant.now();
        String finalEventId = UUID.randomUUID().toString();
        context.setEventId(finalEventId);
        context.getPaperTrackings().setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECRI001", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI002", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI004A", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI004B", timestamp, businessTimestamp.plusSeconds(1), "REG123", "", List.of(DocumentTypeEnum.PLICO.getValue())),
                buildEvent(finalEventId, "RECRI004C", timestamp, businessTimestamp.plusSeconds(2), "REG123", "", null)
        ));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(sequenceValidatorRir.execute(context))
                .verifyComplete();

        // Assert
        verify(paperTrackingsDAO, times(1)).updateItem(any(), any());
    }

    @Test
    void validateSequenceValidRECRI004AWithFailureCause() {
        // Arrange
        context.getPaperProgressStatusEvent().setStatusCode("RECRI004C");
        Instant timestamp = Instant.now();
        Instant businessTimestamp = Instant.now();
        String finalEventId = UUID.randomUUID().toString();
        context.setEventId(finalEventId);
        context.getPaperTrackings().setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECRI001", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI002", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI004A", timestamp, businessTimestamp, "REG123", "M01", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI004B", timestamp, businessTimestamp.plusSeconds(1), "REG123", "", List.of(DocumentTypeEnum.PLICO.getValue())),
                buildEvent(finalEventId, "RECRI004C", timestamp, businessTimestamp.plusSeconds(2), "REG123", "", null)
        ));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.empty());

        // Act
        StepVerifier.create(sequenceValidatorRir.execute(context))
                .verifyComplete();

        // Assert
        verify(paperTrackingsDAO, times(1)).updateItem(any(), any());
    }

    @Test
    void validateSequenceValidRECRI004AWithInvalidFailureCause() {
        // Arrange
        context.getPaperProgressStatusEvent().setStatusCode("RECRI004C");
        Instant timestamp = Instant.now();
        Instant businessTimestamp = Instant.now();
        String finalEventId = UUID.randomUUID().toString();
        context.setEventId(finalEventId);
        context.getPaperTrackings().setEvents(List.of(
                buildEvent(UUID.randomUUID().toString(), "RECRI001", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI002", timestamp, businessTimestamp, "REG123", "", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI004A", timestamp, businessTimestamp, "REG123", "INVALID", null),
                buildEvent(UUID.randomUUID().toString(), "RECRI004B", timestamp, businessTimestamp.plusSeconds(1), "REG123", "", List.of(DocumentTypeEnum.PLICO.getValue())),
                buildEvent(finalEventId, "RECRI004C", timestamp, businessTimestamp.plusSeconds(2), "REG123", "", null)
        ));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.empty());

        // Act & Assert
        StepVerifier.create(sequenceValidatorRir.execute(context))
//                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
//                        throwable.getMessage().contains("Invalid deliveryFailureCause: INVALID"))
                .verifyComplete(); // Per ora la validazione del deliveryFailureCause per RIR non è strict
    }
    
    private Event buildEvent(String id, String statusCode, Instant statusTimestamp, Instant requestTimestamp, String registeredLetterCode, String deliveryFailureCause, List<String> attachmentTypes) {
        Event event = new Event();
        event.setAttachments(new ArrayList<>());
        event.setStatusCode(statusCode);
        event.setId(id);
        event.setRegisteredLetterCode(registeredLetterCode);
        event.setStatusTimestamp(statusTimestamp);
        event.setRequestTimestamp(requestTimestamp);
        event.setDeliveryFailureCause(deliveryFailureCause);

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