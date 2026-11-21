package reciter.database.dynamodb.repository;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.ESearchCount;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class ESearchCountRepository  {
	
	private final DynamoDbTable<ESearchCount> eSearchCountTable;
	
	public ESearchCountRepository(DynamoDbEnhancedClient enhancedClient) {
		this.eSearchCountTable = enhancedClient.table("ESearchCount", TableSchema.fromBean(ESearchCount.class));
	}
	
	
	public void save(ESearchCount eSearchCount) {
		eSearchCountTable.putItem(eSearchCount);
	}
	
	public Optional<ESearchCount> findById(String id) {
		return Optional.ofNullable(eSearchCountTable.getItem(r -> r.key(k -> k.partitionValue(id))));
	}
}
