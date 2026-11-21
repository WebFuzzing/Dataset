package reciter.database.dynamodb.repository;

import java.util.List;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.CwidSkipAudit;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.enhanced.dynamodb.model.QueryConditional;

@Repository
public class CwidSkipAuditRepository {

	private final DynamoDbTable<CwidSkipAudit> cwidSkipAuditTable;

	public CwidSkipAuditRepository(DynamoDbEnhancedClient enhancedClient) {
		this.cwidSkipAuditTable = enhancedClient.table("CwidSkipAudit", TableSchema.fromBean(CwidSkipAudit.class));
	}

	public void save(CwidSkipAudit cwidSkipAudit) {
		// Write-once: cwid + eventTimestamp identifies a single skip event, so the
		// put is conditioned on neither key attribute already existing; a repeat
		// POST for the same job run throws ConditionalCheckFailedException instead
		// of silently overwriting the original record.
		PutItemEnhancedRequest<CwidSkipAudit> request = PutItemEnhancedRequest.builder(CwidSkipAudit.class)
				.item(cwidSkipAudit)
				.conditionExpression(Expression.builder()
						.expression("attribute_not_exists(cwid) AND attribute_not_exists(eventTimestamp)").build())
				.build();
		cwidSkipAuditTable.putItem(request);
	}

	public List<CwidSkipAudit> findByCwid(String cwid) {
		// Uses QueryConditional to query by Partition Key (Hash Key) safely; results
		// come back naturally sorted by the eventTimestamp sort key.
		QueryConditional queryConditional = QueryConditional.keyEqualTo(k -> k.partitionValue(cwid));

		return cwidSkipAuditTable.query(r -> r.queryConditional(queryConditional).scanIndexForward(true)).items()
				.stream().toList();
	}
}
