package it.pagopa.pn.papertracker.middleware.queue.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.*;
import lombok.experimental.SuperBuilder;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder
public class ExternalChannelEventHeader extends GenericEventHeader {

    public static final String PN_EVENT_HEADER_DRYRUN = "dryRun";
    @JsonProperty("dryRun")
    private Boolean dryRun;

}