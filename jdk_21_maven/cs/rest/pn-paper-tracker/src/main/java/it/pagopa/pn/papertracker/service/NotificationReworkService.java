package it.pagopa.pn.papertracker.service;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.SequenceResponse;
import reactor.core.publisher.Mono;

public interface NotificationReworkService {

    Mono<SequenceResponse> retrieveSequenceAndEventStatus(String statusCode, String deliveryFailureCause, String productType);

    Mono<Void> updatePaperTrackingsStatusForRework(String trackingId, String reworkId);

}
