package it.pagopa.pn.papertracker.middleware.dao;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackerDryRunOutputs;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface PaperTrackerDryRunOutputsDAO {

    Flux<PaperTrackerDryRunOutputs> retrieveOutputEvents(String requestId);

    Mono<PaperTrackerDryRunOutputs> insertOutputEvent(PaperTrackerDryRunOutputs paperTrackerDryRunOutputs);
}
