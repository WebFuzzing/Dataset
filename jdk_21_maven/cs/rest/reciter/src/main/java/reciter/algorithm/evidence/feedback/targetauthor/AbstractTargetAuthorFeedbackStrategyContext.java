package reciter.algorithm.evidence.feedback.targetauthor;

import java.util.List;

import reciter.model.article.ReCiterArticle;
import reciter.model.identity.Identity;

public abstract class AbstractTargetAuthorFeedbackStrategyContext implements TargetAuthorFeedbackStrategyContext {

	private TargetAuthorFeedbackStrategy strategy;
	private double strategyScore;
	
	public AbstractTargetAuthorFeedbackStrategyContext(TargetAuthorFeedbackStrategy strategy) {
		this.strategy = strategy;
	}

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		strategyScore = strategy.executeFeedbackStrategy(reCiterArticles, identity);
		return strategyScore;
	}

	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		return strategy.executeFeedbackStrategy(reCiterArticle, identity);
	}
	
	
}
