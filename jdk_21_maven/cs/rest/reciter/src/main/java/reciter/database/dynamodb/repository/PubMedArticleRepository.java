package reciter.database.dynamodb.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import reciter.database.dynamodb.model.PubMedArticle;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbTable;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class PubMedArticleRepository {

	private final DynamoDbTable<PubMedArticle> pubMedArticleTable;

	public PubMedArticleRepository(DynamoDbEnhancedClient enhancedClient) {
		this.pubMedArticleTable = enhancedClient.table("PubMedArticle", TableSchema.fromBean(PubMedArticle.class));
	}

	public void save(PubMedArticle pubMedArticle) {
		pubMedArticleTable.putItem(pubMedArticle);
	}

	public void saveAll(List<PubMedArticle> pubMedArticles) {
		pubMedArticles.forEach(pubMedArticle -> pubMedArticleTable.putItem(pubMedArticle));
	}

	public Optional<PubMedArticle> findById(Long pmid) {
		return Optional.ofNullable(pubMedArticleTable.getItem(r -> r.key(k -> k.partitionValue(pmid))));
	}

	@SuppressWarnings("unused")
	public long getItemCount() {
		long count = 0;

		for (PubMedArticle pubMedArticle : pubMedArticleTable.scan().items()) {
			count++;
		}
		return count;
	}

	public List<PubMedArticle> findAllById(List<Long> pmids) {
		List<PubMedArticle> pubMedArticles = new ArrayList<>();
		for (Long pmid : pmids) {
			PubMedArticle pubMedArticle = pubMedArticleTable.getItem(r -> r.key(k -> k.partitionValue(pmid)));
			if (pubMedArticle != null) {
				pubMedArticles.add(pubMedArticle);
			}
		}
		return pubMedArticles;
	}
}
