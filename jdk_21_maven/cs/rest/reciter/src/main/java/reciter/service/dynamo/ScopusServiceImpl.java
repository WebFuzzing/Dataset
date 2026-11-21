package reciter.service.dynamo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reciter.database.dynamodb.repository.ScopusArticleRepository;
import reciter.model.scopus.ScopusArticle;
import reciter.service.ScopusService;

@Service("scopusService")
@RequiredArgsConstructor
public class ScopusServiceImpl implements ScopusService {

	private final ScopusArticleRepository scopusRepository;

	@Override
	public void save(Collection<ScopusArticle> scopusArticles) {
		List<reciter.database.dynamodb.model.ScopusArticle> dbScopusArticles = new ArrayList<>();
		for (ScopusArticle scopusArticle : scopusArticles) {
			reciter.database.dynamodb.model.ScopusArticle dbScopusArticle = null;
			/*
			 * Commented out to store pubmed id as unique ID for ScopusArticle table in
			 * DynamoDb if (scopusArticle.getPubmedId() == 0) { dbScopusArticle = new
			 * reciter.database.dynamodb.model.ScopusArticle(
			 * String.valueOf(scopusArticle.getPubmedId()), scopusArticle ); } else {
			 * dbScopusArticle = new reciter.database.dynamodb.model.ScopusArticle(
			 * scopusArticle.getDoi().trim(), scopusArticle ); }
			 */

			if (scopusArticle.getPubmedId() != 0 && !dbScopusArticles.stream()
					.anyMatch(article -> article.getId().equals(String.valueOf(scopusArticle.getPubmedId())))) {
				dbScopusArticle = new reciter.database.dynamodb.model.ScopusArticle(
						String.valueOf(scopusArticle.getPubmedId()), scopusArticle);
			}
			if (dbScopusArticle != null)
				dbScopusArticles.add(dbScopusArticle);
		}
		scopusRepository.saveAll(dbScopusArticles);
	}

	@Override
	public List<ScopusArticle> findByPmids(List<String> pmids) {
		List<ScopusArticle> scopusArticles = new ArrayList<>(pmids.size());
		Iterator<reciter.database.dynamodb.model.ScopusArticle> iterator = scopusRepository.findAllById(pmids)
				.iterator();
		while (iterator.hasNext()) {
			reciter.database.dynamodb.model.ScopusArticle article = iterator.next();
			if (article != null && article.getScopusArticle() != null) {
				scopusArticles.add(article.getScopusArticle());
			}
		}
		return scopusArticles;
	}

	@Override
	public ScopusArticle findByPmid(String pmid) {
		reciter.database.dynamodb.model.ScopusArticle scopusArticle = scopusRepository.findById(pmid)
				.orElseGet(() -> null);
		if (scopusArticle != null) {
			return scopusArticle.getScopusArticle();
		}
		return null;
	}

	@Override
	public void delete() {
		scopusRepository.deleteAll();
	}
}
