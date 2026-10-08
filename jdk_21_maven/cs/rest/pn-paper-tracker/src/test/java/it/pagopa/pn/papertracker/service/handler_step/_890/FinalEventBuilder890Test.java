package it.pagopa.pn.papertracker.service.handler_step._890;

import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.StatusCodeEnum;
import it.pagopa.pn.papertracker.generated.openapi.msclient.pndatavault.model.PaperAddress;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.*;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.model.HandlerContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinalEventBuilder890Test {

    @Mock
    private DataVaultClient dataVaultClient;

    @Mock
    private PaperTrackingsDAO paperTrackingsDAO;

    private HandlerContext context;

    private PaperTrackings paperTrackings;

    private FinalEventBuilder890 finalEventBuilder890;

    private static final String EVENT_ID = UUID.randomUUID().toString();
    private Event event = new Event();
    private Event event1 = new Event();

    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        context = new HandlerContext();
        context.setTrackingId("req-123");
        context.setEventId(EVENT_ID);
        finalEventBuilder890 = new FinalEventBuilder890(dataVaultClient, paperTrackingsDAO);
        paperTrackings = new PaperTrackings();
        paperTrackings.setTrackingId("req-123");
        paperTrackings.setProductType(ProductType._890.getValue());
        paperTrackings.setPaperStatus(new PaperStatus());
        paperTrackings.getPaperStatus().setRegisteredLetterCode("RL123");

        event.setStatusTimestamp(now);
        event.setRequestTimestamp(now);
        event.setId(EVENT_ID);

        event1.setStatusTimestamp(now);
        event1.setRequestTimestamp(now);
        event1.setId(EVENT_ID + "1");

        context.setPaperTrackings(paperTrackings);
    }

    @Test
    void executeWithStockStatus() {
        context.setEventId(EVENT_ID);
        event.setStatusCode(RECAG005C.name());
        event1.setStatusCode(RECAG005A.name());
        paperTrackings.setEvents(List.of(event, event1));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));
        StepVerifier.create(finalEventBuilder890.execute(context))
                .verifyComplete();

        // Assert
        assertEquals(1, context.getEventsToSend().size());
        assertEquals(RECAG005C.name(), context.getEventsToSend().getFirst().getStatusDetail());
        assertNull(context.getEventsToSend().getFirst().getDeliveryFailureCause());
        assertNull(context.getEventsToSend().getFirst().getDiscoveredAddress());
        assertEquals(StatusCodeEnum.PROGRESS, context.getEventsToSend().getFirst().getStatusCode());
    }

    @Test
    void executeWithoutStockStatus2COK() {
        context.setEventId(EVENT_ID);
        event.setStatusCode(RECAG002C.name());
        event1.setStatusCode(RECAG002A.name());
        paperTrackings.setEvents(List.of(event, event1));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));
        StepVerifier.create(finalEventBuilder890.execute(context))
                .verifyComplete();

        // Assert
        assertEquals(1, context.getEventsToSend().size());
        assertEquals(RECAG002C.name(), context.getEventsToSend().getFirst().getStatusDetail());
        assertNull(context.getEventsToSend().getFirst().getDeliveryFailureCause());
        assertNull(context.getEventsToSend().getFirst().getDiscoveredAddress());
        assertEquals(StatusCodeEnum.OK, context.getEventsToSend().getFirst().getStatusCode());
    }


    @Test
    void executeWithoutStockStatus3COK() {
        PaperAddress paperAddress = new PaperAddress();
        paperAddress.setCity("city");
        paperAddress.setName("name");
        paperAddress.setAddress("address");
        when(dataVaultClient.deAnonymizeDiscoveredAddress(any(), any()))
                .thenReturn(Mono.just(paperAddress));
        context.setEventId(EVENT_ID);
        event.setStatusCode(RECAG003C.name());
        event1.setStatusCode(RECAG003A.name());
        paperTrackings.getPaperStatus().setDeliveryFailureCause("M02");
        paperTrackings.getPaperStatus().setAnonymizedDiscoveredAddress("discAddr");
        paperTrackings.setEvents(List.of(event, event1));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));
        StepVerifier.create(finalEventBuilder890.execute(context))
                .verifyComplete();

        // Assert
        assertEquals(1, context.getEventsToSend().size());
        assertEquals(RECAG003C.name(), context.getEventsToSend().getFirst().getStatusDetail());
        assertNotNull(context.getEventsToSend().getFirst().getDeliveryFailureCause());
        assertNotNull(context.getEventsToSend().getFirst().getDiscoveredAddress());
        assertEquals(StatusCodeEnum.OK, context.getEventsToSend().getFirst().getStatusCode());
    }

    @Test
    void executeWithoutStockStatus3CKO() {
        PaperAddress paperAddress = new PaperAddress();
        paperAddress.setCity("city");
        paperAddress.setName("name");
        paperAddress.setAddress("address");
        when(dataVaultClient.deAnonymizeDiscoveredAddress(any(), any()))
                .thenReturn(Mono.just(paperAddress));
        context.setEventId(EVENT_ID);
        event.setStatusCode(RECAG003C.name());
        event1.setStatusCode(RECAG003A.name());
        paperTrackings.getPaperStatus().setDeliveryFailureCause("M06");
        paperTrackings.getPaperStatus().setAnonymizedDiscoveredAddress("discAddr");
        paperTrackings.setEvents(List.of(event, event1));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));
        StepVerifier.create(finalEventBuilder890.execute(context))
                .verifyComplete();

        // Assert
        assertEquals(1, context.getEventsToSend().size());
        assertEquals(RECAG003C.name(), context.getEventsToSend().getFirst().getStatusDetail());
        assertNotNull(context.getEventsToSend().getFirst().getDeliveryFailureCause());
        assertNotNull(context.getEventsToSend().getFirst().getDiscoveredAddress());
        assertEquals(StatusCodeEnum.KO, context.getEventsToSend().getFirst().getStatusCode());
    }

    @Test
    void executeWithoutStockStatusF() {
        PaperAddress paperAddress = new PaperAddress();
        paperAddress.setCity("city");
        paperAddress.setName("name");
        paperAddress.setAddress("address");
        when(dataVaultClient.deAnonymizeDiscoveredAddress(any(), any()))
                .thenReturn(Mono.just(paperAddress));
        context.setEventId(EVENT_ID);
        event.setStatusCode(RECAG003F.name());
        event1.setStatusCode(RECAG003D.name());
        paperTrackings.getPaperStatus().setDeliveryFailureCause("M01");
        paperTrackings.getPaperStatus().setAnonymizedDiscoveredAddress("discAddr");
        paperTrackings.setEvents(List.of(event, event1));
        when(paperTrackingsDAO.updateItem(any(), any())).thenReturn(Mono.just(paperTrackings));
        StepVerifier.create(finalEventBuilder890.execute(context))
                .verifyComplete();

        // Assert
        assertEquals(1, context.getEventsToSend().size());
        assertEquals(RECAG003F.name(), context.getEventsToSend().getFirst().getStatusDetail());
        assertNotNull(context.getEventsToSend().getFirst().getDeliveryFailureCause());
        assertNotNull(context.getEventsToSend().getFirst().getDiscoveredAddress());
        assertEquals(StatusCodeEnum.KO, context.getEventsToSend().getFirst().getStatusCode());
    }

    @Test
    void executeWithRECAG012event() {
        context.setEventId(EVENT_ID);
        event.setStatusCode(RECAG012.name());
        paperTrackings.getPaperStatus().setDeliveryFailureCause("M01");
        paperTrackings.getPaperStatus().setAnonymizedDiscoveredAddress("discAddr");
        paperTrackings.setEvents(List.of(event, event1));
        StepVerifier.create(finalEventBuilder890.execute(context))
                .verifyComplete();

        // Assert
        assertEquals(0, context.getEventsToSend().size());
    }
}
