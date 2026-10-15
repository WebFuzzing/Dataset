package it.pagopa.pn.papertracker.rest;

import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingErrorsResponse;
import it.pagopa.pn.papertracker.generated.openapi.server.v1.dto.TrackingsRequest;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaperTrackerErrorControllerTest {

    @Mock
    private PaperTrackerErrorService paperTrackerErrorService;

    private PaperTrackerErrorController paperTrackerErrorController;

    @BeforeEach
    void setUp() {
        paperTrackerErrorController = new PaperTrackerErrorController(paperTrackerErrorService);
    }

    @Test
    void retrieveTrackingErrorsReturnsOkResponse() {
        //Arrange
        TrackingsRequest request = new TrackingsRequest();
        Mono<TrackingsRequest> requestMono = Mono.just(request);
        TrackingErrorsResponse responseMock = new TrackingErrorsResponse();

        when(paperTrackerErrorService.retrieveTrackingErrors(request)).thenReturn(Mono.just(responseMock));

        //Act
        Mono<ResponseEntity<TrackingErrorsResponse>> response = paperTrackerErrorController.retrieveTrackingErrors(requestMono, null);

        //Assert
        StepVerifier.create(response)
                .expectNext(ResponseEntity.ok(responseMock))
                .verifyComplete();
        verify(paperTrackerErrorService, times(1)).retrieveTrackingErrors(request);
    }

    @Test
    void retrieveTrackingErrorsHandlesError() {
        //Arrange
        TrackingsRequest request = new TrackingsRequest();
        Mono<TrackingsRequest> requestMono = Mono.just(request);

        when(paperTrackerErrorService.retrieveTrackingErrors(request)).thenReturn(Mono.error(new RuntimeException("Service error")));

        //Act
        Mono<ResponseEntity<TrackingErrorsResponse>> response = paperTrackerErrorController.retrieveTrackingErrors(requestMono, null);

        //Assert
        StepVerifier.create(response)
                .expectErrorMatches(throwable -> throwable instanceof RuntimeException
                        && "Service error".equals(throwable.getMessage()))
                .verify();
        verify(paperTrackerErrorService, times(1)).retrieveTrackingErrors(request);
    }

}
