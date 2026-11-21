package reciter.service.dynamo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import reciter.database.dynamodb.model.ScienceMetrixDepartmentCategory;
import reciter.database.dynamodb.repository.ScienceMetrixDepartmentCategoryRepository;
import reciter.service.ScienceMetrixDepartmentCategoryService;

@Service
public class ScienceMetrixDepartmentCategoryServiceImpl implements ScienceMetrixDepartmentCategoryService {
	
	@Autowired
	private ScienceMetrixDepartmentCategoryRepository scienceMetrixDepartmentCategoryRepository;

	@Override
	public List<ScienceMetrixDepartmentCategory> findByScienceMetrixJournalSubfieldId(Long subfieldId) {
		return scienceMetrixDepartmentCategoryRepository.findByScienceMetrixJournalSubfieldId(subfieldId);
	}
	
	@Override
    public List<ScienceMetrixDepartmentCategory> findAll() {
    	Iterable<ScienceMetrixDepartmentCategory> scienceMetrixDeptCatgIterable = scienceMetrixDepartmentCategoryRepository.findAll();
        List<ScienceMetrixDepartmentCategory> scienceMetrixDeptCategories = new ArrayList<>();
        Iterator<ScienceMetrixDepartmentCategory> iterator = scienceMetrixDeptCatgIterable.iterator();
        while (iterator.hasNext()) {
        	ScienceMetrixDepartmentCategory scienceMetrix = iterator.next();
        	scienceMetrixDeptCategories.add(scienceMetrix);
        }
        return scienceMetrixDeptCategories;
    }
	
	@Override
    public void save(Collection<ScienceMetrixDepartmentCategory> scienceMetrixDepartmentCategories) {
        scienceMetrixDepartmentCategoryRepository.saveAll(scienceMetrixDepartmentCategories);
    }

	@Override
	public long getItemCount() {
		return scienceMetrixDepartmentCategoryRepository.count();
	}
	
	
}
