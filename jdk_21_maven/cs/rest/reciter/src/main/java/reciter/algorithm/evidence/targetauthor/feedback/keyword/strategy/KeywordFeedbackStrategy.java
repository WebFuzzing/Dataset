package reciter.algorithm.evidence.targetauthor.feedback.keyword.strategy;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import reciter.ApplicationContextHolder;
import reciter.algorithm.evidence.feedback.targetauthor.AbstractTargetAuthorFeedbackStrategy;
import reciter.database.dynamodb.model.MeshTerm;
import reciter.engine.StrategyParameters;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.article.ReCiterArticleKeywords.Keyword;
import reciter.model.article.ReCiterArticleMeshHeading;
import reciter.model.article.ReCiterCitationYNEnum;
import reciter.model.article.ReCiterMeshHeadingDescriptorName;
import reciter.model.identity.Identity;
import reciter.service.dynamo.DynamoDbMeshTermService;

public class KeywordFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(KeywordFeedbackStrategy.class);
	Map<String, List<ReCiterArticleFeedbackScore>> feedbackKeywordMap = null;
	List<Keyword> listOfKeywords = null;
	List<MeshTerm> listOfMeshTerms = new ArrayList<>();
	Map<String, Long> meshCounts = null;
	
	public static StrategyParameters strategyParameters;
	
	DynamoDbMeshTermService meshTermService = ApplicationContextHolder.getContext()
			.getBean(DynamoDbMeshTermService.class);
	
	public KeywordFeedbackStrategy(StrategyParameters strategyParameters) {
		this.strategyParameters = strategyParameters;
	}
	
	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {

		return 0.0;

	}
	
	private String extractKeyword(ReCiterArticleMeshHeading meshHeaderName) {
		 
		String keyword =null;
		if(meshHeaderName!=null && ((meshHeaderName.getDescriptorName()!=null 
				&& meshHeaderName.getDescriptorName().getDescriptorName()!=null && meshHeaderName.getDescriptorName().getMajorTopicYN().equalsIgnoreCase("Y"))
				|| meshHeaderName.getQualifierNameList().stream()
            		    .filter(qualifier -> qualifier != null && qualifier.getMajorTopicYN() == ReCiterCitationYNEnum.Y)
            		    .findAny()
            		    .isPresent())) {
			 			keyword = meshHeaderName.getDescriptorName().getDescriptorName();
                    }
		return keyword;		
	}

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		
		StopWatch stopWatchforCoAuthorFeedback = new StopWatch("keyword");
		stopWatchforCoAuthorFeedback.start("keyword");
		try {
			
			// if listofMeshTerms list is empty then make a dynamoDB call to populate the list to use in the calculation
				listOfMeshTerms = retrieveMeshTermCount();
				 // Create a Map with the keyword as key and the count as value
				meshCounts = listOfMeshTerms.stream()
								            .collect(Collectors.toMap(
								                MeshTerm::getMesh,    // Use the keyword as the key
								                MeshTerm::getCount      // Use the count as the value
								            ));
			slf4jLogger.info("reCiterArticles size: ", reCiterArticles.size());

			// Compute total accepted articles for informed absence penalty
			final int totalAccepted = (int) reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == ACCEPTED)
				.count();

	        Map<String, Map<Integer, Long>> keywordCountsByArticleStatus = reCiterArticles.stream()
	        		 .filter(article -> article!=null && article.getMeshHeadings() !=null && article.getMeshHeadings().size() > 0)
	                .flatMap(article -> article.getMeshHeadings().stream()
	                	 .flatMap(meshHeading -> {
	                    	ReCiterMeshHeadingDescriptorName descriptorName = meshHeading.getDescriptorName();
	                        List<String> keywords = new ArrayList<>();

	                        // Extract keyword from descriptorName if majorTopicYN is Y
	                        if ((descriptorName!=null && descriptorName.getMajorTopicYN()!=null && descriptorName.getMajorTopicYN().equalsIgnoreCase("Y"))
	                        	|| meshHeading.getQualifierNameList().stream()
                    		    .filter(qualifier -> qualifier != null && qualifier.getMajorTopicYN() == ReCiterCitationYNEnum.Y)
                    		    .findAny()
                    		    .isPresent()) {
	                            keywords.add(descriptorName.getDescriptorName());
	                        }
	                        
		                        // Create a stream of (keyword, status) pairs
	                        return keywords.stream()
	                            .map(keyword -> new AbstractMap.SimpleEntry<>(keyword, article.getGoldStandard()));
	                    })
	                )
	                // Count occurrences of each (keyword, status) pair
	                .collect(Collectors.groupingBy(
	                    Map.Entry::getKey,
	                    Collectors.groupingBy(
	                        Map.Entry::getValue,
	                        Collectors.counting()
	                    )
	                ));

	        
		        reCiterArticles.stream()
				   .filter(article-> article!=null && article.getMeshHeadings()!=null && article.getMeshHeadings().size()> 0)
				   .forEach(article -> {
											
										feedbackKeywordMap = new HashMap<>();   
										
									   article.getMeshHeadings().stream()
											.filter(meshHeading -> meshHeading!=null && meshHeading.getDescriptorName()!=null )
											.forEach(meshHeading -> {
												
												 int countAccepted = 0;
												 int countRejected = 0;
												 double scoreAll = 0.0;
												 double scoreWithout1Accepted = 0.0;
												 double scoreWithout1Rejected = 0.0;
												 
												 String keyword = extractKeyword(meshHeading);
												
												if(keyword!=null && !keyword.isBlank())
												{	
													
													if(keywordCountsByArticleStatus!=null && keywordCountsByArticleStatus.size() > 0)
													{
														if(keywordCountsByArticleStatus.containsKey(keyword))
														{
															if(keywordCountsByArticleStatus.get(keyword).containsKey(ACCEPTED))
																countAccepted = Math.toIntExact(keywordCountsByArticleStatus.get(keyword).get(ACCEPTED));
															if(keywordCountsByArticleStatus.get(keyword).containsKey(REJECTED))
																countRejected = Math.toIntExact(keywordCountsByArticleStatus.get(keyword).get(REJECTED));
														}
													}
													
													if(countAccepted > 0 || countRejected > 0)
													{	
														
														scoreAll = computeScore(countAccepted , countRejected);
														scoreWithout1Accepted = computeScore(countAccepted > 0 ? countAccepted - 1 : countAccepted,
																countRejected);
														scoreWithout1Rejected = computeScore(countAccepted,
																countRejected > 0 ? countRejected - 1 : countRejected);
														
														double feedbackScore= determineFeedbackScore(article.getGoldStandard(),scoreWithout1Accepted, scoreWithout1Rejected, scoreAll);
														long meshCount = 0;
														double factor = 0.0;
														if(meshCounts!=null && meshCounts.containsKey(keyword))
														{	
															meshCount = meshCounts.get(keyword);
															factor = calculateFactor(meshCount);
															feedbackScore = feedbackScore * factor;
														}
														
														String exportedFeedbackScore = decimalFormat.format(feedbackScore);
														
														ReCiterArticleFeedbackScore feedbackEmail = populateArticleFeedbackScore(article.getArticleId(),keyword,
																   countAccepted,countRejected,
																   scoreAll,scoreWithout1Accepted,
																   scoreWithout1Rejected,article.getGoldStandard(),feedbackScore,exportedFeedbackScore,"Keyword");
														
														feedbackKeywordMap.computeIfAbsent(keyword, k -> new ArrayList<>()).add(feedbackEmail);

													}
												// Informed absence: keyword never seen
												else if (countAccepted == 0 && countRejected == 0 && totalAccepted > 0) {
													double penalty = computeInformedAbsencePenalty(totalAccepted);
													long meshCount = 0;
													double factor = 1.0;
													if(meshCounts!=null && meshCounts.containsKey(keyword))
													{
														meshCount = meshCounts.get(keyword);
														factor = calculateFactor(meshCount);
														penalty = penalty * factor;
													}
													String exportedFeedbackScore = decimalFormat.format(penalty);
													ReCiterArticleFeedbackScore feedbackEmail = populateArticleFeedbackScore(article.getArticleId(),keyword,
															   countAccepted,countRejected,
															   penalty,penalty,
															   penalty,article.getGoldStandard(),penalty,exportedFeedbackScore,"Keyword");
													feedbackKeywordMap.computeIfAbsent(keyword, k -> new ArrayList<>()).add(feedbackEmail);
												}

												}
											});

									   
									   double totalScore = feedbackKeywordMap.values().stream().flatMap(List::stream) // Flatten the lists into a single stream of ReCiterFeedbackScoreCoAuthorName
												.mapToDouble(score-> score.getFeedbackScore()) // Extract the scores
												.sum(); // Sum all scores
										
										
										// Sort Map Contents before storing into another Map
									   feedbackKeywordMap.entrySet().stream()
												.filter(entry -> entry.getKey() != null && entry.getValue() != null)
												.sorted(Map.Entry.comparingByKey())
												.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
														(oldValue, newValue) -> oldValue, // merge function
														LinkedHashMap::new // to maintain insertion order
												));
										article.addArticleFeedbackScoresMap(feedbackKeywordMap);
										article.setKeywordFeedackScore(totalScore);
										String exportedKeywordFeedackScore = decimalFormat.format(totalScore); 
										article.setExportedKeywordFeedackScore(exportedKeywordFeedackScore);
				   				});
			
				

			} catch (Exception e) {
				e.printStackTrace();
			}
			stopWatchforCoAuthorFeedback.stop();
			slf4jLogger.info(stopWatchforCoAuthorFeedback.getId() + " took "
					+ stopWatchforCoAuthorFeedback.getTotalTimeSeconds() + "s");		
			
		return 0;
	}
	private List<MeshTerm> retrieveMeshTermCount() {
	    
		return meshTermService.findAll();
	}
	public static double calculateFactor(double keywordCount) {
        double logBase = strategyParameters.getKeywordLogBase();
        double baseline = strategyParameters.getKeywordCountBaseline();
        double keywordOffset = strategyParameters.getKeywordOffset();

        // Ensure keywordCount is at least 1 for the logarithmic calculation (to avoid division by zero)
        double adjustedKeywordCount = Math.max(1, keywordCount);

        // Calculate the log value
        double logValue = Math.log(baseline / adjustedKeywordCount) / Math.log(logBase); // LOG base conversion

        // Calculate factor and ensure it is not less than 0
        double factor = Math.max(logValue + keywordOffset, 0);

        return factor;
    }
}
