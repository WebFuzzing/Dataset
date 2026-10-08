package it.pagopa.pn.papertracker.service.handler_step.generic;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.ProductType;
import it.pagopa.pn.papertracker.model.EventTypeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.*;

@Component
public class HandlersRegistry {
    private final Map<ProductType, AbstractHandlersFactory> registry = new EnumMap<>(ProductType.class);
    private final UnrecognizedEventHandler unrecognizedHandler;

    public HandlersRegistry(List<AbstractHandlersFactory> factories, UnrecognizedEventHandler unrecognizedHandler) {
        factories.forEach(factory -> registry.put(factory.getProductType(), factory));
        this.unrecognizedHandler = unrecognizedHandler;
    }

    public Mono<Void> handleEvent(ProductType productType, EventTypeEnum eventType, HandlerContext ctx) {
        return Optional.ofNullable(registry.get(productType))
                .map(abstractHandlersFactory -> abstractHandlersFactory.build(eventType, ctx))
                .map(handler -> handler.execute(ctx))
                .orElseGet(() -> unrecognizedHandler.handle(ctx));
    }
}
