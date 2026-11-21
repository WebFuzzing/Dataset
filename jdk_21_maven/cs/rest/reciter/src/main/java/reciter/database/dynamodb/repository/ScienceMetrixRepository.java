package reciter.database.dynamodb.repository;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.ScienceMetrix;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.ScanEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

@Repository
public class ScienceMetrixRepository {

	private final DynamoDbTable<ScienceMetrix> scienceMetrixTable;

	public ScienceMetrixRepository(DynamoDbEnhancedClient enhancedClient) {
		this.scienceMetrixTable = enhancedClient.table("ScienceMetrix", TableSchema.fromBean(ScienceMetrix.class));
	}

	public Collection<ScienceMetrix> saveAll(Collection<ScienceMetrix> scienceMetrics) {
		scienceMetrics.forEach(scienceMetrix -> scienceMetrixTable.putItem(scienceMetrix));
		return scienceMetrics;
	}

	public List<ScienceMetrix> findAll() {
		return scienceMetrixTable.scan().items().stream().toList();
	}

	public ScienceMetrix findBySmsid(Long smsid) {
		return scienceMetrixTable.getItem(r -> r.key(k -> k.partitionValue(smsid)));
	}

	public void save(Collection<ScienceMetrix> scienceMetrics) {
		scienceMetrics.forEach(scienceMetrix -> scienceMetrixTable.putItem(scienceMetrix));

	}

	public ScienceMetrix findByEissn(String eissn) {
		return findByAttribute("eissn", eissn);
	}

	public ScienceMetrix findByIssn(String issn) {
		return findByAttribute("issn", issn);
	}

	@SuppressWarnings("unused")
	public long count() {
		long count = 0;

		for (ScienceMetrix scienceMetrix : scienceMetrixTable.scan().items()) {
			count++;
		}
		return count;
	}

	public ScienceMetrix findByAttribute(String attributeName, String value) {
		Map<String, AttributeValue> expressionValues = Map.of(":value", AttributeValue.builder().s(value).build());

		Expression filterExpression = Expression.builder().expression(attributeName + " = :value")
				.expressionValues(expressionValues).build();

		ScanEnhancedRequest scanRequest = ScanEnhancedRequest.builder().filterExpression(filterExpression).build();

		return scienceMetrixTable.scan(scanRequest).items().stream().findFirst().orElse(null);
	}

}
