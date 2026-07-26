package jasper.component;

import jasper.service.dto.ProfileDto;
import org.springframework.data.domain.Page;

/**
 * Interface for managing external user sources.
 *
 * Externals users will not be queried during authorization or authentication.
 * This must be set by the external source customizing the JWT token
 * or HTTP headers as applicable.
 */
public interface ProfileManager {
	void createUser(String userName, String password, String[] roles);
	ProfileDto getUser(String userName);
	Page<ProfileDto> getUsers(String origin, int page, int size);
	void changePassword(String userName, String password);
	void setActive(String userName, boolean active);
	void changeRoles(String userName, String[] roles);
	void deleteUser(String userName);
}
