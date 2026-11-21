package reciter.algorithm.cluster.article.scorer;

import java.util.List;

import reciter.model.article.ReCiterArticle;
import reciter.model.identity.Identity;

public interface ArticleScorer {
	
	void runArticleScorer(List<ReCiterArticle> reCiterArticles, Identity identity);
	List<ReCiterArticle> executePythonScriptForArticleIdentityTotalScore(List<ReCiterArticle> reCiterArticles, Identity identity);
}
