package jasper.component.dto;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.unboundid.scim2.common.types.Email;
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
public class ScimUserResource {
	@Builder.Default
	private List<String> schemas = List.of("urn:ietf:params:scim:schemas:core:2.0:User");
	private String id;
	@Builder.Default
	private boolean active = true;
	private String userName;
	private String password;
	private ObjectNode customClaims;
	private List<Email> emails;
}
