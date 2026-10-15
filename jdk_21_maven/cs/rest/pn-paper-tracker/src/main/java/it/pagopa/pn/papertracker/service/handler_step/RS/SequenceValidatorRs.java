package it.pagopa.pn.papertracker.service.handler_step.RS;

import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.service.PaperTrackerErrorService;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.service.handler_step.generic.GenericSequenceValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SequenceValidatorRs extends GenericSequenceValidator implements HandlerStep {

    public SequenceValidatorRs(PaperTrackingsDAO paperTrackingsDAO, PaperTrackerErrorService paperTrackerErrorService) {
        super(paperTrackingsDAO, paperTrackerErrorService);
    }
}
