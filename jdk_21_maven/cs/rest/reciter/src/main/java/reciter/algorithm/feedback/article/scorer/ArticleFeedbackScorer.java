package reciter.algorithm.feedback.article.scorer;

import java.util.List;

import reciter.model.article.ReCiterArticle;
import reciter.model.identity.Identity;

public interface ArticleFeedbackScorer {

	void runFeedbackArticleScorer(List<ReCiterArticle> articles,Identity identity);
}
