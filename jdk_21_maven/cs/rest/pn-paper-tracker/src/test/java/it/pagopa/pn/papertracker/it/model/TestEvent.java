package it.pagopa.pn.papertracker.it.model;

import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import lombok.Data;

@Data
public class TestEvent {
    private String messageId;
    private Integer ocrResponseIdx;
    private PaperProgressStatusEvent analogMail;
}
