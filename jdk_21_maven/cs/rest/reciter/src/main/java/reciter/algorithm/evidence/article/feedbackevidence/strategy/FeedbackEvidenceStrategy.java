package reciter.algorithm.evidence.article.feedbackevidence.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.evidence.article.AbstractReCiterArticleStrategy;
import reciter.engine.analysis.evidence.FeedbackEvidence;
import reciter.model.article.ReCiterArticle;

/**
 * @author mrj4001
 * This class is to populate FeedbackEvidence for all articles
 */
public class FeedbackEvidenceStrategy extends AbstractReCiterArticleStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(FeedbackEvidenceStrategy.class);
	protected DecimalFormat decimalFormat = new DecimalFormat("#.###");

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
				FeedbackEvidence feedbackEvidence = new FeedbackEvidence();
				feedbackEvidence.setFeedbackScoreCites(reCiterArticle.getCitesFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreCoAuthorName(reCiterArticle.getCoAuthorNameFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreEmail(reCiterArticle.getEmailFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreInstitution(reCiterArticle.getInstitutionFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreJournal(reCiterArticle.getJournalFeedackScore()*100);
				feedbackEvidence.setFeedbackScoreJournalSubField(reCiterArticle.getJournalSubFieldFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreKeyword(reCiterArticle.getKeywordFeedackScore()*100);
				feedbackEvidence.setFeedbackScoreOrcid(reCiterArticle.getOrcidFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreOrcidCoAuthor(reCiterArticle.getOrcidCoAuthorFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreOrganization(reCiterArticle.getOrganizationFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreTargetAuthorName(reCiterArticle.getTargetAuthorNameFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreYear(reCiterArticle.getYearFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreTextSimilarity(reCiterArticle.getTextSimilarityFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreJournalTitleSimilarity(reCiterArticle.getJournalTitleSimilarityFeedbackScore()*100);
				feedbackEvidence.setFeedbackScoreBibliographicCoupling(reCiterArticle.getBibliographicCouplingFeedbackScore()*100);

				if(feedbackEvidence != null) {
					//slf4jLogger.info("Pmid: " + reCiterArticle.getArticleId() + " " + feedbackEvidence);
					reCiterArticle.setFeedbackEvidence(feedbackEvidence);
				}
			});
		return 0;
	}
	//Do not delete. Probably will be used in a near future
	 private double truncate(double value, int decimalPlaces) {
		return BigDecimal.valueOf(value)
         .setScale(decimalPlaces, RoundingMode.DOWN)
         .doubleValue();

	    }
}
