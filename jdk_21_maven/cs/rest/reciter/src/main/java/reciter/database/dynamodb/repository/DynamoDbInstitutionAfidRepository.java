package reciter.database.dynamodb.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.InstitutionAfid;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class DynamoDbInstitutionAfidRepository {

	private final DynamoDbTable<InstitutionAfid> institutionAfidTable;

	public DynamoDbInstitutionAfidRepository(DynamoDbEnhancedClient enhancedClient) {
		this.institutionAfidTable = enhancedClient.table("InstitutionAfid",
				TableSchema.fromBean(InstitutionAfid.class));
	}

	public void save(InstitutionAfid institutionAfid) {
		institutionAfidTable.putItem(institutionAfid);
	}

	public void saveAll(Collection<InstitutionAfid> institutionAfids) {
		institutionAfids.forEach(institutionAfid -> institutionAfidTable.putItem(institutionAfid));
	}

	public Optional<InstitutionAfid> findById(String id) {
		return Optional.ofNullable(institutionAfidTable.getItem(r -> r.key(k -> k.partitionValue(id))));
	}

	public Iterable<InstitutionAfid> findAll() {
		return institutionAfidTable.scan().items();
	}

	@SuppressWarnings("unused")
	public long count() {
		long count = 0;

		for (InstitutionAfid institutionAfid : institutionAfidTable.scan().items()) {
			count++;
		}
		return count;
	}

}
