package reciter.service;

import java.util.List;

import reciter.database.dynamodb.model.Gender;

public interface GenderService {
	
	void save(Gender gender);
	
	void save(List<Gender> genders);
	
	Gender findByUid(String uid);

	List<Gender> findAll();
	
	void deleteAll();

	void delete(String uid);
	
	long getItemCount();

}
