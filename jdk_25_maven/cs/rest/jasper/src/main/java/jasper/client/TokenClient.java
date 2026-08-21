package jasper.client;

import feign.Body;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import jasper.component.dto.AuthTokenResponse;
import org.springframework.cloud.openfeign.FeignClient;

import java.net.URI;

@FeignClient(value = "token")
public interface TokenClient {
	@RequestLine("POST /")
	@Headers("Content-Type: application/x-www-form-urlencoded")
	@Body("client_id={clientId}&client_secret={clientSecret}&scope={scope}&grant_type=client_credentials")
	AuthTokenResponse tokenService(URI baseUri, @Param("clientId") String clientId, @Param("clientSecret") String clientSecret, @Param("scope") String scope);
}
