package it.pagopa.pn.papertracker.service;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.PaperTrackerOutputsResponse;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsRequest;
import reactor.core.publisher.Mono;

public interface PaperTrackerOutputService {

    Mono<PaperTrackerOutputsResponse> retrieveTrackingOutputs(TrackingsRequest trackingsRequest);
}
