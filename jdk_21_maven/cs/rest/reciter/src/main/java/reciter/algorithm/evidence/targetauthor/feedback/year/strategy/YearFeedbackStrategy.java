package reciter.algorithm.evidence.targetauthor.feedback.year.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import reciter.algorithm.evidence.feedback.targetauthor.AbstractTargetAuthorFeedbackStrategy;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.identity.Identity;

public class YearFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(YearFeedbackStrategy.class);
	Map<String, List<ReCiterArticleFeedbackScore>> feedbackYearMap = null;
	private Integer minArticleYear=0;

	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {

		return 0.0;

	}
	
	public static int pickArticleYear(ReCiterArticle article) {
		int articleYear =0;
		if (article != null && article.getPublicationDateStandardized() != null
				&& !article.getPublicationDateStandardized().equalsIgnoreCase(""))
			articleYear = Integer.parseInt(article.getPublicationDateStandardized().substring(0, 4));
		else if (article != null && article.getDatePublicationAddedToEntrez() != null
				&& !article.getDatePublicationAddedToEntrez().equalsIgnoreCase(""))
			articleYear = Integer.parseInt(article.getDatePublicationAddedToEntrez().substring(0, 4));
		
		return articleYear;
	}
	
	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		StopWatch stopWatchforYearFeedback = new StopWatch("Year");
		stopWatchforYearFeedback.start("Year");
		try {
			slf4jLogger.info("reCiterArticles size: ", reCiterArticles.size());
			
			
			 // Group by year and count based on status
	        Map<Integer, Map<Integer, Long>> yearCountByArticleStatus = reCiterArticles.stream()
	            .collect(Collectors.groupingBy(
	                article -> pickArticleYear(article), 
	                Collectors.groupingBy(
	                    ReCiterArticle::getGoldStandard,
	                    Collectors.counting()
	                )
	            ));

	        
	        
	        // Find the minimum year
	        Optional<Integer> minYear = reCiterArticles.stream()
	            .filter(article -> article != null && article.getGoldStandard() == 1) // Filter out null and empty
	            .map(article -> {
	                try {
	                    return pickArticleYear(article); 
	                } catch (NumberFormatException e) {
	                    // Handle invalid integer format
	                    return 9999;
	                }
	            })
	            .filter(year -> year != null) // Filter out any parsing errors
	            .min(Integer::compareTo); // Find the minimum year

			reCiterArticles.stream()
						   .filter(article-> article!=null && (article.getPublicationDateStandardized()!=null || article.getDatePublicationAddedToEntrez()!=null))
						   .forEach(article -> {
							    int countAccepted = 0;
								int countRejected = 0;
								double scoreAll = 0.0;
								double scoreWithout1Accepted = 0.0;
								double scoreWithout1Rejected = 0.0;
								Integer articleYear = 0;
								
								feedbackYearMap = new HashMap<>();
								
								articleYear = pickArticleYear(article);

							if (articleYear != null && articleYear != 0) {
								
								 if(yearCountByArticleStatus !=null && yearCountByArticleStatus.size() > 0 &&yearCountByArticleStatus.containsKey(articleYear) && yearCountByArticleStatus.get(articleYear).containsKey(ACCEPTED))
							    	 countAccepted = Math.toIntExact(yearCountByArticleStatus.get(articleYear).get(ACCEPTED).longValue());
							     if(yearCountByArticleStatus!=null && yearCountByArticleStatus.size()>0 && yearCountByArticleStatus.containsKey(articleYear) && yearCountByArticleStatus.get(articleYear).containsKey(REJECTED))
									 countRejected = Math.toIntExact(yearCountByArticleStatus.get(articleYear).get(REJECTED).longValue());
								
							
								 if(countAccepted > 0 && minArticleYear==0)
								 {	 
									 if(minYear.isPresent())
										 minArticleYear = minYear.get();
									 else
										 minArticleYear = 9999;
								 }
								
								scoreAll = calculateScore(articleYear, countAccepted, countRejected, minArticleYear);
								scoreWithout1Accepted = calculateScore(articleYear, countAccepted > 0 ? countAccepted - 1 : 0, countRejected, minArticleYear);
								scoreWithout1Rejected = calculateScore(articleYear, countAccepted, countRejected > 0 ? countRejected - 1 : 0, minArticleYear);
								
								double feedbackScore= determineFeedbackScore(article.getGoldStandard(),scoreWithout1Accepted, scoreWithout1Rejected, scoreAll);
								String exportedFeedbackScore = decimalFormat.format(feedbackScore);
								
								ReCiterArticleFeedbackScore feedbackYear = populateArticleFeedbackScore(article.getArticleId(),Integer.toString(articleYear),
										   countAccepted,countRejected,
										   scoreAll,scoreWithout1Accepted,
										   scoreWithout1Rejected,article.getGoldStandard(),feedbackScore,exportedFeedbackScore, "Year");	
	
								
								String yearMapKey = (article.getJournal() != null && article.getJournal().getJournalTitle() != null)
										? article.getJournal().getJournalTitle().trim() : "Unknown";
								feedbackYearMap.computeIfAbsent(yearMapKey, k -> new ArrayList<>()).add(feedbackYear);
								
								feedbackYearMap.entrySet().stream()
								.filter(entry -> entry.getKey() != null && entry.getValue() != null)
								.sorted(Map.Entry.comparingByKey())
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
										(oldValue, newValue) -> oldValue, // merge function
										LinkedHashMap::new // to maintain insertion order
								));
								article.addArticleFeedbackScoresMap(feedbackYearMap);
								article.setYearFeedbackScore(feedbackYear.getFeedbackScore());	
								String exportedYearFeedbackScore = decimalFormat.format(feedbackYear.getFeedbackScore());
								article.setExportedYearFeedbackScore(exportedYearFeedbackScore);		
						}
							
								
				});
			

		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
	// Helper method to calculate score based on given formula
    private static double calculateScore(int value, int countAccepted, int countRejected, int firstYearValue) {
        double baseScore = -((firstYearValue - value) / 10.0) * (Math.sqrt(Math.max(countAccepted + countRejected, 1)) + 1);
        double score =  Math.min(Math.max(baseScore, -5.0), 0.0);
        return score;
    }
}
