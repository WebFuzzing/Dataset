package it.pagopa.pn.mandate.middleware.queue.consumer.event;

import it.pagopa.pn.api.dto.events.GenericFifoEvent;
import it.pagopa.pn.api.dto.events.StandardEventHeader;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Builder(toBuilder = true)
@EqualsAndHashCode
@ToString
public class PnMandateExpiredEvent implements GenericFifoEvent<StandardEventHeader, PnMandateExpiredEvent.Payload> {

    private StandardEventHeader header;

    private Payload payload;

    private String messageDeduplicationId;

    private String messageGroupId;



    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Builder(toBuilder = true)
    @EqualsAndHashCode
    @ToString
    public static class Payload {
        private String mandateId;
        private String delegatorInternalUserid;
        private String delegatorUserid;
        private String delegatorCxType;
    }


}
