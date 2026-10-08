package it.pagopa.pn.papertracker.service.impl;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingErrorsResponse;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsRequest;
import it.pagopa.pn.papertracker.mapper.PaperTrackerMapStructMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsErrorsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ErrorDetails;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsErrors;
import org.junit.jupiter.api.Assertions;
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
class PaperTrackerErrorServiceImplTest {

    @Mock
    private PaperTrackingsErrorsDAO paperTrackingsErrorsDAO;

    @Spy
    private PaperTrackerMapStructMapper mapper = Mappers.getMapper(PaperTrackerMapStructMapper.class);

    private PaperTrackerErrorServiceImpl paperTrackerErrorService;

    @BeforeEach
    void setUp() {
        paperTrackerErrorService = new PaperTrackerErrorServiceImpl(paperTrackingsErrorsDAO, mapper);
    }

    @Test
    void retrieveTrackingErrorsReturnsResponseWithErrors() {
        //ARRANGE
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(List.of("tracking1", "tracking2"));
        PaperTrackingsErrors paperTrackingsErrors1 = new PaperTrackingsErrors();
        paperTrackingsErrors1.setTrackingId("tracking1");
        PaperTrackingsErrors paperTrackingsErrors2 = new PaperTrackingsErrors();
        paperTrackingsErrors2.setTrackingId("tracking2");

        when(paperTrackingsErrorsDAO.retrieveErrors("tracking1"))
                .thenReturn(Flux.just(paperTrackingsErrors1));
        when(paperTrackingsErrorsDAO.retrieveErrors("tracking2"))
                .thenReturn(Flux.just(paperTrackingsErrors2));

        //ACT
        Mono<TrackingErrorsResponse> response = paperTrackerErrorService.retrieveTrackingErrors(request);

        //ASSERT
        StepVerifier.create(response)
                .expectNextMatches(res -> res.getResults().getFirst().getTrackingId().equals("tracking1") &&
                        res.getResults().getLast().getTrackingId().equals("tracking2"))
                .verifyComplete();
        verify(paperTrackingsErrorsDAO, times(1)).retrieveErrors("tracking1");
        verify(paperTrackingsErrorsDAO, times(1)).retrieveErrors("tracking2");
    }

    @Test
    void retrieveTrackingErrorsReturnsResponseWithoutErrors() {
        //ARRANGE
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(List.of("tracking1", "tracking2"));
        PaperTrackingsErrors paperTrackingsErrors1 = new PaperTrackingsErrors();
        paperTrackingsErrors1.setTrackingId("tracking1");
        PaperTrackingsErrors paperTrackingsErrors2 = new PaperTrackingsErrors();
        paperTrackingsErrors2.setTrackingId("tracking2");

        when(paperTrackingsErrorsDAO.retrieveErrors("tracking1"))
                .thenReturn(Flux.empty());
        when(paperTrackingsErrorsDAO.retrieveErrors("tracking2"))
                .thenReturn(Flux.empty());

        //ACT
        Mono<TrackingErrorsResponse> response = paperTrackerErrorService.retrieveTrackingErrors(request);

        //ASSERT
        StepVerifier.create(response)
                .expectNextMatches(res -> {
                    Assertions.assertNotNull(res.getResults());
                    return CollectionUtils.isEmpty(res.getResults().getFirst().getErrors())  &&
                            CollectionUtils.isEmpty(res.getResults().getLast().getErrors());
                })
                .verifyComplete();
        verify(paperTrackingsErrorsDAO, times(1)).retrieveErrors("tracking1");
        verify(paperTrackingsErrorsDAO, times(1)).retrieveErrors("tracking2");
    }

    @Test
    void retrieveTrackingErrorsHandlesEmptyTrackingIds() {
        //ARRANGE
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(Collections.emptyList());

        //ACT
        Mono<TrackingErrorsResponse> response = paperTrackerErrorService.retrieveTrackingErrors(request);

        //ASSERT
        StepVerifier.create(response)
                .expectNextMatches(res -> res.getResults().isEmpty())
                .verifyComplete();
        verifyNoInteractions(paperTrackingsErrorsDAO);
    }

    @Test
    void retrieveTrackingErrorsHandlesErrorFromDAO() {
        //ARRANGE
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(List.of("tracking1"));

        when(paperTrackingsErrorsDAO.retrieveErrors("tracking1"))
                .thenReturn(Flux.error(new RuntimeException("DAO error")));

        //ACT
        Mono<TrackingErrorsResponse> response = paperTrackerErrorService.retrieveTrackingErrors(request);

        //ASSERT
        StepVerifier.create(response)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && "DAO error".equals(throwable.getMessage()))
                .verify();
        verify(paperTrackingsErrorsDAO, times(1)).retrieveErrors("tracking1");
    }

    @Test
    void insertPaperTrackingsErrorSuccessfully() {
        //ARRANGE
        PaperTrackingsErrors paperTrackingsErrors = new PaperTrackingsErrors();
        paperTrackingsErrors.setDetails(ErrorDetails.builder().build());
        when(paperTrackingsErrorsDAO.insertError(paperTrackingsErrors)).thenReturn(Mono.just(paperTrackingsErrors));

        //ACT
        Mono<PaperTrackingsErrors> response = paperTrackerErrorService.insertPaperTrackingsError(paperTrackingsErrors);

        //ASSERT
        StepVerifier.create(response)
                .expectNext(paperTrackingsErrors)
                .verifyComplete();
        verify(paperTrackingsErrorsDAO, times(1)).insertError(paperTrackingsErrors);
    }
}
