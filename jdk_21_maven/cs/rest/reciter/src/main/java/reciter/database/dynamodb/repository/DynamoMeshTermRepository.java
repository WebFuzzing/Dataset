package reciter.database.dynamodb.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.MeshTerm;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class DynamoMeshTermRepository {
	private final DynamoDbTable<MeshTerm> meshTermTable;

	public DynamoMeshTermRepository(DynamoDbEnhancedClient enhancedClient) {
		this.meshTermTable = enhancedClient.table("MeshTerm", TableSchema.fromBean(MeshTerm.class));
	}

	public void save(MeshTerm meshTerm) {
		meshTermTable.putItem(meshTerm);
	}

	public void saveAll(List<MeshTerm> meshTerms) {
		meshTerms.forEach(meshTerm -> meshTermTable.putItem(meshTerm));
	}

	public Optional<MeshTerm> findById(String id) {
		return Optional.ofNullable(meshTermTable.getItem(r -> r.key(k -> k.partitionValue(id))));
	}

	public Iterable<MeshTerm> findAll() {
		return meshTermTable.scan().items();
	}

	@SuppressWarnings("unused")
	public long getItemCount() {
		long count = 0;

		for (MeshTerm meshTerm : meshTermTable.scan().items()) {
			count++;
		}
		return count;
	}
}
