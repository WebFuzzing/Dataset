package it.pagopa.pn.papertracker.service.impl;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.PaperTrackerOutputsResponse;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsRequest;
import it.pagopa.pn.papertracker.mapper.PaperTrackerMapStructMapper;
import it.pagopa.pn.papertracker.middleware.dao.PaperTrackerDryRunOutputsDAO;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackerDryRunOutputs;
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
class PaperTrackerOutputServiceImplTest {

    @Mock
    private PaperTrackerDryRunOutputsDAO paperTrackerDryRunOutputsDAO;

    @Spy
    private PaperTrackerMapStructMapper mapper = Mappers.getMapper(PaperTrackerMapStructMapper.class);

    private PaperTrackerOutputServiceImpl paperTrackerOutputService;

    @BeforeEach
    void setUp() {
        paperTrackerOutputService = new PaperTrackerOutputServiceImpl(paperTrackerDryRunOutputsDAO,mapper);
    }

    @Test
    void retrieveTrackingOutputsReturnsResponseWithOutputs() {
        // Arrange
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(List.of("tracking1", "tracking2"));
        PaperTrackerDryRunOutputs paperTrackerDryRunOutputs1 = new PaperTrackerDryRunOutputs();
        paperTrackerDryRunOutputs1.setTrackingId("tracking1");
        PaperTrackerDryRunOutputs paperTrackerDryRunOutputs2 = new PaperTrackerDryRunOutputs();
        paperTrackerDryRunOutputs2.setTrackingId("tracking2");

        when(paperTrackerDryRunOutputsDAO.retrieveOutputEvents("tracking1"))
                .thenReturn(Flux.just(paperTrackerDryRunOutputs1));
        when(paperTrackerDryRunOutputsDAO.retrieveOutputEvents("tracking2"))
                .thenReturn(Flux.just(paperTrackerDryRunOutputs2));

        // Act
        Mono<PaperTrackerOutputsResponse> response = paperTrackerOutputService.retrieveTrackingOutputs(request);

        // Assert
        StepVerifier.create(response)
                .expectNextMatches(res -> res.getResults().getFirst().getTrackingId().equals("tracking1") &&
                        res.getResults().getLast().getTrackingId().equals("tracking2"))
                .verifyComplete();

        verify(paperTrackerDryRunOutputsDAO, times(1)).retrieveOutputEvents("tracking1");
        verify(paperTrackerDryRunOutputsDAO, times(1)).retrieveOutputEvents("tracking2");
    }

    @Test
    void retrieveTrackingOutputsReturnsResponseWithoutOutputs() {
        // Arrange
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(List.of("tracking1", "tracking2"));
        PaperTrackerDryRunOutputs paperTrackerDryRunOutputs1 = new PaperTrackerDryRunOutputs();
        paperTrackerDryRunOutputs1.setTrackingId("tracking1");
        PaperTrackerDryRunOutputs paperTrackerDryRunOutputs2 = new PaperTrackerDryRunOutputs();
        paperTrackerDryRunOutputs2.setTrackingId("tracking2");

        when(paperTrackerDryRunOutputsDAO.retrieveOutputEvents("tracking1"))
                .thenReturn(Flux.empty());
        when(paperTrackerDryRunOutputsDAO.retrieveOutputEvents("tracking2"))
                .thenReturn(Flux.empty());

        // Act
        Mono<PaperTrackerOutputsResponse> response = paperTrackerOutputService.retrieveTrackingOutputs(request);

        // Assert
        StepVerifier.create(response)
                .expectNextMatches(res -> {
                    Assertions.assertNotNull(res.getResults());
                    return CollectionUtils.isEmpty(res.getResults().getFirst().getOutputs())  &&
                            CollectionUtils.isEmpty(res.getResults().getLast().getOutputs());
                })
                .verifyComplete();
        verify(paperTrackerDryRunOutputsDAO, times(1)).retrieveOutputEvents("tracking1");
        verify(paperTrackerDryRunOutputsDAO, times(1)).retrieveOutputEvents("tracking2");
    }

    @Test
    void retrieveTrackingOutputsHandlesEmptyTrackingIds() {
        // Arrange
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(Collections.emptyList());

        // Act
        Mono<PaperTrackerOutputsResponse> response = paperTrackerOutputService.retrieveTrackingOutputs(request);

        // Assert
        StepVerifier.create(response)
                .expectNextMatches(res -> res.getResults().isEmpty())
                .verifyComplete();
        verifyNoInteractions(paperTrackerDryRunOutputsDAO);
    }

    @Test
    void retrieveTrackingOutputsHandlesErrorFromDAO() {
        // Arrange
        TrackingsRequest request = new TrackingsRequest();
        request.setTrackingIds(List.of("tracking1"));

        when(paperTrackerDryRunOutputsDAO.retrieveOutputEvents("tracking1"))
                .thenReturn(Flux.error(new RuntimeException("DAO error")));

        // Act
        Mono<PaperTrackerOutputsResponse> response = paperTrackerOutputService.retrieveTrackingOutputs(request);

        // Assert
        StepVerifier.create(response)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && "DAO error".equals(throwable.getMessage()))
                .verify();
        verify(paperTrackerDryRunOutputsDAO, times(1)).retrieveOutputEvents("tracking1");
    }

}
