package it.pagopa.pn.papertracker.service.handler_step.RS;

import it.pagopa.pn.papertracker.model.HandlerContext;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.service.handler_step.generic.GenericDematValidator;
import it.pagopa.pn.papertracker.utils.OcrUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECRS001C;
import static it.pagopa.pn.papertracker.model.EventStatusCodeEnum.RECRS003C;

@Component
@Slf4j
public class DematValidatorRs extends GenericDematValidator implements HandlerStep {

    public DematValidatorRs(OcrUtility ocrUtility, PaperTrackerErrorService paperTrackerErrorService) {
        super(ocrUtility, paperTrackerErrorService);
    }

    @Override
    public Mono<Void> execute(HandlerContext context) {
        log.info("Starting DematValidatorRs execution for trackingId={}", context.getTrackingId());
        String statusCode = context.getPaperProgressStatusEvent().getStatusCode();
        if (RECRS001C.name().equals(statusCode) || RECRS003C.name().equals(statusCode)) {
            log.info("StatusCode excluded from validateDemat: {}", statusCode);
            return Mono.empty();
        }
        return validateDemat(context);
    }
}