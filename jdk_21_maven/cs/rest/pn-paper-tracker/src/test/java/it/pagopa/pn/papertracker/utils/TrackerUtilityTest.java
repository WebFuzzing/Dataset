package it.pagopa.pn.papertracker.utils;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Event;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TrackerUtilityTest {

    @Test
    void checkIfIsFinalDematReturnsTrueForFinalDematStatusCode() {
        String eventStatusCode = "RECRN001B";
        Assertions.assertTrue(TrackerUtility.checkIfIsFinalDemat(eventStatusCode));
    }

    @Test
    void checkIfIsFinalDematReturnsFalseForNonFinalDematStatusCode() {
        String eventStatusCode = "RECRN002A";
        Assertions.assertFalse(TrackerUtility.checkIfIsFinalDemat(eventStatusCode));
    }

    @Test
    void checkIfIsP000eventReturnsTrueForP000StatusCode() {
        String eventStatusCode = "P000";
        Assertions.assertTrue(TrackerUtility.checkIfIsP000event(eventStatusCode));
    }

    @Test
    void checkIfIsP000eventReturnsFalseForNonP000StatusCode() {
        String eventStatusCode = "P001";
        Assertions.assertFalse(TrackerUtility.checkIfIsP000event(eventStatusCode));
    }

    @Test
    void checkIfIsInternalEventReturnsTrueForInternalEventStatusCodes() {
        List<String> internalEvents = List.of("P000", "P011", "P012", "P013");

        Assertions.assertTrue(TrackerUtility.checkIfIsInternalEvent(internalEvents, "P000"));
        Assertions.assertTrue(TrackerUtility.checkIfIsInternalEvent(internalEvents, "P011"));
        Assertions.assertTrue(TrackerUtility.checkIfIsInternalEvent(internalEvents, "P012"));
        Assertions.assertTrue(TrackerUtility.checkIfIsInternalEvent(internalEvents, "P013"));
    }

    @Test
    void buildOcrRequestIdReturnsCorrectFormat() {
        String trackingId = "trackingId123";
        String eventId = "eventId456";
        String documentType = "CAD";
        String result = TrackerUtility.buildOcrRequestId(trackingId, eventId, documentType);
        Assertions.assertEquals("trackingId123#eventId456#CAD", result);
    }


    @Test
    void validatedEventsReturnsMatchingEvents() {
        List<String> eventsIds = List.of("eventId1", "eventId2");
        Event event1 = new Event();
        event1.setId("eventId1");
        Event event2 = new Event();
        event2.setId("eventId2");
        Event event3 = new Event();
        event3.setId("eventId3");
        List<Event> events = List.of(event1, event2, event3);

        List<Event> result = TrackerUtility.validatedEvents(eventsIds, events);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().anyMatch(event -> "eventId1".equals(event.getId())));
        Assertions.assertTrue(result.stream().anyMatch(event -> "eventId2".equals(event.getId())));
    }


    @Test
    void validatedEventsReturnsMatchingEventsDuplicatedEvent() {
        List<String> eventsIds = List.of("eventId1", "eventId2");
        Event event1 = new Event();
        event1.setId("eventId1");
        event1.setCreatedAt(Instant.now());
        event1.setStatusCode("OLD");
        Event event2 = new Event();
        event2.setId("eventId2");
        Event event3 = new Event();
        event3.setId("eventId3");
        Event event4 = new Event();
        event4.setId("eventId1");
        event4.setCreatedAt(Instant.now().plus(10, ChronoUnit.DAYS));
        event4.setStatusCode("NEW");
        List<Event> events = List.of(event1, event2, event3, event4);

        List<Event> result = TrackerUtility.validatedEvents(eventsIds, events);

        Assertions.assertEquals(2, result.size());
        Assertions.assertTrue(result.stream().anyMatch(event -> "eventId1".equals(event.getId())));
        Assertions.assertTrue(result.stream().anyMatch(event -> "eventId1".equals(event.getId())));
        Assertions.assertTrue(result.stream().anyMatch(event -> "NEW".equals(event.getStatusCode())));
        Assertions.assertTrue(result.stream().noneMatch(event -> "OLD".equals(event.getStatusCode())));
    }

    @Test
    void checkIfIsRedriveReturnsTrueWhenSenderIdMatchesDomain() {
        String senderId = "ABCDEF:nome.cognome@pagopa.it";
        List<String> redriveEnabledDomains = List.of("@pagopa.it", "@external.pagopa.it");

        Assertions.assertTrue(TrackerUtility.checkIfIsRedrive(senderId, redriveEnabledDomains));
    }

    @Test
    void checkIfIsRedriveReturnsFalseWhenSenderIdDoesNotMatchDomain() {
        String senderId = "ABCDEF:nome.cognome@anotherdomain.com";
        List<String> redriveEnabledDomains = List.of("@pagopa.it", "@external.pagopa.it");

        Assertions.assertFalse(TrackerUtility.checkIfIsRedrive(senderId, redriveEnabledDomains));
    }

    @Test
    void checkIfIsRedriveReturnsFalseWhenRedriveEnabledDomainsIsEmpty() {
        String senderId = "example@domain.com";
        List<String> redriveEnabledDomains = List.of();

        Assertions.assertFalse(TrackerUtility.checkIfIsRedrive(senderId, redriveEnabledDomains));
    }

    @Test
    void checkIfIsRedriveReturnsFalseWhenSenderIdIsNull() {
        List<String> redriveEnabledDomains = List.of("@pagopa.it", "@external.pagopa.it");

        Assertions.assertFalse(TrackerUtility.checkIfIsRedrive(null, redriveEnabledDomains));
    }

}
