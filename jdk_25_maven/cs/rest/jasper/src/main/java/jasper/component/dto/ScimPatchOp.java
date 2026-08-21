package jasper.component.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScimPatchOp {
	@Builder.Default
	private List<String> schemas = List.of("urn:ietf:params:scim:api:messages:2.0:PatchOp");
	@JsonProperty("Operations")
	private List<Operation> operations;

	@Getter
	@Setter
	@Builder
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Operation {
		private String op;
		private String path;
		private Object value;
	}

}
