package reciter.algorithm.evidence.article.standardizedscore.strategy;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.evidence.article.AbstractReCiterArticleStrategy;
import reciter.model.article.ReCiterArticle;

public class StandardScoreStrategy extends AbstractReCiterArticleStrategy {
	
	private static final Logger slf4jLogger = LoggerFactory.getLogger(StandardScoreStrategy.class);
	
	//private final String[] standardizedScoreMapping = ReCiterArticleScorer.strategyParameters.getStandardizedScoreMapping().trim().split(",");
	private List<String> standardizedScores;
	
	public StandardScoreStrategy() {
		//this.standardizedScores = Arrays.asList(standardizedScoreMapping);
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
		reCiterArticles.stream().forEach(reCiterArticle -> {
			double standardizedScore = 1;
			for(int i = 0; i < this.standardizedScores.size(); i++) {
					if(i == this.standardizedScores.size() - 1) {
						if(reCiterArticle.getTotalArticleScoreNonStandardized() >= Double.parseDouble(this.standardizedScores.get(i))) {
							reCiterArticle.setTotalArticleScoreStandardized(standardizedScore);
						}
					} else if(reCiterArticle.getTotalArticleScoreNonStandardized() >= Double.parseDouble(this.standardizedScores.get(i)) 
							&&
							reCiterArticle.getTotalArticleScoreNonStandardized() < Double.parseDouble(this.standardizedScores.get(i + 1))) {
						reCiterArticle.setTotalArticleScoreStandardized(standardizedScore);
					}
					standardizedScore++;
			}
			
		});
		return 0;
	}

}
