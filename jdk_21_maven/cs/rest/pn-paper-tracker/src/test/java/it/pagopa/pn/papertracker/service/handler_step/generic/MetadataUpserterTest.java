package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.DiscoveredAddress;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperStatus;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
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

import java.time.OffsetDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataUpserterTest {

    @Mock
    private PaperTrackingsDAO paperTrackingsDAO;

    @Mock
    private DataVaultClient dataVaultClient;

    @Mock
    private PnPaperTrackerConfigs PnPaperTrackerConfigs;

    @InjectMocks
    private MetadataUpserter metadataUpserter;

    private HandlerContext handlerContext;
    private PaperProgressStatusEvent paperProgressStatusEvent;
    private PaperTrackings paperTrackings;
    private String anonymizedDiscoveredAddressId;

    @BeforeEach
    void setUp() {
        anonymizedDiscoveredAddressId = "anonymized_addr_uuid";

        paperProgressStatusEvent = new PaperProgressStatusEvent();
        paperProgressStatusEvent.setRequestId("req-123");
        paperProgressStatusEvent.setStatusCode("RECRN002A");
        paperProgressStatusEvent.setStatusDateTime(OffsetDateTime.now());
        paperProgressStatusEvent.setClientRequestTimeStamp(OffsetDateTime.now());
        paperProgressStatusEvent.setProductType("AR");

        handlerContext = new HandlerContext();
        handlerContext.setPaperProgressStatusEvent(paperProgressStatusEvent);
        handlerContext.setReworkId("reworkId");

        paperTrackings = new PaperTrackings();
        paperTrackings.setTrackingId("req-123");
        paperTrackings.setProductType(ProductType.AR.getValue());
        PaperStatus paperStatus = new PaperStatus();
        paperStatus.setAnonymizedDiscoveredAddress("anonymized_addr_uuid");
        paperTrackings.setPaperStatus(paperStatus);
        paperTrackings.setEvents(new ArrayList<>());

        handlerContext.setPaperTrackings(paperTrackings);
    }

    @Test
    void execute_WithDiscoveredAddress_SetsAnonimizedDiscoveredAddress() {
        // Arrange
        paperProgressStatusEvent.setDiscoveredAddress(new DiscoveredAddress());
        when(dataVaultClient.anonymizeDiscoveredAddress(any(), any())).thenReturn(Mono.just(anonymizedDiscoveredAddressId));

        when(paperTrackingsDAO.updateItem(any(), any(PaperTrackings.class)))
                .thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(metadataUpserter.execute(handlerContext))
                .verifyComplete();

        // Assert
        assertEquals(anonymizedDiscoveredAddressId, handlerContext.getPaperTrackings().getPaperStatus().getAnonymizedDiscoveredAddress());
        verify(paperTrackingsDAO).updateItem(eq("req-123"), any());

    }

    @Test
    void execute_WithNullDiscoveredAddress_DoesNotSetAnonimizedAddress() {
        // Arrange
        paperProgressStatusEvent.setDiscoveredAddress(null);
        paperTrackings.getPaperStatus().setAnonymizedDiscoveredAddress(null);

        when(paperTrackingsDAO.updateItem(eq("req-123"), any(PaperTrackings.class)))
                .thenReturn(Mono.just(paperTrackings));

        // Act
        StepVerifier.create(metadataUpserter.execute(handlerContext))
                .verifyComplete();

        // Assert
        assertNull(handlerContext.getPaperTrackings().getPaperStatus().getAnonymizedDiscoveredAddress());
        verify(paperTrackingsDAO).updateItem(eq("req-123"), any());

    }

    @Test
    void execute_WithPrinterAndDu_MapsFieldsIntoEvent() {
        // Arrange
        paperProgressStatusEvent.setPrinter("printer-123");
        paperProgressStatusEvent.setDu("du-456");
        handlerContext.setEventId("event-id-1");

        when(paperTrackingsDAO.updateItem(eq("req-123"), any(PaperTrackings.class)))
                .thenAnswer(invocation -> {
                    PaperTrackings updated = invocation.getArgument(1);
                    paperTrackings.setEvents(updated.getEvents());
                    return Mono.just(paperTrackings);
                });

        // Act
        StepVerifier.create(metadataUpserter.execute(handlerContext))
                .verifyComplete();

        // Assert
        assertEquals("printer-123", handlerContext.getPaperTrackings().getEvents().get(0).getPrinter());
        assertEquals("du-456", handlerContext.getPaperTrackings().getEvents().get(0).getDu());
    }
}