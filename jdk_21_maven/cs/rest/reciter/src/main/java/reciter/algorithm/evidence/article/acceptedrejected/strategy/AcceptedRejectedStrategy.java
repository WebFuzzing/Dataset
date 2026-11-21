package reciter.algorithm.evidence.article.acceptedrejected.strategy;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.evidence.article.AbstractReCiterArticleStrategy;
import reciter.engine.analysis.evidence.AcceptedRejectedEvidence;
import reciter.model.article.ReCiterArticle;

/**
 * @author szd2013
 * This class is in line with https://github.com/wcmc-its/ReCiter/issues/231
 */
public class AcceptedRejectedStrategy extends AbstractReCiterArticleStrategy {
	
	private static final Logger slf4jLogger = LoggerFactory.getLogger(AcceptedRejectedStrategy.class);

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
				AcceptedRejectedEvidence acceptedRejectedEvidence = new AcceptedRejectedEvidence();
				/*if(reCiterArticle.getGoldStandard() == 1) {
					acceptedRejectedEvidence.setFeedbackScoreAccepted(ReCiterArticleScorer.strategyParameters.getAcceptedArticleScore());
				} else if(reCiterArticle.getGoldStandard() == -1) {
					acceptedRejectedEvidence.setFeedbackScoreRejected(ReCiterArticleScorer.strategyParameters.getRejectedArticleScore());
				} else if(reCiterArticle.getGoldStandard() == 0) {
					acceptedRejectedEvidence.setFeedbackScoreNull(ReCiterArticleScorer.strategyParameters.getFeedbackScoreNullScore());
				}*/
				
				if(acceptedRejectedEvidence != null) {
					slf4jLogger.info("Pmid: " + reCiterArticle.getArticleId() + " " + acceptedRejectedEvidence);
					reCiterArticle.setAcceptedRejectedEvidence(acceptedRejectedEvidence);
				}
			});
		return 0;
	}

}
