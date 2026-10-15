package it.pagopa.pn.papertracker.model;

import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataResultPayload;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.PaperProgressStatusEvent;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.SendEvent;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class HandlerContext {

    private PaperProgressStatusEvent paperProgressStatusEvent;
    private String trackingId;
    private PaperTrackings paperTrackings;
    private String anonymizedDiscoveredAddressId;
    private List<SendEvent> eventsToSend = new ArrayList<>();
    private boolean stopExecution = false;
    private String eventId;
    private String finalStatusCode;
    private boolean dryRunEnabled;
    private Long messageReceiveCount;
    private boolean needToSendRECAG012A = false;
    private String reworkId;
    private OcrDataResultPayload ocrDataResultPayload;
    private boolean isRedrive;
    private String nextRequestIdPcRetry;
}
