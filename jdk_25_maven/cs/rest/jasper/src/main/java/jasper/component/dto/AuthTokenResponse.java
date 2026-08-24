package jasper.component.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthTokenResponse {
	private String access_token;
	private String scope;
	private String token_type;
	private String expires_in;
}
