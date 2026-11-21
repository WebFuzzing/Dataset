package reciter.service;

import java.util.Collection;
import java.util.List;

import reciter.database.dynamodb.model.NameFrequency;

public interface NameFrequencyService {

	void save(NameFrequency nameFrequency);

	void save(Collection<NameFrequency> nameFrequencies);

	NameFrequency findByName(String name);

	List<NameFrequency> findAll();

	void deleteAll();

	void delete(String name);

	long getItemCount();

}
