package reciter.algorithm.evidence.article.acceptedrejected;

import java.util.List;

import reciter.algorithm.evidence.article.ReCiterArticleStrategy;
import reciter.algorithm.evidence.article.ReCiterArticleStrategyContext;
import reciter.model.article.ReCiterArticle;

public class AcceptedRejectedStrategyContext implements ReCiterArticleStrategyContext {
	
	private final ReCiterArticleStrategy strategy;
	
	public AcceptedRejectedStrategyContext(ReCiterArticleStrategy strategy) {
		this.strategy = strategy;
	}

	@Override
	public double executeStrategy(ReCiterArticle reCiterArticle, ReCiterArticle otherReCiterArticle) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles, ReCiterArticle otherReCiterArticle) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double executeStrategy(List<ReCiterArticle> reCiterArticles) {
		// TODO Auto-generated method stub
		return strategy.executeStrategy(reCiterArticles);
	}

}
