package reciter.database.dynamodb.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.GoldStandard;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.Expression;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.enhanced.dynamodb.Key;
import software.amazon.awssdk.enhanced.dynamodb.model.PutItemEnhancedRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.ConditionalCheckFailedException;

@Repository
public class DynamoDbGoldStandardRepository {

	private final DynamoDbTable<GoldStandard> goldStandardTable;

	public DynamoDbGoldStandardRepository(DynamoDbEnhancedClient enhancedClient) {
		this.goldStandardTable = enhancedClient.table("GoldStandard", TableSchema.fromBean(GoldStandard.class));

	}

	public void save(GoldStandard goldStandard) {
		goldStandardTable.putItem(goldStandard);
	}

	public void saveAll(List<GoldStandard> goldStandards) {
		goldStandards.forEach(goldStandard -> goldStandardTable.putItem(goldStandard));
	}

	public Optional<GoldStandard> findById(String id) {
		return Optional.ofNullable(goldStandardTable.getItem(r -> r.key(k -> k.partitionValue(id))));
	}

	public Iterable<GoldStandard> findAll() {
		return goldStandardTable.scan().items();
	}

	@SuppressWarnings("unused")
	public long getItemCount() {
		long count = 0;

		for (GoldStandard goldStandard : goldStandardTable.scan().items()) {
			count++;
		}
		return count;
	}

	public List<GoldStandard> findAllById(List<String> uids) {
		List<GoldStandard> goldStandards = new ArrayList<>();
		for (String uid : uids) {
			GoldStandard goldStandard = goldStandardTable.getItem(r -> r.key(k -> k.partitionValue(uid)));
			if (goldStandard != null) {
				goldStandards.add(goldStandard);
			}
		}
		return goldStandards;
	}

	 /**
     * Delete GoldStandard by UID.
     *
     * @param uid user identifier
     */
    public void deleteById(String uid) {

        goldStandardTable.deleteItem(
                Key.builder()
                        .partitionValue(uid)
                        .build());
    }
}
