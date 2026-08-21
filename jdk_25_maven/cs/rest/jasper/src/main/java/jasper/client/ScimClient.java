package jasper.client;

import com.unboundid.scim2.common.messages.ListResponse;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import jasper.component.dto.ScimPatchOp;
import jasper.component.dto.ScimUserResource;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Profile;

import java.net.URI;
import java.util.Map;

@Profile("scim")
@FeignClient(value = "scim")
public interface ScimClient {
	@RequestLine("POST /Users")
	@Headers({"Content-Type: application/scim+json", "Authorization: Bearer {accessCode}"})
	ScimUserResource createUser(URI baseUri, @Param("accessCode") String accessCode, ScimUserResource user);

	@RequestLine("GET /Users?filter=userName eq \"{tag}\"")
	@Headers("Authorization: Bearer {accessCode}")
	ListResponse<Map<String, Object>> getUser(URI baseUri, @Param("accessCode") String accessCode, @Param("tag") String tag);

	@RequestLine("GET /Users?startIndex={startIndex}&count={count}")
	@Headers("Authorization: Bearer {accessCode}")
	ListResponse<Map<String, Object>> getUsers(URI baseUri, @Param("accessCode") String accessCode, @Param("startIndex") int startIndex, @Param("count") int count);

	@RequestLine("PUT /Users/{id}")
	@Headers({"Content-Type: application/scim+json", "Authorization: Bearer {accessCode}"})
	ScimUserResource updateUser(URI baseUri, @Param("accessCode") String accessCode, @Param("id") String id, ScimUserResource user);

	@RequestLine("PATCH /Users/{id}")
	@Headers({"Content-Type: application/scim+json", "Authorization: Bearer {accessCode}"})
	ScimUserResource patchUser(URI baseUri, @Param("accessCode") String accessCode, @Param("id") String id, ScimPatchOp patch);

	@RequestLine("DELETE /Users/{id}")
	@Headers({"Content-Type: application/scim+json", "Authorization: Bearer {accessCode}"})
	void deleteUser(URI baseUri, @Param("accessCode") String accessCode, @Param("id") String id);
}
