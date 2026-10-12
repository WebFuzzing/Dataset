package it.pagopa.pn.papertracker.middleware.queue.model;

import it.pagopa.pn.api.dto.events.GenericEvent;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import it.pagopa.pn.papertracker.generated.openapi.msclient.externalchannel.model.SingleStatusUpdate;
import it.pagopa.pn.papertracker.generated.openapi.msclient.paperchannel.model.PaperChannelUpdate;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
public class ExternalChannelEvent implements GenericEvent<CustomEventHeader, SingleStatusUpdate> {

    private CustomEventHeader header;
    private SingleStatusUpdate payload;

}
