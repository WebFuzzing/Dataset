package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerValidationException;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.model.sequence.SequenceConfig;
import it.pagopa.pn.papertracker.model.sequence.SequenceConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class GenericSequenceValidatorTest {

    @Mock
    PaperTrackingsDAO paperTrackingsDAO;

    @Mock
    PaperTrackerErrorService paperTrackerErrorService;

    GenericSequenceValidator validator;

    HandlerContext context;
    PaperTrackings paperTrackings;
    List<Event> events;
    SequenceConfig config;

    @BeforeEach
    void setUp() {
        validator = new GenericSequenceValidator(paperTrackingsDAO, paperTrackerErrorService) {
        };
        context = new HandlerContext();
        context.setTrackingId("track-1");
        context.setEventId("event-1");
        context.setPaperProgressStatusEvent(new it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent());
        paperTrackings = new PaperTrackings();
        paperTrackings.setTrackingId("track-1");
        paperTrackings.setPaperStatus(new PaperStatus());
        paperTrackings.setValidationFlow(new ValidationFlow());
        paperTrackings.setValidationConfig(new ValidationConfig());

        Instant statusTimestamp = Instant.now();
        Event e1 = new Event();
        e1.setId("e1");
        e1.setStatusCode("RECRN002A");
        e1.setStatusTimestamp(statusTimestamp);
        e1.setRequestTimestamp(Instant.now());
        e1.setRegisteredLetterCode("RL1");
        e1.setDeliveryFailureCause("M08");
        e1.setAnonymizedDiscoveredAddressId("anon-addr-1");

        Event e2 = new Event();
        e2.setId("e2");
        e2.setStatusCode("RECRN002B");
        e2.setStatusTimestamp(statusTimestamp);
        e2.setRequestTimestamp(Instant.now());
        e2.setRegisteredLetterCode("RL1");
        e2.setDeliveryFailureCause("M08");

        events = List.of(e1, e2);

        config = SequenceConfiguration.getConfig("RECRN002C");
    }

    @Test
    void testValidatePresenceOfStatusCodes() {
        Set<String> required = new HashSet<>(Arrays.asList("RECRN002A", "RECRN002B"));
        Mono<List<Event>> result = ReflectionTestUtils.invokeMethod(
                validator, "validatePresenceOfStatusCodes", events, paperTrackings, context, required, true
        );
        StepVerifier.create(result)
                .expectNext(events)
                .verifyComplete();
    }

    @Test
    void testValidatePresenceOfStatusCodes_MissingStatusCode() {
        Set<String> required = new HashSet<>(Arrays.asList("RECRN002A", "RECRN002C"));
        Mono<List<Event>> result = ReflectionTestUtils.invokeMethod(
                validator, "validatePresenceOfStatusCodes", events, paperTrackings, context, required, true
        );
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().equals("Necessary status code not found in events: [RECRN002C]"))
                .verify();
    }

    @Test
    void testValidateBusinessTimestamps() {
        PaperTrackings toUpdate = new PaperTrackings();
        toUpdate.setPaperStatus(new PaperStatus());
        Mono<List<Event>> result = ReflectionTestUtils.invokeMethod(
                validator, "validateBusinessTimestamps", events, paperTrackings, context, config, true, toUpdate
        );
        StepVerifier.create(result)
                .expectNext(events)
                .verifyComplete();
    }

    @Test
    void testValidateBusinessTimestamps_InvalidTimestamps() {
        events.getFirst().setStatusTimestamp(Instant.now().plusSeconds(1000));
        PaperTrackings toUpdate = new PaperTrackings();
        Mono<List<Event>> result = ReflectionTestUtils.invokeMethod(
                validator, "validateBusinessTimestamps", events, paperTrackings, context, config, true, toUpdate
        );
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().equals("Invalid business timestamps"))
                .verify();
    }

    @Test
    void testValidateAttachments_MissingAttachment() {
        Mono<List<Event>> result = ReflectionTestUtils.invokeMethod(
                validator, "validateAttachments", events, paperTrackings, context, config.validAttachments(), config.requiredAttachments(), true
        );
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().equals("Missed required attachments for the sequence validation: [Plico]"))
                .verify();
    }

    @Test
    void testValidateRegisteredLetterCode() {
        PaperTrackings toUpdate = new PaperTrackings();
        toUpdate.setPaperStatus(new PaperStatus());
        Mono<List<Event>> result = ReflectionTestUtils.invokeMethod(
                validator, "validateRegisteredLetterCode", events, paperTrackings, toUpdate, context, true
        );
        StepVerifier.create(result)
                .expectNext(events)
                .verifyComplete();
        assertEquals("RL1", toUpdate.getPaperStatus().getRegisteredLetterCode());
    }

    @Test
    void testValidateRegisteredLetterCode_MismatchedCodes() {
        events.getFirst().setRegisteredLetterCode("RL2");
        PaperTrackings toUpdate = new PaperTrackings();
        toUpdate.setPaperStatus(new PaperStatus());
        Mono<List<Event>> result = ReflectionTestUtils.invokeMethod(
                validator, "validateRegisteredLetterCode", events, paperTrackings, toUpdate, context, true
        );
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().equals("Registered letter codes do not match in sequence: [RL2, RL1]"))
                .verify();
    }

    @Test
    void testValidateDeliveryFailureCause() {
        Mono<List<Event>> result = ReflectionTestUtils.invokeMethod(
                validator, "validateDeliveryFailureCause", events, paperTrackings, context, true
        );
        StepVerifier.create(result)
                .expectNext(events)
                .verifyComplete();
    }

    @Test
    void testValidateDeliveryFailureCause_MismatchedCauses() {
        events.getFirst().setDeliveryFailureCause("F01");
        Mono<List<Event>> result = ReflectionTestUtils.invokeMethod(
                validator, "validateDeliveryFailureCause", events, paperTrackings, context, true
        );
        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerValidationException &&
                        throwable.getMessage().equals("Invalid deliveryFailureCause: F01"))
                .verify();
    }

    @Test
    void testEnrichPaperTrackingToUpdateWithAddressAndFailureCause() {
        PaperTrackings toUpdate = new PaperTrackings();
        toUpdate.setPaperStatus(new PaperStatus());
        Mono<List<Event>> result = ReflectionTestUtils.invokeMethod(
                validator, "enrichPaperTrackingToUpdateWithAddressAndFailureCause", events, toUpdate, "RECRN002C"
        );
        StepVerifier.create(result)
                .expectNext(events)
                .verifyComplete();
        assertEquals("anon-addr-1", toUpdate.getPaperStatus().getAnonymizedDiscoveredAddress());
        assertEquals("M08", toUpdate.getPaperStatus().getDeliveryFailureCause());
    }
}
