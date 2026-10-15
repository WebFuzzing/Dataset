package it.pagopa.pn.papertracker.rest;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.api.PaperTrackerOutputApi;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.PaperTrackerOutputsResponse;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsRequest;
import it.pagopa.pn.papertracker.service.PaperTrackerOutputService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PaperTrackerOutputController implements PaperTrackerOutputApi {

    private final PaperTrackerOutputService paperTrackerOutputService;

    @Override
    public Mono<ResponseEntity<PaperTrackerOutputsResponse>> retrieveTrackingOutputs(Mono<TrackingsRequest> trackingsRequest, final ServerWebExchange exchange) {
        return trackingsRequest
                .flatMap(paperTrackerOutputService::retrieveTrackingOutputs)
                .map(ResponseEntity::ok);
    }
}
