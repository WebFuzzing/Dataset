package reciter.algorithm.evidence.feedback.targetauthor;

import java.text.DecimalFormat;
import java.util.List;

import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.identity.Identity;

public class AbstractTargetAuthorFeedbackStrategy implements TargetAuthorFeedbackStrategy {

	protected final int ACCEPTED = 1;
	protected final int REJECTED = -1;
	protected DecimalFormat decimalFormat = new DecimalFormat("#.######");

	// Informed absence configuration (set by ReciterFeedbackArticleScorer)
	private boolean informedAbsenceEnabled = false;
	private double informedAbsenceScale = 10.0;
	private double informedAbsenceStrength = 1.0;

	public void setInformedAbsenceConfig(boolean enabled, double scale, double strength) {
		this.informedAbsenceEnabled = enabled;
		this.informedAbsenceScale = scale;
		this.informedAbsenceStrength = strength;
	}

	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		return 0;
	}

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		return 0;
	}

	// Helper method to compute score
	protected double computeScore(int countAccepted, int countRejected) {
		return (1 / (1 + Math.exp(-(countAccepted - countRejected) / (Math.sqrt(countAccepted + countRejected) + 1))))
				- 0.5;
	}

	/**
	 * Compute informed absence penalty for a feedback feature.
	 *
	 * When a specific attribute value has zero accepted and zero rejected records,
	 * but the researcher has substantial overall acceptance history, the zero score
	 * is evidence of absence rather than absence of evidence.
	 *
	 * @param totalAccepted total number of accepted articles for this researcher
	 * @return negative penalty scaled by total accepted count and feature strength,
	 *         or 0.0 if informed absence is disabled or totalAccepted is 0
	 */
	protected double computeInformedAbsencePenalty(int totalAccepted) {
		if (!informedAbsenceEnabled || totalAccepted == 0) {
			return 0.0;
		}
		// sigmoid(totalAccepted / scale) - 0.5 gives a value in (0, 0.5)
		// Negate it so penalty is negative; multiply by strength
		double sigmoidValue = 1.0 / (1.0 + Math.exp(-totalAccepted / informedAbsenceScale));
		return informedAbsenceStrength * -(sigmoidValue - 0.5);
	}
	
	protected double determineFeedbackScore(int goldStandard, double scoreWithout1Accepted, double scoreWithout1Rejected, double scoreAll) 
	{
	    double feedbackScore=0.0; 
        try {
        	if(goldStandard == 1)
        		feedbackScore = scoreWithout1Accepted;
        	else if(goldStandard == -1)
        		feedbackScore = scoreWithout1Rejected;
        	else
        		feedbackScore = scoreAll;
        
        } catch (Exception e) {
            e.printStackTrace();
        }
        return feedbackScore;
	    
	}
	
	protected ReCiterArticleFeedbackScore populateArticleFeedbackScore(long articleId,String feedbackScoreFieldValue, int countAccepted,int countRejected,double scoreAll,double scoreWithout1Accepted,double scoreWithout1Rejected, int goldStandard,double feedbackScore,String exportedFeedbackScore,String feedbackScoreType)
	{
		ReCiterArticleFeedbackScore reciterArticleFeedbackScore = new ReCiterArticleFeedbackScore();
		reciterArticleFeedbackScore.setGoldStandard(goldStandard);
		reciterArticleFeedbackScore.setArticleId(articleId);
		reciterArticleFeedbackScore.setFeedbackScoreType(feedbackScoreType);
		reciterArticleFeedbackScore.setFeedbackScoreFieldValue(feedbackScoreFieldValue);
		reciterArticleFeedbackScore.setAcceptedCount(countAccepted);
		reciterArticleFeedbackScore.setRejectedCount(countRejected);
		reciterArticleFeedbackScore.setScoreAll(scoreAll);
		reciterArticleFeedbackScore.setScoreWithout1Accepted(scoreWithout1Accepted);
		reciterArticleFeedbackScore.setScoreWithout1Rejected(scoreWithout1Rejected);
		reciterArticleFeedbackScore.setFeedbackScore(feedbackScore);
		reciterArticleFeedbackScore.setExportedFeedbackScore(exportedFeedbackScore);
		return reciterArticleFeedbackScore;
	}
}
