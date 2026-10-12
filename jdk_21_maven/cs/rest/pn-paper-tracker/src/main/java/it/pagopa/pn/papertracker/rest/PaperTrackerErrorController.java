package it.pagopa.pn.papertracker.rest;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.api.PaperTrackerErrorApi;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingErrorsResponse;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsRequest;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@RestController
@Slf4j
@RequiredArgsConstructor
public class PaperTrackerErrorController implements PaperTrackerErrorApi {

    private final PaperTrackerErrorService paperTrackerErrorService;

    @Override
    public Mono<ResponseEntity<TrackingErrorsResponse>> retrieveTrackingErrors(Mono<TrackingsRequest> trackingsRequest, final ServerWebExchange exchange) {
        return trackingsRequest
                .flatMap(paperTrackerErrorService::retrieveTrackingErrors)
                .map(ResponseEntity::ok);
    }
}
