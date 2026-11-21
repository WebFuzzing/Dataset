package reciter.service.dynamo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reciter.database.dynamodb.model.ScienceMetrix;
import reciter.database.dynamodb.repository.ScienceMetrixRepository;
import reciter.service.ScienceMetrixService;

@Service
public class ScienceMetrixServiceImpl implements ScienceMetrixService {

    @Autowired
    private ScienceMetrixRepository scienceMetrixRepository;

    @Override
    public ScienceMetrix findByEissn(String eissn) {
        return scienceMetrixRepository.findByEissn(eissn);
    }

    @Override
    public ScienceMetrix findByIssn(String issn) {
        return scienceMetrixRepository.findByIssn(issn);
    }

    @Override
    public ScienceMetrix findBySmsid(Long smsid) {
        return scienceMetrixRepository.findBySmsid(smsid);
    }
    
    @Override
    public List<ScienceMetrix> findAll() {
    	Iterable<ScienceMetrix> scienceMetrixIterable = scienceMetrixRepository.findAll();
        List<ScienceMetrix> scienceMetrixJournals = new ArrayList<>();
        Iterator<ScienceMetrix> iterator = scienceMetrixIterable.iterator();
        while (iterator.hasNext()) {
        	ScienceMetrix scienceMetrix = iterator.next();
        	scienceMetrixJournals.add(scienceMetrix);
        }
        return scienceMetrixJournals;
    }
    
    @Override
    public void save(Collection<ScienceMetrix> scienceMetrix) {
    	scienceMetrixRepository.saveAll(scienceMetrix);
    }

	@Override
	public long getItemCount() {
		return scienceMetrixRepository.count();
	}
}
