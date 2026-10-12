package it.pagopa.pn.papertracker.service.impl;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerBadRequestException;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.SequenceResponse;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsState;
import it.pagopa.pn.papertracker.model.sequence.SequenceConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationReworkServiceImplTest {

    @InjectMocks
    private NotificationReworkServiceImpl service;

    @Mock
    private PaperTrackingsDAO paperTrackingsDAO;

    @Test
    void retrieveSequenceAndEventStatus_InvalidProductType() {
        String statusCode = "RECRN005C";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, null,"890"))
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof PnPaperTrackerBadRequestException ex) {
                        assertNotNull(ex.getProblem().getDetail());
                        return ex.getProblem().getDetail().contains("statusCode RECRN005C is invalid");
                    }
                    return false;
                });
    }

    @Test
    void retrieveSequenceAndEventStatus_InvalidRIR() {
        String statusCode = "RECRI003C";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, null,"AR"))
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof PnPaperTrackerBadRequestException ex) {
                        assertNotNull(ex.getProblem().getDetail());
                        return ex.getProblem().getDetail().contains("statusCode RECRN005C is invalid");
                    }
                    return false;
                });
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldReturnSequenceForValidStatusCode() {
        String statusCode = "RECRN005C";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, null,"AR"))
                .assertNext(response -> {
                    assertEquals(SequenceResponse.FinalStatusCodeEnum.OK, response.getFinalStatusCode());
                    assertNotNull(response.getSequence());
                    assertEquals(
                            SequenceConfiguration.getConfig(statusCode).sequenceStatusCodes().size(),
                            response.getSequence().size()
                    );
                    response.getSequence().forEach(sequenceItem -> {
                        if(!CollectionUtils.isEmpty(sequenceItem.getAttachments())) {
                            assertTrue(sequenceItem.getAttachments().containsAll(SequenceConfiguration.getConfig(statusCode).validAttachments().get(sequenceItem.getStatusCode())));
                        }
                    });
                    response.getSequence().forEach(item -> assertTrue(StringUtils.hasText(item.getStatusCode())));
                })
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldReturnSequenceForStock890() {
        String statusCode = "RECAG005C";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, null,"890"))
                .assertNext(response -> {
                    assertEquals(SequenceResponse.FinalStatusCodeEnum.OK, response.getFinalStatusCode());
                    assertNotNull(response.getSequence());
                    assertEquals(
                            SequenceConfiguration.getConfig(statusCode).sequenceStatusCodes().size(),
                            response.getSequence().size()
                    );
                    response.getSequence().forEach(sequenceItem -> {
                        if(!CollectionUtils.isEmpty(sequenceItem.getAttachments())) {
                            assertTrue(sequenceItem.getAttachments().containsAll(SequenceConfiguration.getConfig(statusCode).validAttachments().get(sequenceItem.getStatusCode())));
                        }
                    });
                    response.getSequence().forEach(item -> assertTrue(StringUtils.hasText(item.getStatusCode())));
                })
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldReturnSequenceForKOStatusCode() {
        String statusCode = "RECRN002F";
        String deliveryFailureCause = "M01";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause,"AR"))
                .assertNext(response -> {
                    assertEquals(SequenceResponse.FinalStatusCodeEnum.KO, response.getFinalStatusCode());
                    assertNotNull(response.getSequence());
                    assertEquals(
                            SequenceConfiguration.getConfig(statusCode).sequenceStatusCodes().size(),
                            response.getSequence().size()
                    );
                    response.getSequence().forEach(sequenceItem -> {
                        if(!CollectionUtils.isEmpty(sequenceItem.getAttachments())) {
                            assertTrue(sequenceItem.getAttachments().containsAll(SequenceConfiguration.getConfig(statusCode).validAttachments().get(sequenceItem.getStatusCode())));
                        }
                    });
                })
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldReturnSequenceForKOStatusCodeWithoutDeliveryFailureCause() {
        String statusCode = "RECRN002F";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, null,"AR"))
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof PnPaperTrackerBadRequestException ex) {
                        assertNotNull(ex.getProblem().getDetail());
                        return Objects.requireNonNull(ex.getProblem().getErrors().getFirst().getDetail()).equalsIgnoreCase("deliveryFailureCause null is invalid");
                    }
                    return false;
                })
                .verify();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldReturnOKForRECRN002CWithM02() {
        String statusCode = "RECRN002C";
        String deliveryFailureCause = "M02";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause,"AR"))
                .assertNext(response -> {
                    assertEquals(SequenceResponse.FinalStatusCodeEnum.OK, response.getFinalStatusCode());
                    assertNotNull(response.getSequence());
                    assertEquals(
                            SequenceConfiguration.getConfig(statusCode).sequenceStatusCodes().size(),
                            response.getSequence().size()
                    );
                    response.getSequence().forEach(sequenceItem -> {
                        if(!CollectionUtils.isEmpty(sequenceItem.getAttachments())) {
                            assertTrue(sequenceItem.getAttachments().containsAll(SequenceConfiguration.getConfig(statusCode).validAttachments().get(sequenceItem.getStatusCode())));
                        }
                    });
                })
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldReturnOKForRECRN002CWithM05() {
        String statusCode = "RECRN002C";
        String deliveryFailureCause = "M05";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause,"AR"))
                .assertNext(response -> {
                    assertEquals(SequenceResponse.FinalStatusCodeEnum.OK, response.getFinalStatusCode());
                    assertNotNull(response.getSequence());
                    assertEquals(
                            SequenceConfiguration.getConfig(statusCode).sequenceStatusCodes().size(),
                            response.getSequence().size()
                    );
                    response.getSequence().forEach(sequenceItem -> {
                        if(!CollectionUtils.isEmpty(sequenceItem.getAttachments())) {
                            assertTrue(sequenceItem.getAttachments().containsAll(SequenceConfiguration.getConfig(statusCode).validAttachments().get(sequenceItem.getStatusCode())));
                        }
                    });
                    response.getSequence().forEach(item -> assertTrue(StringUtils.hasText(item.getStatusCode())));
                })
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldReturnKOForRECRN002CWithM06() {
        String statusCode = "RECRN002C";
        String deliveryFailureCause = "M06";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause,"AR"))
                .assertNext(response -> {
                    assertEquals(SequenceResponse.FinalStatusCodeEnum.KO, response.getFinalStatusCode());
                    assertNotNull(response.getSequence());
                    assertEquals(
                            SequenceConfiguration.getConfig(statusCode).sequenceStatusCodes().size(),
                            response.getSequence().size()
                    );
                    response.getSequence().forEach(sequenceItem -> {
                        if(!CollectionUtils.isEmpty(sequenceItem.getAttachments())) {
                            assertTrue(sequenceItem.getAttachments().containsAll(SequenceConfiguration.getConfig(statusCode).validAttachments().get(sequenceItem.getStatusCode())));
                        }
                    });
                    response.getSequence().forEach(item -> assertTrue(StringUtils.hasText(item.getStatusCode())));
                })
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldReturnKOForRECRN002CWithM07() {
        String statusCode = "RECRN002C";
        String deliveryFailureCause = "M07";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause,"AR"))
                .assertNext(response -> {
                    assertEquals(SequenceResponse.FinalStatusCodeEnum.KO, response.getFinalStatusCode());
                    assertNotNull(response.getSequence());
                    assertEquals(
                            SequenceConfiguration.getConfig(statusCode).sequenceStatusCodes().size(),
                            response.getSequence().size()
                    );
                    response.getSequence().forEach(sequenceItem -> {
                        if(!CollectionUtils.isEmpty(sequenceItem.getAttachments())) {
                            assertTrue(sequenceItem.getAttachments().containsAll(SequenceConfiguration.getConfig(statusCode).validAttachments().get(sequenceItem.getStatusCode())));
                        }
                    });
                    response.getSequence().forEach(item -> assertTrue(StringUtils.hasText(item.getStatusCode())));
                })
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldReturnKOForRECRN002CWithM08() {
        String statusCode = "RECRN002C";
        String deliveryFailureCause = "M08";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause,"AR"))
                .assertNext(response -> {
                    assertEquals(SequenceResponse.FinalStatusCodeEnum.KO, response.getFinalStatusCode());
                    assertNotNull(response.getSequence());
                    assertEquals(
                            SequenceConfiguration.getConfig(statusCode).sequenceStatusCodes().size(),
                            response.getSequence().size()
                    );
                    response.getSequence().forEach(sequenceItem -> {
                        if(!CollectionUtils.isEmpty(sequenceItem.getAttachments())) {
                            assertTrue(sequenceItem.getAttachments().containsAll(SequenceConfiguration.getConfig(statusCode).validAttachments().get(sequenceItem.getStatusCode())));
                        }
                    });
                    response.getSequence().forEach(item -> assertTrue(StringUtils.hasText(item.getStatusCode())));
                })
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldReturnKOForRECRN002CWithM09() {
        String statusCode = "RECRN002C";
        String deliveryFailureCause = "M09";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause,"AR"))
                .assertNext(response -> {
                    assertEquals(SequenceResponse.FinalStatusCodeEnum.KO, response.getFinalStatusCode());
                    assertNotNull(response.getSequence());
                    assertFalse(response.getSequence().isEmpty());
                    assertEquals(
                            SequenceConfiguration.getConfig(statusCode).sequenceStatusCodes().size(),
                            response.getSequence().size()
                    );
                    response.getSequence().forEach(sequenceItem -> {
                        if(!CollectionUtils.isEmpty(sequenceItem.getAttachments())) {
                            assertTrue(sequenceItem.getAttachments().containsAll(SequenceConfiguration.getConfig(statusCode).validAttachments().get(sequenceItem.getStatusCode())));
                        }
                    });
                    response.getSequence().forEach(item -> assertTrue(StringUtils.hasText(item.getStatusCode())));
                })
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldThrowExceptionForProgressStatusCode() {
        String progressStatusCode = "RECRN010";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(progressStatusCode, null,"AR"))
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof PnPaperTrackerBadRequestException ex) {
                        assertNotNull(ex.getProblem().getDetail());
                        return Objects.requireNonNull(ex.getProblem().getErrors().getFirst().getDetail()).equalsIgnoreCase("statusCode RECRN010 is PROGRESS");
                    }
                    return false;
                })
                .verify();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldVerifySequenceContent() {
        String statusCode = "RECRN005C";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(statusCode, null,"AR"))
                .assertNext(response -> {
                    assertNotNull(response.getSequence());
                    response.getSequence().forEach(item -> assertTrue(StringUtils.hasText(item.getStatusCode())));
                    assertEquals(
                            SequenceConfiguration.getConfig(statusCode).sequenceStatusCodes().size(),
                            response.getSequence().size()
                    );
                    response.getSequence().forEach(sequenceItem -> {
                        if(!CollectionUtils.isEmpty(sequenceItem.getAttachments())) {
                            assertTrue(sequenceItem.getAttachments().containsAll(SequenceConfiguration.getConfig(statusCode).validAttachments().get(sequenceItem.getStatusCode())));
                        }
                    });
                })
                .verifyComplete();
    }

    @Test
    void retrieveSequenceAndEventStatus_shouldThrowExceptionForInvalidStatusCode() {
        String invalidStatusCode = "INVALID";
        String deliveryFailureCause = "M02";

        StepVerifier.create(service.retrieveSequenceAndEventStatus(invalidStatusCode, deliveryFailureCause,"AR"))
                .expectErrorMatches(throwable -> {
                    if (throwable instanceof PnPaperTrackerBadRequestException ex) {
                        assertNotNull(ex.getProblem().getDetail());
                        return Objects.requireNonNull(ex.getProblem().getErrors().getFirst().getDetail()).equalsIgnoreCase("statusCode INVALID is invalid");
                    }
                    return false;
                })
                .verify();
    }

    @Test
    void updatePaperTrackingsStatusForRework(){

        String trackingId = "tracking123";
        String reworkId = "rework123";
        PaperTrackings existingPaperTracking = new PaperTrackings();
        existingPaperTracking.setTrackingId(trackingId);
        when(paperTrackingsDAO.updateItemForRework(eq(trackingId), any(PaperTrackings.class)))
                .thenReturn(Mono.just(existingPaperTracking));

        Mono<Void> response = service.updatePaperTrackingsStatusForRework(trackingId, reworkId);

        StepVerifier.create(response)
                .verifyComplete();
        verify(paperTrackingsDAO, times(1)).updateItemForRework(eq(trackingId), argThat(pt ->
                pt.getState().equals(PaperTrackingsState.AWAITING_REWORK_EVENTS) &&
                        pt.getNotificationReworkId().equals(reworkId) &&
                        pt.getNotificationReworkRequestTimestamp() != null
        ));
    }
}