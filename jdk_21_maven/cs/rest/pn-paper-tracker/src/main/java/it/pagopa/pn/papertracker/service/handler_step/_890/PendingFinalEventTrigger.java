package it.pagopa.pn.papertracker.service.handler_step._890;

import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.BusinessState;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Event;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.utils.TrackerUtility;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.ArrayList;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECAG012;

@Component
@RequiredArgsConstructor
@Slf4j
public class PendingFinalEventTrigger implements HandlerStep {

    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("PendingFinalEventTrigger executed for trackingId: {}", context.getTrackingId());
        Event event = TrackerUtility.extractEventFromContext(context);
        if(RECAG012.name().equalsIgnoreCase(event.getStatusCode()) && context.getPaperTrackings().getBusinessState().equals(BusinessState.AWAITING_REFINEMENT_OCR)) {
            context.setEventId(context.getPaperTrackings().getPendingFinalEventId());
            context.setEventsToSend(new ArrayList<>());
        }else{
            context.setStopExecution(true);
        }
        return Mono.empty();
    }
}
