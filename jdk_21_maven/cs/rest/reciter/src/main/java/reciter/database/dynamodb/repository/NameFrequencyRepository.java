package reciter.database.dynamodb.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.NameFrequency;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class NameFrequencyRepository {

	private final DynamoDbTable<NameFrequency> nameFrequencyTable;
	
	public NameFrequencyRepository(DynamoDbEnhancedClient enhancedClient) {
		this.nameFrequencyTable = enhancedClient.table("NameFrequency", TableSchema.fromBean(NameFrequency.class));
	}

	public void save(NameFrequency nameFrequency) {
		nameFrequencyTable.putItem(nameFrequency);
	}
	
	public void saveAll(Collection<NameFrequency> nameFrequency) {
		nameFrequency.forEach(entity -> nameFrequencyTable.putItem(entity));
	}

	public Optional<NameFrequency> findById(String id) {
		return Optional.ofNullable(nameFrequencyTable.getItem(r -> r.key(k -> k.partitionValue(id))));
	}
	
	public Iterable<NameFrequency> findAll() {
		return nameFrequencyTable.scan().items();
	}
	
	public void deleteAll() {
		nameFrequencyTable.scan().items().forEach(entity -> nameFrequencyTable.deleteItem(entity));
	}
	
	@SuppressWarnings("unused")
	public long count() {
		long count = 0;

		for (NameFrequency item : nameFrequencyTable.scan().items()) {
			count++;
		}
		return count;
	}
	
	public void deleteById(String id) {
		NameFrequency nameFrequency = new NameFrequency();
		nameFrequency.setName(id);
		nameFrequencyTable.deleteItem(nameFrequency);
	}

	public boolean existsById(String name) {
		NameFrequency nameFrequency = nameFrequencyTable.getItem(r -> r.key(k -> k.partitionValue(name)));
		return nameFrequency != null;
	}
}
