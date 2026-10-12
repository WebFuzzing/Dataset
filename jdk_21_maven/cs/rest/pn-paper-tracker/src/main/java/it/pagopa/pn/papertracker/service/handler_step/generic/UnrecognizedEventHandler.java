package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.model.HandlerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
@Slf4j
@RequiredArgsConstructor
public class UnrecognizedEventHandler{

    private final MetadataUpserter metadataUpserter;

    public Mono<Void> handle(HandlerContext ctx) {
        log.info("UnrecognizedEventHandler called. No action performed just saving for trackingId {}", ctx.getTrackingId());
        return Mono.just(metadataUpserter)
                .flatMap(s -> s.execute(ctx))
                .then();
    }
}

