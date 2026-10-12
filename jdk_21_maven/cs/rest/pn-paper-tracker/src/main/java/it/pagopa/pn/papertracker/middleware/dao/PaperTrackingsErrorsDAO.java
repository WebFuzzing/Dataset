package it.pagopa.pn.papertracker.middleware.dao;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsErrors;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaperTrackingsErrorsDAO {

    Mono<PaperTrackingsErrors> insertError(PaperTrackingsErrors entity);

    Flux<PaperTrackingsErrors> retrieveErrors(String requestId);

}
