package it.pagopa.pn.papertracker.rest;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.api.NotificationReworkApi;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.SequenceResponse;
import it.pagopa.pn.papertracker.service.NotificationReworkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
@RestController
@Slf4j
@RequiredArgsConstructor
public class NotificationReworkController implements NotificationReworkApi {

    private final NotificationReworkService notificationReworkService;

    @Override
    public Mono<ResponseEntity<SequenceResponse>> retrieveSequenceAndFinalStatus(String statusCode, String productType, String deliveryFailureCause, final ServerWebExchange exchange) {
        return notificationReworkService.retrieveSequenceAndEventStatus(statusCode, deliveryFailureCause, productType)
                .map(ResponseEntity::ok);
    }

    @Override
    public Mono<ResponseEntity<Void>> initNotificationRework(String reworkId, String trackingId,  final ServerWebExchange exchange) {
        return notificationReworkService.updatePaperTrackingsStatusForRework(trackingId, reworkId)
                .thenReturn(ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }
}
