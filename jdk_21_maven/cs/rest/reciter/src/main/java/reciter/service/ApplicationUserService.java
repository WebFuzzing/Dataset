package reciter.service;

import reciter.database.dynamodb.model.ApplicationUser;

public interface ApplicationUserService {
	
	boolean createUser(ApplicationUser appUser);
	
	boolean authenticateUser(ApplicationUser appUser);
}
