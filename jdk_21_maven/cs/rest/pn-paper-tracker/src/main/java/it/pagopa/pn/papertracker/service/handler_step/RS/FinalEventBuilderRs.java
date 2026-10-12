package it.pagopa.pn.papertracker.service.handler_step.RS;

import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.service.handler_step.generic.GenericFinalEventBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FinalEventBuilderRs extends GenericFinalEventBuilder implements HandlerStep {

    public FinalEventBuilderRs(DataVaultClient dataVaultClient, PaperTrackingsDAO paperTrackingsDAO) {
        super(dataVaultClient, paperTrackingsDAO);
    }
}
