package it.pagopa.pn.papertracker.service.handler_step.RIR;

import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.Event;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.model.DeliveryFailureCauseEnum;
import it.pagopa.pn.papertracker.model.EventStatusCodeEnum;
import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.service.handler_step.generic.GenericSequenceValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static it.pagopa.pn.papertracker.utils.TrackerUtility.idRECRI004XEvent;

@Component
@Slf4j
public class SequenceValidatorRir extends GenericSequenceValidator implements HandlerStep {

    public SequenceValidatorRir(PaperTrackingsDAO paperTrackingsDAO, PaperTrackerErrorService paperTrackerErrorService) {
        super(paperTrackingsDAO, paperTrackerErrorService);
    }

    @Override
    protected Mono<List<Event>> validateDeliveryFailureCause(List<Event> events, PaperTrackings paperTrackings, HandlerContext context, Boolean strictFinalEventValidation) {
        log.info("Beginning validation for delivery failure cause for events : {}", events);
        return Flux.fromIterable(events)
                .flatMap(event -> {
                    EventStatusCodeEnum statusCodeEnum = EventStatusCodeEnum.fromKey(event.getStatusCode());
                    List<DeliveryFailureCauseEnum> allowedCauses = statusCodeEnum.getDeliveryFailureCauseList();
                    if(idRECRI004XEvent(event) && !StringUtils.hasText(event.getDeliveryFailureCause())){
                        return Mono.empty();
                    }else {
                        return super.checkPresenceOfCause(event, context, paperTrackings, allowedCauses, strictFinalEventValidation);
                    }
                })
                .flatMap(event -> {
                    EventStatusCodeEnum statusCodeEnum = EventStatusCodeEnum.fromKey(event.getStatusCode());
                    List<DeliveryFailureCauseEnum> allowedCauses = statusCodeEnum.getDeliveryFailureCauseList();
                    return super.checkIfIsValidCause(context, paperTrackings, strictFinalEventValidation, allowedCauses, event)
                            .flatMap(unused -> {
                                if(idRECRI004XEvent(event) && !StringUtils.hasText(event.getDeliveryFailureCause())){
                                    return Mono.empty();
                                }
                                return super.checkIfStrictValidation(context, paperTrackings, strictFinalEventValidation, allowedCauses, event);
                            });
                })
                .filter(event -> StringUtils.hasText(event.getDeliveryFailureCause()))
                .collectList()
                .filter(filteredEvents -> !CollectionUtils.isEmpty(filteredEvents))
                .flatMap(filteredEvents -> super.allDeliveryFailureCauseAreEquals(context, paperTrackings, strictFinalEventValidation, filteredEvents))
                .thenReturn(events);
    }
}