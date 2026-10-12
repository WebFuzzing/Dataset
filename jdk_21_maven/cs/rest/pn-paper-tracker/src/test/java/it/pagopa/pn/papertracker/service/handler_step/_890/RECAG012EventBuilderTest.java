package it.pagopa.pn.papertracker.service.handler_step._890;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.SendEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.FileType;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.model.OcrStatusEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RECAG012EventBuilder Tests")
class RECAG012EventBuilderTest {

    @Mock
    private PaperTrackingsDAO paperTrackingsDAO;

    @InjectMocks
    private RECAG012EventBuilder recag012EventBuilder;

    private HandlerContext context;
    private PaperTrackings paperTrackings;
    private ValidationConfig validationConfig;
    private ValidationFlow validationFlow;
    private List<SendEvent> eventsToSend;

    @BeforeEach
    void setUp() {
        // Common setup
        eventsToSend = new ArrayList<>();
        context = new HandlerContext();
        context.setTrackingId("TEST_TRACKING_ID");
        context.setEventId("TEST_EVENT_ID");
        context.setEventsToSend(eventsToSend);

        paperTrackings = new PaperTrackings();
        validationConfig = new ValidationConfig();
        validationFlow = new ValidationFlow();

        validationFlow.setOcrRequests(new ArrayList<>());

        paperTrackings.setValidationConfig(validationConfig);
        paperTrackings.setValidationFlow(validationFlow);
        context.setPaperTrackings(paperTrackings);
    }

    @Nested
    class SkipScenarios {

        @Test
        void shouldSkipWhenStateIsDone() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.DONE);
            addEventToPaperTrackings("RECAG011B");

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isEmpty();
            verifyNoInteractions(paperTrackingsDAO);
        }

        @Test
        void shouldSkipWhenEventIsFinalEventType() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_REFINEMENT);
            addEventToPaperTrackings("RECAG005C"); // FINAL_EVENT

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isEmpty();
            verifyNoInteractions(paperTrackingsDAO);
        }

        @Test
        void shouldSkipWhenRecag012EventNotFound() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_REFINEMENT);
            context.setNeedToSendRECAG012A(false);
            addEventToPaperTrackings("RECAG011B");
            // No RECAG012 event added

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isEmpty();
            verifyNoInteractions(paperTrackingsDAO);
        }
    }

    @Nested
    class RECAG012AEventBuilding {

        @Test
        void shouldBuildRECAG012AWhenAttachmentsMissingButRECAG012Received() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_REFINEMENT);
            context.setNeedToSendRECAG012A(true);
            addEventToPaperTrackings("RECAG012");

            PaperProgressStatusEvent progressEvent = new PaperProgressStatusEvent();
            progressEvent.setStatusCode("RECAG012");
            progressEvent.setRequestId("TEST_REQUEST_ID");
            context.setPaperProgressStatusEvent(progressEvent);

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isNotEmpty();
            assertThat(eventsToSend.get(0).getStatusCode()).isEqualTo(StatusCodeEnum.PROGRESS);
            assertThat(eventsToSend.get(0).getStatusDetail()).isEqualTo("RECAG012A");
            verifyNoInteractions(paperTrackingsDAO);
        }

        @Test
        void shouldNotBuildRECAG012AWhenFlagIsFalse() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_REFINEMENT);
            context.setNeedToSendRECAG012A(false);
            addEventToPaperTrackings("RECAG012");
            addRECAG012Event();

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
        }

        @Test
        void shouldNotBuildRECAG012AWhenStatusCodeIsNotRECAG012() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_REFINEMENT);
            context.setNeedToSendRECAG012A(true);
            addEventToPaperTrackings("RECAG011B");

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isEmpty();
        }
    }

    @Nested
    class OcrDisabledOrDryMode {

        @Test
        void shouldBuildFeedbackWhenOcrDisabled() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_REFINEMENT);
            context.setNeedToSendRECAG012A(false);
            validationConfig.setOcrEnabled(OcrStatusEnum.DISABLED);
            addEventToPaperTrackings("RECAG011B");
            addRECAG012Event();

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isNotEmpty();
            assertThat(eventsToSend.get(0).getStatusCode()).isEqualTo(StatusCodeEnum.OK);
            assertThat(eventsToSend.get(0).getStatusDetail()).isEqualTo("RECAG012");
        }

        @Test
        void shouldBuildFeedbackWhenOcrDry() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_OCR);
            context.setNeedToSendRECAG012A(false);
            validationConfig.setOcrEnabled(OcrStatusEnum.DRY);
            addEventToPaperTrackings("RECAG011B");
            addRECAG012Event();

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isNotEmpty();
        }
    }

    @Nested
    class OcrRunModeEmptyRequests {

        @Test
        void shouldNotBuildFeedbackWhenNoOcrRequestsMatchRequiredDocs() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_OCR);
            context.setNeedToSendRECAG012A(false);
            validationConfig.setOcrEnabled(OcrStatusEnum.RUN);
            validationConfig.setRequiredAttachmentsRefinementStock890(Arrays.asList("DOC_TYPE_A", "DOC_TYPE_B"));
            validationConfig.setSendOcrAttachmentsRefinementStock890(List.of("DOC_TYPE_A"));
            validationConfig.setOcrFileTypes(List.of(FileType.PDF.getValue()));

            OcrRequest ocrRequest = new OcrRequest();
            ocrRequest.setDocumentType("DOC_TYPE_C"); // Different type
            validationFlow.setOcrRequests(Collections.singletonList(ocrRequest));

            addEventToPaperTrackings("RECAG011B");
            addRECAG012Event();

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isEmpty();
        }

        @Test
        void shouldBuildFeedbackWhenOcrRequestsListIsEmpty() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_REFINEMENT);
            context.setNeedToSendRECAG012A(false);
            validationConfig.setOcrEnabled(OcrStatusEnum.RUN);
            validationConfig.setRequiredAttachmentsRefinementStock890(List.of());
            validationFlow.setOcrRequests(Collections.emptyList());
            context.setEventId("RECAG012_EVENT_ID");

            addEventToPaperTrackings("RECAG011B");
            addRECAG012Event();

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isNotEmpty();
        }
    }

    @Nested
    class OcrRunModeWithRequests {

        @Test
        void shouldBuildFeedbackWhenAllOcrResponsesReceived() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_OCR);
            context.setNeedToSendRECAG012A(false);
            validationConfig.setOcrEnabled(OcrStatusEnum.RUN);
            validationConfig.setRequiredAttachmentsRefinementStock890(Arrays.asList("DOC_TYPE_A", "DOC_TYPE_B"));

            OcrRequest request1 = new OcrRequest();
            request1.setDocumentType("DOC_TYPE_A");
            request1.setResponseTimestamp(Instant.now());

            OcrRequest request2 = new OcrRequest();
            request2.setDocumentType("DOC_TYPE_B");
            request2.setResponseTimestamp(Instant.now());

            validationFlow.setOcrRequests(Arrays.asList(request1, request2));

            addEventToPaperTrackings("RECAG011B");
            addRECAG012Event();
            context.setEventId("RECAG012_EVENT_ID");

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isNotEmpty();
        }

        @Test
        void shouldWaitWhenNotAllOcrResponsesReceived() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_OCR);
            context.setNeedToSendRECAG012A(false);
            validationConfig.setOcrEnabled(OcrStatusEnum.RUN);
            validationConfig.setRequiredAttachmentsRefinementStock890(Arrays.asList("DOC_TYPE_A", "DOC_TYPE_B"));
            validationConfig.setSendOcrAttachmentsRefinementStock890(Arrays.asList("DOC_TYPE_A", "DOC_TYPE_B"));
            validationConfig.setOcrFileTypes(List.of(FileType.PDF.getValue()));

            OcrRequest request1 = new OcrRequest();
            request1.setDocumentType("DOC_TYPE_A");
            request1.setUri("DOC_TYPE_A.pdf");
            request1.setResponseTimestamp(Instant.now());

            OcrRequest request2 = new OcrRequest();
            request2.setDocumentType("DOC_TYPE_B");
            request2.setUri("DOC_TYPE_B.pdf");
            request2.setResponseTimestamp(null); // Missing response

            validationFlow.setOcrRequests(Arrays.asList(request1, request2));

            addEventToPaperTrackings("RECAG011B");
            addRECAG012Event();

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isEmpty();
            verifyNoInteractions(paperTrackingsDAO);
        }

        @Test
        void shouldWaitWhenSomeResponsesAreForDifferentDocTypes() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_OCR);
            context.setNeedToSendRECAG012A(false);
            validationConfig.setOcrEnabled(OcrStatusEnum.RUN);
            validationConfig.setRequiredAttachmentsRefinementStock890(List.of("DOC_TYPE_A", "DOC_TYPE_B"));
            validationConfig.setSendOcrAttachmentsRefinementStock890(List.of("DOC_TYPE_A", "DOC_TYPE_B"));
            validationConfig.setOcrFileTypes(List.of(FileType.PDF.getValue()));

            OcrRequest request1 = new OcrRequest();
            request1.setDocumentType("DOC_TYPE_A");
            request1.setUri("DOC_TYPE_A.pdf");
            request1.setResponseTimestamp(Instant.now());

            OcrRequest request2 = new OcrRequest();
            request2.setDocumentType("DOC_TYPE_C"); // Wrong type
            request2.setUri("DOC_TYPE_C.pdf");
            request2.setResponseTimestamp(Instant.now());

            validationFlow.setOcrRequests(Arrays.asList(request1, request2));

            addEventToPaperTrackings("RECAG011B");
            addRECAG012Event();

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();

            assertThat(eventsToSend).isEmpty();
            verifyNoInteractions(paperTrackingsDAO);
        }

        @Test
        void shouldHandleEmptyRequiredDocsList() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_OCR);
            context.setNeedToSendRECAG012A(false);
            validationConfig.setOcrEnabled(OcrStatusEnum.RUN);
            validationConfig.setRequiredAttachmentsRefinementStock890(Collections.emptyList());

            OcrRequest request = new OcrRequest();
            request.setDocumentType("DOC_TYPE_A");
            request.setResponseTimestamp(Instant.now());
            validationFlow.setOcrRequests(Collections.singletonList(request));

            addEventToPaperTrackings("RECAG011B");
            addRECAG012Event();
            context.setEventId("RECAG012_EVENT_ID");

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    @Nested
    class EdgeCases {

        @Test
        void shouldNotUpdateWhenEventsListIsEmpty() {
            // Arrange
            paperTrackings.setState(PaperTrackingsState.AWAITING_REFINEMENT);
            context.setNeedToSendRECAG012A(false);
            validationConfig.setOcrEnabled(OcrStatusEnum.DISABLED);
            addEventToPaperTrackings("RECAG011B");
            addRECAG012Event();

            // Act
            Mono<Void> result = recag012EventBuilder.execute(context);

            // Assert
            StepVerifier.create(result)
                    .verifyComplete();
        }
    }

    // Helper methods
    private void addEventToPaperTrackings(String statusCode) {
        Event event = new Event();
        event.setId("TEST_EVENT_ID");
        event.setStatusCode(statusCode);
        event.setStatusTimestamp(Instant.now());
        event.setRequestTimestamp(Instant.now());

        if (paperTrackings.getEvents() == null) {
            paperTrackings.setEvents(new ArrayList<>());
        }
        paperTrackings.getEvents().add(event);
    }

    private void addRECAG012Event() {
        Event recag012Event = new Event();
        recag012Event.setId("RECAG012_EVENT_ID");
        recag012Event.setStatusCode("RECAG012");
        recag012Event.setStatusTimestamp(Instant.now());
        recag012Event.setRequestTimestamp(Instant.now());

        if (paperTrackings.getEvents() == null) {
            paperTrackings.setEvents(new ArrayList<>());
        }
        paperTrackings.getEvents().add(recag012Event);
    }
}