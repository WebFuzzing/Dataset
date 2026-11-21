package reciter.algorithm.evidence.feedback.targetauthor;

import java.util.List;

import reciter.model.article.ReCiterArticle;
import reciter.model.identity.Identity;

public interface TargetAuthorFeedbackStrategy {

	double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity);
	double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity);
}

