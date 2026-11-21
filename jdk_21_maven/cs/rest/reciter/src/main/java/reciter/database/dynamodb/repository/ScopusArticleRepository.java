package reciter.database.dynamodb.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.ScopusArticle;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class ScopusArticleRepository {

	private final DynamoDbTable<ScopusArticle> scopusArticleTable;

	public ScopusArticleRepository(DynamoDbEnhancedClient enhancedClient) {
		this.scopusArticleTable = enhancedClient.table("ScopusArticle", TableSchema.fromBean(ScopusArticle.class));
	}

	public void save(ScopusArticle scopusArticle) {
		scopusArticleTable.putItem(scopusArticle);
	}

	public void saveAll(List<ScopusArticle> scopusArticles) {
		scopusArticles.forEach(scopusArticle -> scopusArticleTable.putItem(scopusArticle));
	}

	public Optional<ScopusArticle> findById(String id) {
		return Optional.ofNullable(scopusArticleTable.getItem(r -> r.key(k -> k.partitionValue(id))));
	}

	public void deleteAll() {
		scopusArticleTable.scan().items().forEach(scopusArticle -> scopusArticleTable.deleteItem(scopusArticle));
	}

	public List<ScopusArticle> findAllById(List<String> uids) {
		List<ScopusArticle> scopusArticles = new ArrayList<>();
		for (String uid : uids) {
			ScopusArticle scopusArticle = scopusArticleTable.getItem(r -> r.key(k -> k.partitionValue(uid)));
			if (scopusArticle != null) {
				scopusArticles.add(scopusArticle);
			}
		}
		return scopusArticles;
	}
}
