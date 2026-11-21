package reciter.service;

import reciter.database.dynamodb.model.InstitutionAfid;

import java.util.List;

public interface InstitutionAfidService {

	List<String> getAfidByInstitution(String institution);
	List<InstitutionAfid> findAll();
}
