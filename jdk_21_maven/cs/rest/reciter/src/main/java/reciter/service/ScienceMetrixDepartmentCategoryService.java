package reciter.service;

import java.util.Collection;
import java.util.List;

import reciter.database.dynamodb.model.ScienceMetrixDepartmentCategory;

public interface ScienceMetrixDepartmentCategoryService {
	List<ScienceMetrixDepartmentCategory> findByScienceMetrixJournalSubfieldId(Long subfieldId);

	void save(Collection<ScienceMetrixDepartmentCategory> scienceMetrixDepartmentCategories);
	
	long getItemCount();

	List<ScienceMetrixDepartmentCategory> findAll();
}
