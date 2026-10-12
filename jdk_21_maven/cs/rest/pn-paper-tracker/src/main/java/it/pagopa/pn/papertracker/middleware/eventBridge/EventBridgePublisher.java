package it.pagopa.pn.papertracker.middleware.eventBridge;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.pagopa.pn.papertracker.config.PnPaperTrackerConfigs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;
import software.amazon.awssdk.services.eventbridge.model.PutEventsResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventBridgePublisher {

    private final EventBridgeAsyncClient client;
    private final ObjectMapper objectMapper;
    private final PnPaperTrackerConfigs configs;

    public Mono<PutEventsResponse> publish(Object event) {
        log.info("Sending event to EventBridge");
        return Mono.fromCallable(() ->
                        objectMapper.writeValueAsString(event))
                .flatMap(detail -> Mono.fromFuture(client.putEvents(createRequest(detail))))
                .doOnError(e -> log.error("EventBridge publish error: {}", e.getMessage()))
                .doOnNext(response -> log.info("EventBridge publish response: {}", response.entries()));
    }

    private PutEventsRequest createRequest(String detail) {
        PutEventsRequestEntry entry = PutEventsRequestEntry.builder()
                .eventBusName(configs.getEventBus().getName())
                .source(configs.getEventBus().getSource())
                .detailType(configs.getEventBus().getDetailType())
                .detail(detail)
                .build();

        return PutEventsRequest.builder()
                .entries(entry)
                .build();
    }

}