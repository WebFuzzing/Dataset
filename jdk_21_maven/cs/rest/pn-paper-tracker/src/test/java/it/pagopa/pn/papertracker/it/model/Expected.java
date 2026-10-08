package it.pagopa.pn.papertracker.it.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataPayloadDTO;
import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataResultPayload;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackerDryRunOutputs;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackings;
import it.pagopa.pn.papertracker.middleware.dao.dynamo.entity.PaperTrackingsErrors;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Expected {
    private List<PaperTrackingsErrors> errors;
    private List<PaperTrackerDryRunOutputs> outputs;
    private List<PaperTrackings> trackings;
    private Integer sentToOcr;
    private List<OcrDataResultPayload> ocrResultPayload;
    private List<OcrDataPayloadDTO> ocrDataPayload;
}
