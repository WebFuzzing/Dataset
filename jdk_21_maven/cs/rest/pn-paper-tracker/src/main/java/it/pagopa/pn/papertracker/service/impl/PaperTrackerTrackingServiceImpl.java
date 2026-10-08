package it.pagopa.pn.papertracker.service.impl;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.config.TrackerConfigUtils;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingCreationRequest;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsRequest;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsResponse;
import it.pagopa.pn.papertracker.mapper.PaperTrackerMapStructMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.service.PaperTrackerTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

import static it.pagopa.pn.papertracker.mapper.PaperTrackingsMapper.toPaperTrackings;

@Service
@Slf4j
@RequiredArgsConstructor
public class PaperTrackerTrackingServiceImpl implements PaperTrackerTrackingService {

    private final PaperTrackingsDAO paperTrackingsDAO;
    private final TrackerConfigUtils trackerConfigUtils;
    private final PnPaperTrackerConfigs pnPaperTrackerConfigs;
    private final PaperTrackerMapStructMapper mapper;

    @Override
    public Mono<Void> insertPaperTrackings(TrackingCreationRequest trackingCreationRequest, String xOriginClientId) {
        log.info("Init tracking for request: {}", trackingCreationRequest);

        return paperTrackingsDAO.putIfAbsent(toPaperTrackings(trackingCreationRequest, trackerConfigUtils, pnPaperTrackerConfigs, Instant.now(), xOriginClientId)).then();
    }

    @Override
    public Mono<TrackingsResponse> retrieveTrackings(TrackingsRequest trackingsRequest) {
        log.info("Retrieving trackings for request: {}", trackingsRequest);

        TrackingsResponse response = new TrackingsResponse();
        return paperTrackingsDAO.retrieveAllByTrackingIds(trackingsRequest.getTrackingIds())
                .map(mapper::toTracking)
                .collectList()
                .doOnNext(trackings -> log.info("Retrieved {} trackings for request {}", trackings.size(), trackingsRequest))
                .doOnNext(response::setTrackings)
                .thenReturn(response);
    }

    @Override
    public Mono<Void> updatePaperTrackingsStatus(String trackingId, PaperTrackings paperTrackings) {
        log.info("Updating tracking status for trackingId: {}", trackingId);

        return paperTrackingsDAO.updateItem(trackingId, paperTrackings)
                .then();
    }

    @Override
    public Mono<TrackingsResponse> retrieveTrackingsByAttemptId(String attemptId, String pcRetry) {
        log.info("Retrieving trackings for attemptId: {} with pcRetry: {}", attemptId, pcRetry);

        TrackingsResponse response = new TrackingsResponse();
        return paperTrackingsDAO.retrieveEntityByAttemptId(attemptId, pcRetry)
                .map(mapper::toTracking)
                .collectList()
                .doOnNext(response::setTrackings)
                .thenReturn(response);
    }

}
