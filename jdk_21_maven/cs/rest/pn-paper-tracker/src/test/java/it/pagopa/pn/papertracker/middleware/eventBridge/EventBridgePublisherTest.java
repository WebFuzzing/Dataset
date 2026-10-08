package it.pagopa.pn.papertracker.middleware.eventBridge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventBridgePublisherTest {

    @Mock
    private EventBridgeAsyncClient client;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private PnPaperTrackerConfigs configs;

    @InjectMocks
    private EventBridgePublisher publisher;

    @Test
    void publishSuccessfully() throws JsonProcessingException {
        Object event = new Object();
        String eventDetail = "{\"key\":\"value\"}";
        PutEventsResponse response = PutEventsResponse.builder().build();

        when(configs.getEventBus()).thenReturn(new PnPaperTrackerConfigs.EventBus());
        when(objectMapper.writeValueAsString(event)).thenReturn(eventDetail);
        when(client.putEvents(any(PutEventsRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(response));

        StepVerifier.create(publisher.publish(event))
                .expectNext(response)
                .verifyComplete();

        verify(client, times(1)).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void publishWithSerializationError() throws JsonProcessingException {
        Object event = new Object();

        when(objectMapper.writeValueAsString(event)).thenThrow(new JsonProcessingException("Serialization error") {
        });

        StepVerifier.create(publisher.publish(event))
                .expectError(JsonProcessingException.class)
                .verify();

        verify(client, never()).putEvents(any(PutEventsRequest.class));
    }

    @Test
    void publishWithClientError() throws JsonProcessingException {
        Object event = new Object();
        String eventDetail = "{\"key\":\"value\"}";

        when(configs.getEventBus()).thenReturn(new PnPaperTrackerConfigs.EventBus());
        when(objectMapper.writeValueAsString(event)).thenReturn(eventDetail);
        when(client.putEvents(any(PutEventsRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(new RuntimeException("Client error")));

        StepVerifier.create(publisher.publish(event))
                .expectError(RuntimeException.class)
                .verify();

        verify(client, times(1)).putEvents(any(PutEventsRequest.class));
    }
}
