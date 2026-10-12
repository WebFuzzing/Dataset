package it.pagopa.pn.papertracker.service;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingCreationRequest;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsRequest;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsResponse;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import reactor.core.publisher.Mono;

public interface PaperTrackerTrackingService {

    Mono<Void> insertPaperTrackings(TrackingCreationRequest trackingCreationRequest, String xOriginClientId);

    Mono<TrackingsResponse> retrieveTrackings(TrackingsRequest trackingsRequest);

    Mono<Void> updatePaperTrackingsStatus(String trackingId, PaperTrackings paperTrackings);

    Mono<TrackingsResponse> retrieveTrackingsByAttemptId(String attemptId, String pcRetry);

}
