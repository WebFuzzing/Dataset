package reciter.service.dynamo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reciter.database.dynamodb.model.Gender;
import reciter.database.dynamodb.repository.GenderRepository;
import reciter.service.GenderService;

@Service
public class GenderServiceImpl implements GenderService {
	
	@Autowired
	private GenderRepository genderRepository;

	@Override
	public void save(Gender gender) {
		genderRepository.save(gender);
	}

	@Override
	public void save(List<Gender> genders) {
		genderRepository.saveAll(genders);
		
	}

	@Override
	public Gender findByUid(String genderSource) {
		reciter.database.dynamodb.model.Gender gender = genderRepository.findById(genderSource).orElseGet(() -> null);
        return gender;
	}

	@Override
	public List<Gender> findAll() {
		Iterable<Gender> genderIterable = genderRepository.findAll();
        List<Gender> genders = new ArrayList<>();
        Iterator<Gender> iterator = genderIterable.iterator();
        while (iterator.hasNext()) {
        	Gender gender = iterator.next();
        	genders.add(gender);
        }
        return genders;
	}

	@Override
	public void deleteAll() {
		genderRepository.deleteAll();
	}

	@Override
	public void delete(String genderSource) {
		if(genderRepository.existsById(genderSource)) {
			genderRepository.deleteById(genderSource);
		}
	}

	@Override
	public long getItemCount() {
		return genderRepository.count();
	}

}
