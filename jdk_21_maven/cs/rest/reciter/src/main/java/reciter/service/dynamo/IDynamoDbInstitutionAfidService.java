package reciter.service.dynamo;

import reciter.database.dynamodb.model.InstitutionAfid;

import java.util.Collection;
import java.util.List;

public interface IDynamoDbInstitutionAfidService {
    void save(Collection<InstitutionAfid> institutionAfids);
    InstitutionAfid findByInstitution(String institution);
	List<InstitutionAfid> findAll();
	long getItemCount();
}
