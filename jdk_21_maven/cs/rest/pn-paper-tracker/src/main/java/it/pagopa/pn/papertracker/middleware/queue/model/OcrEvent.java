package it.pagopa.pn.papertracker.middleware.queue.model;

import com.sngular.apigenerator.asyncapi.business_model.model.event.OcrDataPayloadDTO;
import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OcrEvent implements GenericEvent<GenericEventHeader, OcrDataPayloadDTO> {

    private GenericEventHeader header;
    private OcrDataPayloadDTO payload;

    @Override
    public GenericEventHeader getHeader() {
        return header;
    }

    @Override
    public OcrDataPayloadDTO getPayload() {
        return payload;
    }
}
