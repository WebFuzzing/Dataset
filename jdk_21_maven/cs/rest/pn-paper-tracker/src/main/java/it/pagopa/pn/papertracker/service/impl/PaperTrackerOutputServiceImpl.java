package it.pagopa.pn.papertracker.service.impl;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.PaperTrackerOutputsResponse;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.PaperTrackerOutputsResponseResultsInner;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsRequest;
import it.pagopa.pn.papertracker.mapper.PaperTrackerMapStructMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackerDryRunOutputsDAO;
import it.pagopa.pn.papertracker.service.PaperTrackerOutputService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaperTrackerOutputServiceImpl implements PaperTrackerOutputService {

    private final PaperTrackerDryRunOutputsDAO paperTrackerDryRunOutputsDAO;
    private final PaperTrackerMapStructMapper mapper;

    @Override
    public Mono<PaperTrackerOutputsResponse> retrieveTrackingOutputs(TrackingsRequest trackingsRequest) {
        PaperTrackerOutputsResponse paperTrackerOutputsResponse = new PaperTrackerOutputsResponse();
        return Flux.fromIterable(trackingsRequest.getTrackingIds())
                .flatMap(trackingId -> paperTrackerDryRunOutputsDAO.retrieveOutputEvents(trackingId)
                        .map(mapper::toDtoPaperTrackerOutput)
                        .collectList()
                        .map(paperTrackerDryRunOutputs -> {
                            PaperTrackerOutputsResponseResultsInner paperTrackerOutputsResponseResultInner = new PaperTrackerOutputsResponseResultsInner();
                            paperTrackerOutputsResponseResultInner.setTrackingId(trackingId);
                            paperTrackerOutputsResponseResultInner.setOutputs(paperTrackerDryRunOutputs);
                            return paperTrackerOutputsResponseResultInner;
                        }))
                .collectList()
                .doOnNext(paperTrackerOutputsResponse::setResults)
                .map(paperTrackerOutputsResponseResultInners -> paperTrackerOutputsResponse);
    }
}
