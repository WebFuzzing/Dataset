package it.pagopa.pn.papertracker.service;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingErrorsResponse;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsRequest;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsErrors;
import reactor.core.publisher.Mono;

public interface PaperTrackerErrorService {

    Mono<TrackingErrorsResponse> retrieveTrackingErrors(TrackingsRequest trackingsRequest);

    Mono<PaperTrackingsErrors> insertPaperTrackingsError(PaperTrackingsErrors paperTrackingsErrors);

}
