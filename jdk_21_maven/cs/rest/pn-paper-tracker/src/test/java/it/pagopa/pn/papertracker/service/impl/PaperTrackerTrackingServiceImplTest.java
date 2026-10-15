package it.pagopa.pn.papertracker.service.impl;

import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import it.pagopa.pn.papertracker.config.TrackerConfigUtils;
import it.pagopa.pn.papertracker.exception.PnPaperTrackerConflictException;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingCreationRequest;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsRequest;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsResponse;
import it.pagopa.pn.papertracker.mapper.PaperTrackerMapStructMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProcessingMode;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaperTrackerTrackingServiceImplTest {

    @Mock
    private PaperTrackingsDAO paperTrackingsDAO;

    @Mock
    private PnPaperTrackerConfigs pnPaperTrackerConfigs;

    private PaperTrackerTrackingServiceImpl paperTrackerEventService;

    @Spy
    private PaperTrackerMapStructMapper mapper = Mappers.getMapper(PaperTrackerMapStructMapper.class);

    @BeforeEach
    void setUp() {
        when(pnPaperTrackerConfigs.getRequiredAttachmentsRefinementStock890()).thenReturn(List.of("1970-01-01;23L"));
        when(pnPaperTrackerConfigs.getSendOcrAttachmentsRefinementStock890()).thenReturn(List.of("1970-01-01;23L"));
        when(pnPaperTrackerConfigs.getSendOcrAttachmentsFinalValidationStock890()).thenReturn(List.of("1970-01-01;ARCAD;CAD"));
        when(pnPaperTrackerConfigs.getSendOcrAttachmentsFinalValidation()).thenReturn(List.of("1970-01-01;Plico;AR;23L"));
        when(pnPaperTrackerConfigs.getStrictFinalValidationStock890()).thenReturn(List.of("1970-01-01;true"));
        when(pnPaperTrackerConfigs.getStrictDeliveryFailureCause()).thenReturn(List.of("1970-01-01;false"));
        when(pnPaperTrackerConfigs.getProductsProcessingModes()).thenReturn(List.of("1970-01-01;AR:RUN;RS:DRY"));
        when(pnPaperTrackerConfigs.getEnableOcrValidationFor()).thenReturn(List.of("1970-01-01;AR:RUN;RIR:RUN;"));
        TrackerConfigUtils trackerConfigUtils = new TrackerConfigUtils(pnPaperTrackerConfigs);
        paperTrackerEventService = new PaperTrackerTrackingServiceImpl(paperTrackingsDAO,trackerConfigUtils,pnPaperTrackerConfigs, mapper);
    }

    @Test
    void insertPaperTrackingsValidRequest() {
        //ARRANGE
        TrackingCreationRequest request = getTrackerCreationRequest();
        String xOriginClientId = "clientId";

        when(paperTrackingsDAO.putIfAbsent(argThat(pt ->
                pt.getTrackingId().equals(String.join(".", request.getAttemptId(), request.getPcRetry())) &&
                        pt.getUnifiedDeliveryDriver().equals(request.getUnifiedDeliveryDriver()) &&
                        pt.getProductType() == ProductType.RS.getValue() &&
                        pt.getProcessingMode() == ProcessingMode.DRY &&
                        pt.getAnalogRequestClientId().equals(xOriginClientId)
        ))).thenReturn(Mono.just(new PaperTrackings()));

        //ACT
        Mono<Void> response = paperTrackerEventService.insertPaperTrackings(request, xOriginClientId);

        //ASSERT
        StepVerifier.create(response)
                .verifyComplete();
        verify(paperTrackingsDAO, times(1)).putIfAbsent(argThat(pt ->
                pt.getTrackingId().equals(String.join(".", request.getAttemptId(), request.getPcRetry())) &&
                        pt.getUnifiedDeliveryDriver().equals(request.getUnifiedDeliveryDriver()) &&
                        pt.getProductType() == ProductType.RS.getValue() &&
                        pt.getProcessingMode() == ProcessingMode.DRY &&
                        pt.getAnalogRequestClientId().equals(xOriginClientId)
        ));
    }

    @Test
    void insertPaperTrackingsConflictException() {
        //ARRANGE
        TrackingCreationRequest request = getTrackerCreationRequest();
        String xOriginClientId = "clientId";

        when(paperTrackingsDAO.putIfAbsent(argThat(pt ->
                pt.getTrackingId().equals(String.join(".", request.getAttemptId(), request.getPcRetry())) &&
                        pt.getUnifiedDeliveryDriver().equals(request.getUnifiedDeliveryDriver()) &&
                        pt.getProductType() == ProductType.RS.getValue()
        ))).thenReturn(Mono.error(new PnPaperTrackerConflictException("", "")));

        //ACT
        Mono<Void> response = paperTrackerEventService.insertPaperTrackings(request, xOriginClientId);

        //ASSERT
        StepVerifier.create(response)
                .expectError(PnPaperTrackerConflictException.class)
                .verify();
        verify(paperTrackingsDAO, times(1)).putIfAbsent(argThat(pt ->
                pt.getTrackingId().equals(String.join(".", request.getAttemptId(), request.getPcRetry())) &&
                        pt.getUnifiedDeliveryDriver().equals(request.getUnifiedDeliveryDriver()) &&
                        pt.getProductType() == ProductType.RS.getValue()
        ));
    }

    @Test
    void retrieveTrackingsReturnsResponseWithTrackings() {
        //ARRANGE
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(List.of("tracking1", "tracking2"));
        PaperTrackings paperTracking1 = new PaperTrackings();
        PaperTrackings paperTracking2 = new PaperTrackings();
        TrackingsResponse expectedResponse = new TrackingsResponse();
        expectedResponse.setTrackings(List.of(mapper.toTracking(paperTracking1), mapper.toTracking(paperTracking2)));

        when(paperTrackingsDAO.retrieveAllByTrackingIds(request.getTrackingIds()))
                .thenReturn(Flux.just(paperTracking1, paperTracking2));

        //ACT
        Mono<TrackingsResponse> response = paperTrackerEventService.retrieveTrackings(request);

        //ASSERT
        StepVerifier.create(response)
                .expectNextMatches(res -> res.getTrackings().equals(expectedResponse.getTrackings()))
                .verifyComplete();
        verify(paperTrackingsDAO, times(1)).retrieveAllByTrackingIds(request.getTrackingIds());
    }

    @Test
    void retrieveTrackingsReturnsResponseWithoutTrackings() {
        //ARRANGE
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(List.of("tracking1", "tracking2"));

        when(paperTrackingsDAO.retrieveAllByTrackingIds(request.getTrackingIds()))
                .thenReturn(Flux.empty());

        //ACT
        Mono<TrackingsResponse> response = paperTrackerEventService.retrieveTrackings(request);

        //ASSERT
        StepVerifier.create(response)
                .expectNextMatches(res -> CollectionUtils.isEmpty(res.getTrackings()))
                .verifyComplete();
        verify(paperTrackingsDAO, times(1)).retrieveAllByTrackingIds(request.getTrackingIds());
    }



    @Test
    void retrieveTrackingsHandlesEmptyTrackingIds() {
        //ARRANGE
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(Collections.emptyList());

        when(paperTrackingsDAO.retrieveAllByTrackingIds(request.getTrackingIds()))
                .thenReturn(Flux.empty());

        //ACT
        Mono<TrackingsResponse> response = paperTrackerEventService.retrieveTrackings(request);

        //ASSERT
        StepVerifier.create(response)
                .expectNextMatches(res -> res.getTrackings().isEmpty())
                .verifyComplete();
        verify(paperTrackingsDAO, times(1)).retrieveAllByTrackingIds(request.getTrackingIds());
    }

    @Test
    void retrieveTrackingsHandlesErrorFromDAO() {
        //ARRANGE
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(List.of("tracking1"));

        when(paperTrackingsDAO.retrieveAllByTrackingIds(request.getTrackingIds()))
                .thenReturn(Flux.error(new RuntimeException("DAO error")));

        //ACT
        Mono<TrackingsResponse> response = paperTrackerEventService.retrieveTrackings(request);

        //ASSERT
        StepVerifier.create(response)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && "DAO error".equals(throwable.getMessage()))
                .verify();
        verify(paperTrackingsDAO, times(1)).retrieveAllByTrackingIds(request.getTrackingIds());
    }

    @Test
    void retrieveTrackingsByAttemptIdReturnsResponseWithTrackings() {
        //ARRANGE
        String attemptId = "attempt123";
        String pcRetry = "PCRETRY_0";
        PaperTrackings paperTracking1 = new PaperTrackings();
        PaperTrackings paperTracking2 = new PaperTrackings();
        TrackingsResponse expectedResponse = new TrackingsResponse();
        expectedResponse.setTrackings(List.of(mapper.toTracking(paperTracking1), mapper.toTracking(paperTracking2)));

        when(paperTrackingsDAO.retrieveEntityByAttemptId(attemptId, pcRetry))
                .thenReturn(Flux.just(paperTracking1, paperTracking2));

        //ACT
        Mono<TrackingsResponse> response = paperTrackerEventService.retrieveTrackingsByAttemptId(attemptId, pcRetry);

        //ASSERT
        StepVerifier.create(response)
                .expectNextMatches(res -> res.getTrackings().equals(expectedResponse.getTrackings()))
                .verifyComplete();
        verify(paperTrackingsDAO, times(1)).retrieveEntityByAttemptId(attemptId, pcRetry);
    }

    @Test
    void retrieveTrackingsByAttemptIdReturnsResponseWithoutTrackings() {
        //ARRANGE
        String attemptId = "attempt123";
        String pcRetry = "PCRETRY_0";

        when(paperTrackingsDAO.retrieveEntityByAttemptId(attemptId, pcRetry))
                .thenReturn(Flux.empty());

        //ACT
        Mono<TrackingsResponse> response = paperTrackerEventService.retrieveTrackingsByAttemptId(attemptId, pcRetry);

        //ASSERT
        StepVerifier.create(response)
                .expectNextMatches(res -> CollectionUtils.isEmpty(res.getTrackings()))
                .verifyComplete();
        verify(paperTrackingsDAO, times(1)).retrieveEntityByAttemptId(attemptId, pcRetry);
    }

    @Test
    void retrieveTrackingsByAttemptIdHandlesEmptyTrackingIds() {
        //ARRANGE
        String attemptId = "attempt123";
        String pcRetry = "PCRETRY_0";

        when(paperTrackingsDAO.retrieveEntityByAttemptId(attemptId, pcRetry))
                .thenReturn(Flux.empty());

        //ACT
        Mono<TrackingsResponse> response = paperTrackerEventService.retrieveTrackingsByAttemptId(attemptId, pcRetry);

        //ASSERT
        StepVerifier.create(response)
                .expectNextMatches(res -> res.getTrackings().isEmpty())
                .verifyComplete();
        verify(paperTrackingsDAO, times(1)).retrieveEntityByAttemptId(attemptId, pcRetry);
    }

    @Test
    void retrieveTrackingsByAttemptIdHandlesErrorFromDAO() {
        //ARRANGE
        String attemptId = "attempt123";
        String pcRetry = "PCRETRY_0";

        when(paperTrackingsDAO.retrieveEntityByAttemptId(attemptId, pcRetry))
                .thenReturn(Flux.error(new RuntimeException("DAO error")));

        //ACT
        Mono<TrackingsResponse> response = paperTrackerEventService.retrieveTrackingsByAttemptId(attemptId, pcRetry);

        //ASSERT
        StepVerifier.create(response)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && "DAO error".equals(throwable.getMessage()))
                .verify();
        verify(paperTrackingsDAO, times(1)).retrieveEntityByAttemptId(attemptId, pcRetry);
    }

    private TrackingCreationRequest getTrackerCreationRequest() {
        TrackingCreationRequest request = new TrackingCreationRequest();
        request.setAttemptId("request123");
        request.setPcRetry("PCRETRY_0");
        request.setUnifiedDeliveryDriver("driver456");
        request.setProductType("RS");
        return request;
    }

}