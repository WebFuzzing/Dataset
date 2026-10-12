package it.pagopa.pn.papertracker.service.handler_step.RIS;

import it.pagopa.pn.papertracker.middleware.dao.PaperTrackingsDAO;
import it.pagopa.pn.papertracker.middleware.msclient.DataVaultClient;
import it.pagopa.pn.papertracker.service.handler_step.HandlerStep;
import it.pagopa.pn.papertracker.service.handler_step.generic.GenericFinalEventBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FinalEventBuilderRis extends GenericFinalEventBuilder implements HandlerStep {

    public FinalEventBuilderRis(DataVaultClient dataVaultClient, PaperTrackingsDAO paperTrackingsDAO) {
        super(dataVaultClient, paperTrackingsDAO);
    }
}
