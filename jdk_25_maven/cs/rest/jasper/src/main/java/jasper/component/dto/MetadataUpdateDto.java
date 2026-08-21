package jasper.component.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_EMPTY;

@Getter
@Setter
@JsonInclude(NON_EMPTY)
public class MetadataUpdateDto implements Serializable {
	private String modified = Instant.now().toString();
	private int responses;
	private int internalResponses;
	private Map<String, Integer> plugins;
	private boolean obsolete;
}
