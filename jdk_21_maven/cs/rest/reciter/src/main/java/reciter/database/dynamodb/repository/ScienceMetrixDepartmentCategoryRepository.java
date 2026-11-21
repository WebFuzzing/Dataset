package reciter.database.dynamodb.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.ScienceMetrixDepartmentCategory;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
public class ScienceMetrixDepartmentCategoryRepository {

	private final DynamoDbTable<ScienceMetrixDepartmentCategory> scienceMetrixDepartmentCategoryTable;

	public ScienceMetrixDepartmentCategoryRepository(DynamoDbEnhancedClient enhancedClient) {
		this.scienceMetrixDepartmentCategoryTable = enhancedClient.table("ScienceMetrixDepartmentCategory",
				TableSchema.fromBean(ScienceMetrixDepartmentCategory.class));
	}

	public List<ScienceMetrixDepartmentCategory> findByScienceMetrixJournalSubfieldId(Long subfieldId) {

		return findByAttribute("scienceMetrixJournalSubfieldId", subfieldId);
	}

	public void save(ScienceMetrixDepartmentCategory scienceMetrixDepartmentCategory) {
		scienceMetrixDepartmentCategoryTable.putItem(scienceMetrixDepartmentCategory);
	}

	public void saveAll(Collection<ScienceMetrixDepartmentCategory> scienceMetrixDepartmentCategories) {
		scienceMetrixDepartmentCategories
				.forEach(scienceMetrixDepartmentCategory -> scienceMetrixDepartmentCategoryTable
						.putItem(scienceMetrixDepartmentCategory));
	}

	public Iterable<ScienceMetrixDepartmentCategory> findAll() {
		return scienceMetrixDepartmentCategoryTable.scan().items();
	}

	// temp
	public void deleteAll() {
		scienceMetrixDepartmentCategoryTable.scan().items()
				.forEach(scienceMetrixDepartmentCategory -> scienceMetrixDepartmentCategoryTable
						.deleteItem(scienceMetrixDepartmentCategory));
	}

	@SuppressWarnings("unused")
	public long count() {
		long count = 0;

		for (ScienceMetrixDepartmentCategory scienceMetrixDepartmentCategory : scienceMetrixDepartmentCategoryTable
				.scan().items()) {
			count++;
		}
		return count;
	}

	public List<ScienceMetrixDepartmentCategory> findByAttribute(String attributeName, long value) {
		Map<String, AttributeValue> expressionValues = Map.of(":value",
				AttributeValue.builder().n(String.valueOf(value)).build());

		Expression filterExpression = Expression.builder().expression(attributeName + " = :value")
				.expressionValues(expressionValues).build();

		ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder().filterExpression(filterExpression).build();

		return scienceMetrixDepartmentCategoryTable.scan(scanRequest).items().stream().collect(Collectors.toList());
	}

}