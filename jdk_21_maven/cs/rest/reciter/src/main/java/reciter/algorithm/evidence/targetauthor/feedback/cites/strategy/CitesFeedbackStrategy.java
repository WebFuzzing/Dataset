package reciter.algorithm.evidence.targetauthor.feedback.cites.strategy;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import reciter.algorithm.evidence.feedback.targetauthor.AbstractTargetAuthorFeedbackStrategy;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.identity.Identity;



public class CitesFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(CitesFeedbackStrategy.class);
	Map<String, List<ReCiterArticleFeedbackScore>> feedbackCitesMap =null;


	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {

		return 0.0;

	}
	
	public static Map<Long, Integer> countArticlesByStandard(Map<Long, List<ReCiterArticle>> articlesMap) {
        return articlesMap.entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> countArticles(entry.getValue())
                ));
    }

    private static int countArticles(List<ReCiterArticle> articles) {
        return articles.stream()
                .mapToInt(ReCiterArticle::getGoldStandard) // assuming standardValue is int
                .filter(value -> value == 1 || value == -1) // filtering based on the standard values
                .map(value -> 1) // map each valid value to 1
                .sum(); // sum up the counts
    }

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		StopWatch stopWatchforCitesFeedback = new StopWatch("Cites");
		stopWatchforCitesFeedback.start("Cites");
		try {
			slf4jLogger.info("reCiterArticles size in cites: ", reCiterArticles.size());

			// Compute total accepted articles for informed absence penalty
			final int totalAccepted = (int) reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == ACCEPTED)
				.count();

			 Map<Long, ReCiterArticle> articleMap = reCiterArticles.stream()
		                .collect(Collectors.toMap(ReCiterArticle::getArticleId, Function.identity()));
			 
			 // Citing article Id Map with the group by status of the Cited Articles 
	    	 Map<Long, Map<Integer, List<ReCiterArticle>>> citingArticlesGroupedByStatus = reCiterArticles.stream()
	    	            .collect(Collectors.toMap(
	    	            		ReCiterArticle::getArticleId, // Citing article ID
	    	                article -> article.getCommentsCorrectionsPmids().stream()
	    	                    .map(correctionId -> articleMap.get(correctionId)) // Get cited articles
	    	                    .filter(Objects::nonNull) // Filter out nulls in case of invalid IDs
	    	                    .collect(Collectors.groupingBy(
	    	                    		ReCiterArticle::getGoldStandard, // Group by status of cited articles
	    	                        Collectors.toList()
	    	                    )),
	    	                (existing, replacement) -> existing // handle merging if necessary
	    	            ));
	    	 
	    	 //List of Cited Articles per Citing Article
	    	 
	    	// Create a set of article IDs for quick lookup
	         Set<Long> articleIds = reCiterArticles.stream()
	                                         .map(ReCiterArticle::getArticleId)
	                                         .collect(Collectors.toSet());
	         
	    	 // Create the map
	         Map<Long, Map<Integer, List<ReCiterArticle>>> citedArticlesGroupByStatus = reCiterArticles.stream()
	                 .flatMap(article -> article.getCommentsCorrectionsPmids().stream()
	                		 .filter(refId -> articleIds.contains(refId)) // Filter referenced IDs that exist in the articles list
	                         .map(referenceId -> new AbstractMap.SimpleEntry<>(referenceId, article)))
	                 		 .filter(Objects::nonNull)	
	                 .collect(Collectors.groupingBy(
	                         Map.Entry::getKey, // Group by commentCommercial articleId
	                         Collectors.groupingBy(
	                                 entry -> entry.getValue().getGoldStandard(), // Group by status
	                                 Collectors.mapping(Map.Entry::getValue, Collectors.toList()) // Collect articles
	                         )
	                 ));
	    	 
	    	 
	         //Count Accepted and Rejected articles per cited and citing article
			
	         reCiterArticles.stream().filter(article -> article!=null)
	         						  .forEach(article ->{
	         							  
	         							 feedbackCitesMap = new HashMap<>();
	         							 //Citing Articles
	         							 Map<Integer, List<ReCiterArticle>> citingArticlesStatusMap = citingArticlesGroupedByStatus.get(article.getArticleId());
	         							  
	         							 //Accepted articles
	         							List<ReCiterArticle> citingAcceptedArticles = null;
	         							List<ReCiterArticle> citingRejectedArticles =null;
	         							int citingArticlesCountAccepted = 0;
	         							int citingArticlesCountRejected = 0;
         							    if(citingArticlesStatusMap!=null && citingArticlesStatusMap.size() > 0)
         							    {	
         							    	
         							    	citingAcceptedArticles = citingArticlesStatusMap.getOrDefault(1, new ArrayList<ReCiterArticle>());
         							    	citingArticlesCountAccepted = citingAcceptedArticles.size();
		         						 
		         							 citingRejectedArticles = citingArticlesStatusMap.getOrDefault(-1, new ArrayList<ReCiterArticle>());
		         							 citingArticlesCountRejected = citingRejectedArticles.size();
		         						}
         							   //Cited Articles 
         							   Map<Integer, List<ReCiterArticle>> citedArticlesSatusMap = citedArticlesGroupByStatus.get(article.getArticleId());
         							 
	         							  
	         							 //Accepted articles
	         							List<ReCiterArticle> citedAcceptedArticles = null;
	         							List<ReCiterArticle> citedRejectedArticles =null;
	         							int citedArticlesCountAccepted = 0;
	         							int citedArticlesCountRejected = 0;
	       							    if(citedArticlesSatusMap!=null && citedArticlesSatusMap.size() > 0)
	       							    {	  
	   							    		citedAcceptedArticles = citedArticlesSatusMap.getOrDefault(1, new ArrayList<ReCiterArticle>());
       							    		citedArticlesCountAccepted = citedAcceptedArticles.size();
		         						 
		         							 citedRejectedArticles = citedArticlesSatusMap.getOrDefault(-1, new ArrayList<ReCiterArticle>());
		         							citedArticlesCountRejected = citedRejectedArticles.size();
			         					}
	         							
	       							    int overallCountRejected = citingArticlesCountRejected + citedArticlesCountRejected;
	       							    int overallCountAccepted = citingArticlesCountAccepted + citedArticlesCountAccepted;
	       							    
	
	       							    double scoreAllBase = computeScore(overallCountAccepted, overallCountRejected);

	       							    // Informed absence: no citing/cited articles in accepted or rejected
	       							    final double scoreAll;
	       							    if (overallCountAccepted == 0 && overallCountRejected == 0 && totalAccepted > 0) {
	       							        scoreAll = computeInformedAbsencePenalty(totalAccepted);
	       							    } else {
	       							        scoreAll = scoreAllBase;
	       							    }

	         							 //Club the Rejected Articles and Accepted Articles
	         							 List<ReCiterArticle> mergedList = Stream.of(
	         									 (citingAcceptedArticles != null && !citingAcceptedArticles.isEmpty()) ? citingAcceptedArticles : new ArrayList<ReCiterArticle>(), 
	         									 (citingRejectedArticles != null && !citingRejectedArticles.isEmpty()) ? citingRejectedArticles : new ArrayList<ReCiterArticle>(), 
	         									 (citedAcceptedArticles != null && !citedAcceptedArticles.isEmpty()) ? citedAcceptedArticles : new ArrayList<ReCiterArticle>(),
	         									 (citedRejectedArticles != null && !citedRejectedArticles.isEmpty()) ? citedRejectedArticles : new ArrayList<ReCiterArticle>())
	         					                .flatMap(Collection::stream) // Flatten the streams
	         					                .distinct()
	         					                .collect(Collectors.toList()); 
	         							
										Optional.ofNullable(mergedList).ifPresent(citredArticleList -> citredArticleList.forEach(mergedArticle -> {
	
		         								//Citing article Mapping

												// Compute leave-one-out scores for accepted and rejected articles
												double scoreWithout1Accepted = computeScore(
													overallCountAccepted > 0 ? overallCountAccepted - 1 : overallCountAccepted,
													overallCountRejected);
												double scoreWithout1Rejected = computeScore(
													overallCountAccepted,
													overallCountRejected > 0 ? overallCountRejected - 1 : overallCountRejected);

												double feedbackScore = determineFeedbackScore(article.getGoldStandard(), scoreWithout1Accepted, scoreWithout1Rejected, scoreAll);
												String exportedFeedbackScore = decimalFormat.format(feedbackScore);

		         								ReCiterArticleFeedbackScore citingArticleFeedbackScore = populateArticleFeedbackScore(article.getArticleId(),Long.toString(mergedArticle.getArticleId()),
		         										overallCountAccepted,overallCountRejected,
														   scoreAll,scoreWithout1Accepted,
														   scoreWithout1Rejected,article.getGoldStandard(),feedbackScore,exportedFeedbackScore,"Cites");	
		         								
		         								
												
		         								feedbackCitesMap.merge(Long.toString(article.getArticleId()), new ArrayList<>(Arrays.asList(citingArticleFeedbackScore)), (existingList, newList) -> {
									                existingList.addAll(newList);
									                return existingList;
											});	
	        							}));
										
	         							double totalScore = feedbackCitesMap.values().stream().flatMap(List::stream) // Flatten the lists into a single stream of ReCiterFeedbackScoreCoAuthorName
	        									.mapToDouble(score-> score.getFeedbackScore()) // Extract the scores
	        									.sum();
	         							
	         							
	         							double finalScore = (totalScore != 0? totalScore : 0) /((overallCountRejected + overallCountAccepted) != 0 ? (overallCountRejected + overallCountAccepted) : 1) ;
	         							article.setCitesFeedbackScore(finalScore);
	         							article.addArticleFeedbackScoresMap(feedbackCitesMap);
	         							String exportedCitesFeedbackScore = decimalFormat.format(finalScore);
	         							article.setExportedCitesFeedbackScore(exportedCitesFeedbackScore);
	         							  
	         						  });
	         					

		} catch (Exception e) {
			e.printStackTrace();
		}
		stopWatchforCitesFeedback.stop();
		slf4jLogger.info(stopWatchforCitesFeedback.getId() + " took "
				+ stopWatchforCitesFeedback.getTotalTimeSeconds() + "s"); 
		return 0;
	}

}
