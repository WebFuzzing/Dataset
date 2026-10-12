package it.pagopa.pn.papertracker.middleware.queue.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.pagopa.pn.api.dto.events.GenericEventHeader;
import lombok.*;
import lombok.experimental.SuperBuilder;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString
@SuperBuilder
public class CustomEventHeader extends GenericEventHeader {

    @JsonProperty("messageAttributes")
    private Map<String, MessageAttributeValue> messageAttributes;

}
