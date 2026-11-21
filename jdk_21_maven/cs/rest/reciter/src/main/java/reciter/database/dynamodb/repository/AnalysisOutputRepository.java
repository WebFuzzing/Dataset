package reciter.database.dynamodb.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.AnalysisOutput;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class AnalysisOutputRepository {

	private final DynamoDbTable<AnalysisOutput> analysisTable;

	public AnalysisOutputRepository(DynamoDbEnhancedClient enhancedClient) {
		this.analysisTable = enhancedClient.table("Analysis", TableSchema.fromBean(AnalysisOutput.class));
	}

	public void save(AnalysisOutput analysisOutput) {
		analysisTable.putItem(analysisOutput);
	}

	public void saveAll(List<AnalysisOutput> analysisOutputs) {
		analysisOutputs.forEach(entity -> analysisTable.putItem(entity));
	}

	public Optional<AnalysisOutput> findById(String id) {
		return Optional.ofNullable(analysisTable.getItem(r -> r.key(k -> k.partitionValue(id))));
	}

	public Iterable<AnalysisOutput> findAll() {
		return analysisTable.scan().items();
	}

	public void deleteById(String id) {
		AnalysisOutput analysisOutput = new AnalysisOutput();
		analysisOutput.setUid(id);
		analysisTable.deleteItem(analysisOutput);
	}

	public void deleteAll() {
		analysisTable.scan().items().forEach(entity -> analysisTable.deleteItem(entity));
	}

	@SuppressWarnings("unused")
	public long getItemCount() {
		long count = 0;

		for (AnalysisOutput item : analysisTable.scan().items()) {
			count++;
		}
		return count;
	}

	public List<AnalysisOutput> findAllById(List<String> uids) {
		List<AnalysisOutput> analysisOutputs = new ArrayList<>();
		for (String uid : uids) {
			AnalysisOutput analysisOutput = analysisTable.getItem(r -> r.key(k -> k.partitionValue(uid)));
			if (analysisOutput != null) {
				analysisOutputs.add(analysisOutput);
			}
		}
		return analysisOutputs;
	}
}
