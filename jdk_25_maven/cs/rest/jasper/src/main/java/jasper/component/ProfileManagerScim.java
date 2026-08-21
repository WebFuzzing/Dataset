package jasper.component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.unboundid.scim2.common.types.Email;
import io.micrometer.core.annotation.Timed;
import jasper.client.ScimClient;
import jasper.component.dto.ScimPatchOp;
import jasper.component.dto.ScimUserResource;
import jasper.config.Props;
import jasper.security.Auth;
import jasper.service.dto.ProfileDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static jasper.security.AuthoritiesConstants.PRIVATE;

@Profile("scim")
@Component
public class ProfileManagerScim implements ProfileManager {
	private static final Logger logger = LoggerFactory.getLogger(ProfileManagerScim.class);

	@Autowired
	Props props;

	@Autowired
	Auth auth;

	@Autowired
	ScimClient scimClient;

	@Autowired
	AccessToken accessToken;

	@Autowired
	ObjectMapper objectMapper;

	@Override
	@Timed(value = "jasper.scim", histogram = true)
	public void createUser(String userName, String password, String[] roles) {
		var user = ScimUserResource.builder()
			.userName(userName)
			.password(password)
			.customClaims(getClaims(roles))
			.emails(List.of(new Email().setValue(userName + "@jasper.local")))
			.build();
		scimClient.createUser(baseUri(), accessToken.getAdminToken(), user);
	}

	private URI baseUri() {
		try {
			return new URI(auth.security().getScimEndpoint());
		} catch (URISyntaxException e) {
			throw new RuntimeException(e);
		}
	}

	private ObjectNode getClaims(String[] roles) {
		var claims = objectMapper.createObjectNode();
		return claims.set(auth.security().getAuthoritiesClaim(), new TextNode(String.join(",", roles)));
	}

	private String[] getRoles(ScimUserResource user) {
		if (user.getCustomClaims() != null &&
			user.getCustomClaims().get(auth.security().getAuthoritiesClaim()) != null) {
			return user.getCustomClaims().get(auth.security().getAuthoritiesClaim()).asText().split(",");
		}
		return new String[] { props.getDefaultRole(), auth.security().getDefaultRole() };
	}

	@Override
	@Timed(value = "jasper.scim", histogram = true)
	public ProfileDto getUser(String userName) {
		return mapUser(_getUser(userName));
	}

	private ProfileDto mapUser(ScimUserResource user) {
		var result = new ProfileDto();
		result.setActive(user.isActive());
		var roles = Arrays.asList(getRoles(user));
		for (var role : roles) {
			if (!role.equals(PRIVATE)) {
				result.setRole(role);
				break;
			}
		}
		if (roles.contains(PRIVATE)) {
			result.setTag("_user/" + user.getUserName());
		} else {
			result.setTag("+user/" + user.getUserName());
		}
		return result;
	}

	private ScimUserResource _getUser(String userName) {
		return objectMapper.convertValue(
			scimClient.getUser(baseUri(), accessToken.getAdminToken(), userName).getResources().get(0),
			ScimUserResource.class);
	}

	@Override
	@Timed(value = "jasper.scim", histogram = true)
	public Page<ProfileDto> getUsers(String origin, int page, int size) {
		var users = scimClient.getUsers(baseUri(), accessToken.getAdminToken(), page + 1, size);
		return new PageImpl<>(
			users.getResources(),
			PageRequest.of(users.getStartIndex() - 1, users.getItemsPerPage()),
			users.getTotalResults())
			.map(m -> objectMapper.convertValue(m, ScimUserResource.class))
			.map(u -> mapUser(u));
	}

	@Override
	@Timed(value = "jasper.scim", histogram = true)
	public void changePassword(String userName, String password) {
		var id = (String) scimClient
			.getUser(baseUri(), accessToken.getAdminToken(), userName).getResources()
			.get(0)
			.get("id");
		var patch = ScimPatchOp.builder().operations(List.of(
			ScimPatchOp.Operation.builder()
				.op("replace")
				.path("password")
				.value(password)
				.build()
		)).build();
		scimClient.patchUser(baseUri(), accessToken.getAdminToken(), id, patch);
	}

	@Override
	@Timed(value = "jasper.scim", histogram = true)
	public void setActive(String userName, boolean active) {
		var user = _getUser(userName);
		var patch = ScimPatchOp.builder().operations(List.of(
			ScimPatchOp.Operation.builder()
				.op("replace")
				.path("active")
				.value(active)
				.build()
		)).build();
		scimClient.patchUser(baseUri(), accessToken.getAdminToken(), user.getId(), patch);
	}

	@Override
	@Timed(value = "jasper.scim", histogram = true)
	public void changeRoles(String userName, String[] roles) {
		var user = _getUser(userName);
		var claims = user.getCustomClaims();
		if (claims == null || claims.isNull()) {
			claims = getClaims(roles);
		} else {
			claims.set(auth.security().getAuthoritiesClaim(), new TextNode(String.join(",", roles)));
		}
		var patch = ScimPatchOp.builder().operations(List.of(
			ScimPatchOp.Operation.builder()
				.op("replace")
				.path("customClaims")
				.value(claims)
				.build()
			)).build();
		scimClient.patchUser(baseUri(), accessToken.getAdminToken(), user.getId(), patch);
	}

	@Override
	@Timed(value = "jasper.scim", histogram = true)
	public void deleteUser(String userName) {
		var user = _getUser(userName);
		scimClient.deleteUser(baseUri(), accessToken.getAdminToken(), user.getId());
	}
}
