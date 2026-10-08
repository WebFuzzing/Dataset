package it.pagopa.pn.papertracker.service.handler_step;

import it.pagopa.pn.papertracker.model.HandlerContext;
import reactor.core.publisher.Mono;

public interface HandlerStep {
    Mono<Void> execute(HandlerContext context);
}