package it.pagopa.pn.papertracker.middleware.dao;

import com.sngular.apigenerator.asyncapi.business_model.model.event.Data;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface PaperTrackingsDAO {

    Mono<PaperTrackings> putIfAbsent(PaperTrackings entity);

    Mono<PaperTrackings> retrieveEntityByTrackingId(String trackingId);

    Flux<PaperTrackings> retrieveEntityByAttemptId(String attemptId, String pcRetry);

    Mono<PaperTrackings> updateItem(String requestId, PaperTrackings paperTrackings);

    Mono<PaperTrackings> updateItemForRework(String trackingId, PaperTrackings paperTrackings);

    Mono<PaperTrackings> updateOcrRequests(Integer ocrRequestIndex, String trackingId, Data.ValidationStatus validationStatus);

    Flux<PaperTrackings> retrieveAllByTrackingIds(List<String> trackingIds);
}
