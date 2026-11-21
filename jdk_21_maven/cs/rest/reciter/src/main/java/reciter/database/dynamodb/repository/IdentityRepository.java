package reciter.database.dynamodb.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.Identity;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class IdentityRepository {
	private final DynamoDbTable<Identity> identityTable;

	public IdentityRepository(DynamoDbEnhancedClient enhancedClient) {
		this.identityTable = enhancedClient.table("Identity", TableSchema.fromBean(Identity.class));
	}

	public void save(Identity identity) {
		identityTable.putItem(identity);
	}

	public void saveAll(List<Identity> identities) {
		identities.forEach(identity -> identityTable.putItem(identity));
	}

	public Optional<Identity> findById(String id) {
		return Optional.ofNullable(identityTable.getItem(r -> r.key(k -> k.partitionValue(id))));
	}

	public Iterable<Identity> findAll() {
		return identityTable.scan().items();
	}

	public void deleteById(String id) {
		Identity identity = new Identity();
		identity.setUid(id);
		identityTable.deleteItem(identity);
	}

	public void deleteAll() {
		identityTable.scan().items().forEach(identity -> identityTable.deleteItem(identity));
	}

	@SuppressWarnings("unused")
	public long count() {
		long count = 0;

		for (Identity identity : identityTable.scan().items()) {
			count++;
		}
		return count;
	}

	public List<Identity> findAllById(List<String> uids) {
		List<Identity> identities = new ArrayList<>();
		for (String uid : uids) {
			Identity identity = identityTable.getItem(r -> r.key(k -> k.partitionValue(uid)));
			if(identity != null) {
				identities.add(identity);
			}
		}
		return identities;
	}
}
