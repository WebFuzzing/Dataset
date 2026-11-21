package reciter.service.dynamo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reciter.database.dynamodb.model.NameFrequency;
import reciter.database.dynamodb.repository.NameFrequencyRepository;
import reciter.service.NameFrequencyService;

@Service
public class NameFrequencyServiceImpl implements NameFrequencyService {

	@Autowired
	private NameFrequencyRepository nameFrequencyRepository;

	@Override
	public void save(NameFrequency nameFrequency) {
		nameFrequencyRepository.save(nameFrequency);
	}

	@Override
	public void save(Collection<NameFrequency> nameFrequencies) {
		nameFrequencyRepository.saveAll(nameFrequencies);
	}

	@Override
	public NameFrequency findByName(String name) {
		return nameFrequencyRepository.findById(name).orElse(null);
	}

	@Override
	public List<NameFrequency> findAll() {
		Iterable<NameFrequency> iterable = nameFrequencyRepository.findAll();
		List<NameFrequency> nameFrequencies = new ArrayList<>();
		Iterator<NameFrequency> iterator = iterable.iterator();
		while (iterator.hasNext()) {
			nameFrequencies.add(iterator.next());
		}
		return nameFrequencies;
	}

	@Override
	public void deleteAll() {
		nameFrequencyRepository.deleteAll();
	}

	@Override
	public void delete(String name) {
		if (nameFrequencyRepository.existsById(name)) {
			nameFrequencyRepository.deleteById(name);
		}
	}

	@Override
	public long getItemCount() {
		return nameFrequencyRepository.count();
	}

}
