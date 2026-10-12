package it.pagopa.pn.papertracker.service.handler_step.AR;

import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.service.handler_step.generic.GenericDematValidator;
import it.pagopa.pn.papertracker.utils.OcrUtility;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class DematValidatorAr extends GenericDematValidator implements HandlerStep {

    public DematValidatorAr(OcrUtility ocrUtility, PaperTrackerErrorService paperTrackerErrorService) {
        super(ocrUtility, paperTrackerErrorService);
    }
}
