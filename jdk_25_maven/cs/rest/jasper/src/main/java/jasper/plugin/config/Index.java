package jasper.plugin.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.jackson.Jacksonized;

import java.io.Serializable;

@Getter
@Setter
@Jacksonized @Builder
@JsonInclude(Include.NON_NULL)
public class Index implements Serializable {
	@Builder.Default
	private boolean tags = true;
	@Builder.Default
	private boolean sources = true;
	@Builder.Default
	private boolean alts = true;
	@Builder.Default
	private boolean fulltext = true;
	@Builder.Default
	private boolean published = true;
	@Builder.Default
	private boolean modified = true;
}
