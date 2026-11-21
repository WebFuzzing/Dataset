package reciter.service;

import reciter.database.dynamodb.model.ScienceMetrix;

import java.util.Collection;
import java.util.List;

public interface ScienceMetrixService {
    ScienceMetrix findByEissn(String eissn);
    ScienceMetrix findByIssn(String issn);
    ScienceMetrix findBySmsid(Long smsid);
	void save(Collection<ScienceMetrix> scienceMetrix);
	long getItemCount();
	List<ScienceMetrix> findAll();
}
