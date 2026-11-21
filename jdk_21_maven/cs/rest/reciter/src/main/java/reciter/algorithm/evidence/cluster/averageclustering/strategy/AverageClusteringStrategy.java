package reciter.algorithm.evidence.cluster.averageclustering.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.cluster.article.scorer.ReCiterArticleScorer;
import reciter.algorithm.cluster.model.ReCiterCluster;
import reciter.algorithm.evidence.cluster.AbstractClusterStrategy;
import reciter.engine.analysis.evidence.AverageClusteringEvidence;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterAuthor;

/**
 * @author szd2013
 * @see <a href= "https://github.com/wcmc-its/ReCiter/issues/232">Average Clustering Strategy</a>
 */
public class AverageClusteringStrategy extends AbstractClusterStrategy {
	
	private static final Logger slf4jLogger = LoggerFactory.getLogger(AverageClusteringStrategy.class);

	@Override
	public double executeStrategy(ReCiterCluster reCiterCluster) {
			
			List<String> articleAuthorFirstNames = new ArrayList<String>();
			reCiterCluster.getArticleCluster().stream().forEach(reCiterArticle -> {
				if(reCiterCluster.getArticleCluster().size() > 1) {
					populateArticeAuthorFirstName(reCiterArticle, articleAuthorFirstNames);
				}
				if(ReCiterArticleScorer.strategyParameters.isUseGoldStandardEvidence()) {
					double totalArticleScoreWithoutClustering =  ((reCiterArticle.getAuthorNameEvidence() != null)?(reCiterArticle.getAuthorNameEvidence().getNameScoreTotal()):0) +
							((reCiterArticle.getEmailEvidence() != null)?reCiterArticle.getEmailEvidence().getEmailMatchScore():0) +
							((reCiterArticle.getGenderEvidence() != null && reCiterArticle.getGenderEvidence().getGenderScoreIdentityArticleDiscrepancy() != null)?reCiterArticle.getGenderEvidence().getGenderScoreIdentityArticleDiscrepancy():0) +
							reCiterArticle.getGrantEvidenceTotalScore() +
							((reCiterArticle.getRelationshipEvidence() != null)?reCiterArticle.getRelationshipEvidence().getRelationshipEvidenceTotalScore():0) +
							((reCiterArticle.getEducationYearEvidence() != null)?reCiterArticle.getEducationYearEvidence().getDiscrepancyDegreeYearBachelorScore():0) +
							((reCiterArticle.getEducationYearEvidence() != null)?reCiterArticle.getEducationYearEvidence().getDiscrepancyDegreeYearDoctoralScore():0) +
							reCiterArticle.getOrganizationalEvidencesTotalScore() +
							reCiterArticle.getAffiliationScore() + 
							reCiterArticle.getArticleCountEvidence().getArticleCountScore() +
							((reCiterArticle.getPersonTypeEvidence() != null)?reCiterArticle.getPersonTypeEvidence().getPersonTypeScore():0) +
							((reCiterArticle.getJournalCategoryEvidence() != null)?reCiterArticle.getJournalCategoryEvidence().getJournalSubfieldScore():0) +
							((reCiterArticle.getAcceptedRejectedEvidence().getFeedbackScoreAccepted() !=null)?reCiterArticle.getAcceptedRejectedEvidence().getFeedbackScoreAccepted():0) + 
							((reCiterArticle.getAcceptedRejectedEvidence().getFeedbackScoreRejected() !=null)?reCiterArticle.getAcceptedRejectedEvidence().getFeedbackScoreRejected():0) +
							((reCiterArticle.getAcceptedRejectedEvidence().getFeedbackScoreNull() !=null)?reCiterArticle.getAcceptedRejectedEvidence().getFeedbackScoreNull():0);
					
					reCiterArticle.setTotalArticleScoreWithoutClustering(totalArticleScoreWithoutClustering);
				} else {
					double totalArticleScoreWithoutClustering = ((reCiterArticle.getAuthorNameEvidence() != null)?(reCiterArticle.getAuthorNameEvidence().getNameScoreTotal()):0) + 
							((reCiterArticle.getEmailEvidence() != null)?reCiterArticle.getEmailEvidence().getEmailMatchScore():0) + 
							((reCiterArticle.getGenderEvidence() != null && reCiterArticle.getGenderEvidence().getGenderScoreIdentityArticleDiscrepancy() != null)?reCiterArticle.getGenderEvidence().getGenderScoreIdentityArticleDiscrepancy():0) +
							reCiterArticle.getGrantEvidenceTotalScore() +
							((reCiterArticle.getRelationshipEvidence() != null)?reCiterArticle.getRelationshipEvidence().getRelationshipEvidenceTotalScore():0) +
							((reCiterArticle.getEducationYearEvidence() != null)?reCiterArticle.getEducationYearEvidence().getDiscrepancyDegreeYearBachelorScore():0) +
							((reCiterArticle.getEducationYearEvidence() != null)?reCiterArticle.getEducationYearEvidence().getDiscrepancyDegreeYearDoctoralScore():0) +
							reCiterArticle.getOrganizationalEvidencesTotalScore() +
							reCiterArticle.getAffiliationScore() +
							reCiterArticle.getArticleCountEvidence().getArticleCountScore() +
							((reCiterArticle.getJournalCategoryEvidence() != null)?reCiterArticle.getJournalCategoryEvidence().getJournalSubfieldScore():0) +
							((reCiterArticle.getPersonTypeEvidence() != null)?reCiterArticle.getPersonTypeEvidence().getPersonTypeScore():0);
					
					reCiterArticle.setTotalArticleScoreWithoutClustering(totalArticleScoreWithoutClustering);
				}
						
			});
			
			if(articleAuthorFirstNames.size() > 0) {
				Map<String, Long> firstNameFrequencyCounts = articleAuthorFirstNames.stream().collect(Collectors.groupingBy(e -> e , Collectors.counting()));
				if(firstNameFrequencyCounts.size() > 0) {
					Long maxFrequencyCount = Collections.max(firstNameFrequencyCounts.entrySet(), Comparator.comparingLong(Map.Entry::getValue)).getValue();
					if(articleAuthorFirstNames.size() > 0) {
						double mostCommonNameRatio = (double) maxFrequencyCount.longValue()/articleAuthorFirstNames.size();
						//reCiterCluster.setClusterReliabilityScore(Math.pow(mostCommonNameRatio, ReCiterArticleScorer.strategyParameters.getClusterReliabilityScoreFactor()));
					}
				}
			}
		
		double averageClusterScore = getAverageClusterScore(reCiterCluster);
		populateAverageClusterEvidence(reCiterCluster, averageClusterScore);
		
		
		return 0;
	}
	
	private double getAverageClusterScore(ReCiterCluster reCiterCluster) {
		double totalClusterScore = reCiterCluster.getArticleCluster().stream().mapToDouble(reCiterArticle -> reCiterArticle.getTotalArticleScoreWithoutClustering()).sum();
		return totalClusterScore/reCiterCluster.getArticleCluster().size();
	}
	
	private void populateAverageClusterEvidence(ReCiterCluster reCiterCluster, double averageClusterScore) {
		reCiterCluster.getArticleCluster().stream().forEach(reCiterArticle -> {
			reCiterArticle.setTotalArticleScoreWithoutClustering(reCiterArticle.getTotalArticleScoreWithoutClustering()
					- (((reCiterArticle.getAcceptedRejectedEvidence() != null && reCiterArticle.getAcceptedRejectedEvidence().getFeedbackScoreAccepted() !=null)?reCiterArticle.getAcceptedRejectedEvidence().getFeedbackScoreAccepted():0) +
							((reCiterArticle.getAcceptedRejectedEvidence() != null && reCiterArticle.getAcceptedRejectedEvidence().getFeedbackScoreRejected() !=null)?reCiterArticle.getAcceptedRejectedEvidence().getFeedbackScoreRejected():0)));
			/*double clusterScoreDiscrepancy = (reCiterArticle.getTotalArticleScoreWithoutClustering() - averageClusterScore) * ReCiterArticleScorer.strategyParameters.getClusterScoreFactor()
					* ((reCiterCluster.getClusterReliabilityScore()>0)?reCiterCluster.getClusterReliabilityScore():1);*/
			AverageClusteringEvidence averageClusteringEvidence = new AverageClusteringEvidence();
			averageClusteringEvidence.setClusterScoreAverage(roundAvoid(averageClusterScore, 2));
			averageClusteringEvidence.setClusterReliabilityScore(roundAvoid(reCiterCluster.getClusterReliabilityScore(),2));
			//averageClusteringEvidence.setClusterScoreModificationOfTotalScore(roundAvoid(-clusterScoreDiscrepancy, 2));
			averageClusteringEvidence.setTotalArticleScoreWithoutClustering(roundAvoid(reCiterArticle.getTotalArticleScoreWithoutClustering(), 2));
			averageClusteringEvidence.setClusterIdentifier(reCiterCluster.getClusterID());
			//Remove accepted rejected score from raw score - https://github.com/wcmc-its/ReCiter/issues/286
			//double totalArticleScoreNonStandardized = reCiterArticle.getTotalArticleScoreWithoutClustering() - clusterScoreDiscrepancy;
			//reCiterArticle.setTotalArticleScoreNonStandardized(roundAvoid(totalArticleScoreNonStandardized, 2));
			reCiterArticle.setAverageClusteringEvidence(averageClusteringEvidence);
			slf4jLogger.info("Pmid: " + reCiterArticle.getArticleId() + " " + averageClusteringEvidence);
		});
	}
	
	private void populateArticeAuthorFirstName(ReCiterArticle reCiterArticle, List<String> articleAuthorFirstNames) {
		
		ReCiterAuthor reCiterAuthor = reCiterArticle.getArticleCoAuthors().getAuthors().stream().filter(author -> author.isTargetAuthor() == true).findAny().orElse(null);
		
		if(reCiterAuthor != null) {
			String firstName = reCiterAuthor.getAuthorName().getFirstName().replaceAll("[A-Z-.\"() ]", "").trim();
			if(!firstName.isBlank()) {
				articleAuthorFirstNames.add(firstName);
			}
			
		}
		
	}
	
	public static double roundAvoid(double value, int places) {
	    double scale = Math.pow(10, places);
	    return Math.round(value * scale) / scale;
	}

}
