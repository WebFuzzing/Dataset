package it.pagopa.pn.papertracker.rest;

import it.pagopa.pn.papertracker.exception.PnPaperTrackerBadRequestException;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.SequenceResponse;
import it.pagopa.pn.papertracker.service.NotificationReworkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.mockito.Mockito.*;

class NotificationReworkControllerTest {

    private static final String ERROR_CODE_PAPER_TRACKER_BAD_REQUEST = "PN_PAPER_TRACKER_BAD_REQUEST";
    private NotificationReworkService notificationReworkService;
    private NotificationReworkController controller;
    private ServerWebExchange exchange;

    @BeforeEach
    void setUp() {
        notificationReworkService = mock(NotificationReworkService.class);
        controller = new NotificationReworkController(notificationReworkService);
        exchange = mock(ServerWebExchange.class);
    }

    @Test
    void notificationRework_shouldReturnOkResponse() {
        String statusCode = "RECRN005C";
        String deliveryFailureCause = "M02";
        SequenceResponse response = new SequenceResponse();
        response.setFinalStatusCode(SequenceResponse.FinalStatusCodeEnum.OK);
        response.setSequence(List.of());
        when(notificationReworkService.retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause, "AR"))
                .thenReturn(Mono.just(response));

        Mono<ResponseEntity<SequenceResponse>> result = controller.retrieveSequenceAndFinalStatus(statusCode, "AR", deliveryFailureCause, exchange);

        StepVerifier.create(result)
                .expectNextMatches(entity -> entity.getStatusCode().is2xxSuccessful()
                        && entity.getBody().getSequence().equals(List.of())
                        && entity.getBody().getFinalStatusCode() == SequenceResponse.FinalStatusCodeEnum.OK)
                .verifyComplete();

        verify(notificationReworkService, times(1)).retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause, "AR");
    }

    @Test
    void notificationRework_shouldPropagateError() {
        String statusCode = "INVALID";
        String deliveryFailureCause = "M02";
        when(notificationReworkService.retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause, "AR"))
                .thenReturn(Mono.error(new PnPaperTrackerBadRequestException(ERROR_CODE_PAPER_TRACKER_BAD_REQUEST, String.format("statusCode %s is invalid", statusCode))));

        Mono<ResponseEntity<SequenceResponse>> result = controller.retrieveSequenceAndFinalStatus(statusCode, "AR", deliveryFailureCause, exchange);

        StepVerifier.create(result)
                .expectErrorMatches(throwable -> throwable instanceof PnPaperTrackerBadRequestException )
                .verify();

        verify(notificationReworkService, times(1)).retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause,"AR");
    }

    @Test
    void initReworkReturnsNoContentResponse() {
        String reworkId = "rework123";
        String trackingId = "tracking123";

        when(notificationReworkService.updatePaperTrackingsStatusForRework(trackingId, reworkId)).thenReturn(Mono.empty());

        Mono<ResponseEntity<Void>> response = controller.initNotificationRework(reworkId, trackingId, null);

        StepVerifier.create(response)
                .expectNext(ResponseEntity.status(HttpStatus.NO_CONTENT).build())
                .verifyComplete();
        verify(notificationReworkService, times(1)).updatePaperTrackingsStatusForRework(trackingId, reworkId);
    }

    @Test
    void initReworkHandlesError() {
        String reworkId = "rework123";
        String trackingId = "tracking123";

        when(notificationReworkService.updatePaperTrackingsStatusForRework(trackingId, reworkId))
                .thenReturn(Mono.error(new RuntimeException("Service error")));

        Mono<ResponseEntity<Void>> response = controller.initNotificationRework(reworkId, trackingId, null);

        StepVerifier.create(response)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && "Service error".equals(throwable.getMessage()))
                .verify();
        verify(notificationReworkService, times(1)).updatePaperTrackingsStatusForRework(trackingId, reworkId);
    }
}