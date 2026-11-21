package reciter.database.dynamodb.repository;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.FeedbackLog;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class FeedbackLogRepository {

	private final DynamoDbTable<FeedbackLog> feedbackLogTable;
	
	public FeedbackLogRepository(DynamoDbEnhancedClient enhancedClient) {
		this.feedbackLogTable = enhancedClient.table("FeedbackLog", TableSchema.fromBean(FeedbackLog.class));
	}
	
	public void save(FeedbackLog feedbackLog) {
		// Encapsulate the conditional check inside the save method
		Expression conditionExpression = Expression.builder()
				.expression("attribute_not_exists(sk)")
				.build();

		feedbackLogTable.putItem(r -> r.item(feedbackLog)
				.conditionExpression(conditionExpression));
	}
}
