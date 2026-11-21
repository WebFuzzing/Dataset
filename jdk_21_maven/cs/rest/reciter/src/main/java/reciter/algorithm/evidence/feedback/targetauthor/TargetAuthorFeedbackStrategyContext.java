package reciter.algorithm.evidence.feedback.targetauthor;

import java.util.List;

import reciter.algorithm.evidence.StrategyContext;
import reciter.model.article.ReCiterArticle;
import reciter.model.identity.Identity;

public interface TargetAuthorFeedbackStrategyContext extends StrategyContext{
	
	double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity);
	double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity);
}
