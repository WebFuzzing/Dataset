package jasper.service;

import io.micrometer.core.annotation.Timed;
import jasper.component.ProfileManager;
import jasper.errors.DeactivateSelfException;
import jasper.errors.InvalidUserProfileException;
import jasper.security.Auth;
import jasper.service.dto.ProfileDto;
import org.apache.commons.compress.utils.Sets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import java.util.Set;

import static jasper.security.AuthoritiesConstants.ADMIN;
import static jasper.security.AuthoritiesConstants.ANONYMOUS;
import static jasper.security.AuthoritiesConstants.EDITOR;
import static jasper.security.AuthoritiesConstants.MOD;
import static jasper.security.AuthoritiesConstants.PRIVATE;
import static jasper.security.AuthoritiesConstants.USER;
import static jasper.security.AuthoritiesConstants.VIEWER;

@Profile("scim")
@Service
public class ProfileService {
	/**
	 * Valid roles for a user.
	 */
	private static final Set<String> ROLES = Sets.newHashSet(ADMIN, MOD, EDITOR, USER, VIEWER, ANONYMOUS);

	@Autowired
	ProfileManager profileManager;

	@Autowired
	Auth auth;

	@PreAuthorize("@auth.rootMod()")
	@Timed(value = "jasper.service", extraTags = {"service", "profile"}, histogram = true)
	public void create(String qualifiedTag, String password, String role) {
		validateRole(role);
		if (qualifiedTag.startsWith("user/")) {
			throw new InvalidUserProfileException("User tag must be protected or private.");
		}
		if (!qualifiedTag.startsWith("_user/") && !qualifiedTag.startsWith("+user/")) {
			throw new InvalidUserProfileException("User tag must be start with user/");
		}
		profileManager.createUser(qualifiedTag.substring("+user/".length()), password, getRoles(qualifiedTag, role));
	}

	private void validateRole(String role) {
		if (role.equals(ADMIN) && !auth.hasRole(ADMIN)) {
			throw new InvalidUserProfileException("Cannot assign elevated role.");
		}
	}

	private String[] getRoles(String qualifiedTag, String role) {
		if (!ROLES.contains(role)) {
			throw new InvalidUserProfileException("Invalid role: " + role);
		}
		if (qualifiedTag.startsWith("_")) {
			return new String[]{ role, PRIVATE };
		} else {
			return new String[]{ role };
		}
	}

	@PreAuthorize("@auth.hasRole('VIEWER') and @auth.canReadTag(#qualifiedTag)")
	@Timed(value = "jasper.service", extraTags = {"service", "profile"}, histogram = true)
	public ProfileDto get(String qualifiedTag) {
		return profileManager.getUser(qualifiedTag.substring("+user/".length()));
	}

	@PreAuthorize("@auth.rootMod()")
	@Timed(value = "jasper.service", extraTags = {"service", "profile"}, histogram = true)
	public Page<ProfileDto> page(int pageNumber, int pageSize) {
		return profileManager.getUsers(auth.getOrigin(), pageNumber, pageSize);
	}

	@PreAuthorize("(@auth.isUser(#qualifiedTag) or @auth.rootMod()) and @auth.freshLogin()")
	@Timed(value = "jasper.service", extraTags = {"service", "profile"}, histogram = true)
	public void changePassword(String qualifiedTag, String password) {
		profileManager.changePassword(qualifiedTag.substring("+user/".length()), password);
	}

	@PreAuthorize("@auth.hasRole('MOD') and @auth.canWriteUserTag(#qualifiedTag)")
	@Timed(value = "jasper.service", extraTags = {"service", "profile"}, histogram = true)
	public void changeRole(String qualifiedTag, String role) {
		validateRole(role);
		profileManager.changeRoles(qualifiedTag.substring("+user/".length()), getRoles(qualifiedTag, role));
	}

	@PreAuthorize("@auth.rootMod()")
	@Timed(value = "jasper.service", extraTags = {"service", "profile"}, histogram = true)
	public void setActive(String qualifiedTag, boolean active) {
		if (!active && auth.getUserTag().tag.equals(qualifiedTag)) {
			throw new DeactivateSelfException();
		}
		profileManager.setActive(qualifiedTag.substring("+user/".length()), active);
	}

	@PreAuthorize("@auth.rootMod()")
	@Timed(value = "jasper.service", extraTags = {"service", "profile"}, histogram = true)
	public void delete(String qualifiedTag) {
		profileManager.deleteUser(qualifiedTag.substring("+user/".length()));
	}
}
