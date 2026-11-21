package reciter.service.dynamo;

import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reciter.database.dynamodb.model.ApplicationUser;
import reciter.database.dynamodb.repository.ApplicationUserRepository;
import reciter.service.ApplicationUserService;

@Service("ApplicationUserService")
@RequiredArgsConstructor
public class ApplicationUserServiceImpl implements ApplicationUserService {
	
	private final ApplicationUserRepository applicationUserRepository;

	@Override
	public boolean createUser(ApplicationUser appUser) {
		if(appUser.getId() != null
				&& !appUser.getId().isBlank()
				&& appUser.getUsername() != null
				&& !appUser.getUsername().isBlank()
				&& appUser.getPassword() != null
				&& !appUser.getPassword().isBlank()) {
			             // Per-password random salt. BCrypt embeds the salt in the stored hash, so
						// authenticateUser()/checkpw() still verifies correctly. Previously a single
						// per-bean salt was reused for every account, defeating per-user salting.
						String password = BCrypt.hashpw(appUser.getPassword(), BCrypt.gensalt(10));
			appUser.setPassword(password);
			applicationUserRepository.save(appUser);
			return true;
		}
		return false;
	}

	@Override
	public boolean authenticateUser(ApplicationUser appUser) {
		if(appUser.getId() != null
				&& !appUser.getId().isBlank()
				&& appUser.getPassword() != null
				&& !appUser.getPassword().isBlank()) {
			ApplicationUser validUser = applicationUserRepository.findById(appUser.getId().trim()).orElseGet(() -> null);
			if(validUser == null) {
				return false;
			}
			if(validUser != null) {
				if(BCrypt.checkpw(appUser.getPassword(), validUser.getPassword())) {
					return true;
				}
			}
		}
		return false;
	}
	
}
