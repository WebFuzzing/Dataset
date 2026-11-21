package reciter.algorithm.evidence.targetauthor.feedback.email.strategy;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import reciter.algorithm.evidence.feedback.targetauthor.AbstractTargetAuthorFeedbackStrategy;
import reciter.engine.EngineParameters;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;

public class EmailFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(EmailFeedbackStrategy.class);
	List<ReCiterAuthor> listOfAuthors = null;
	Map<String, List<ReCiterArticleFeedbackScore>> feedbackEmailMap = null;
	

	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {

		return 0.0;

	}
	private String sanitizeAffiliation(ReCiterAuthor author) {
		 
		String affiliation = author.getAffiliation().replaceAll(EngineParameters.getRegexForStopWords(),"");
	 	String emailRegex = "[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,4}";
		Pattern pattern = Pattern.compile(emailRegex);
		Matcher matcher = pattern.matcher(affiliation);

		if(matcher.find())
			return matcher.group();
		return null;		
	}

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {

		StopWatch stopWatchforEmailFeedback = new StopWatch("Email");
		stopWatchforEmailFeedback.start("Email");
		try {
			slf4jLogger.info("reCiterArticles size: ", reCiterArticles.size());

			// Compute total accepted articles for informed absence penalty
			final int totalAccepted = (int) reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == ACCEPTED)
				.count();

			 Map<String, Map<Integer, Long>> emailCountsByArticleStatus = reCiterArticles.stream()
	        		 .filter(article -> article!=null && article.getArticleCoAuthors() !=null && article.getArticleCoAuthors().getAuthors()!=null && article.getArticleCoAuthors().getAuthors().size() > 0)
	                .flatMap(article -> article.getArticleCoAuthors().getAuthors().stream()
	                	 .filter(author-> author!=null && author.getAffiliation()!=null && !author.getAffiliation().isBlank() && author.isTargetAuthor())	
	                	 .flatMap(author -> {
	                    	String email = sanitizeAffiliation(author);

	                    	List<String> emailList = new ArrayList<>();

	                        // Extract keyword from descriptorName if majorTopicYN is Y
	                        if(email!=null && !email.isBlank())
	                        {
	                            emailList.add(email);
	                        }
	                        
		                        // Create a stream of (keyword, status) pairs
	                        return emailList.stream()
	                            .map(emailEntry -> new AbstractMap.SimpleEntry<>(emailEntry, article.getGoldStandard()));
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
					   .filter(article-> article!=null && article.getArticleCoAuthors()!=null && article.getArticleCoAuthors().getAuthors()!=null && article.getArticleCoAuthors().getAuthors().size()> 0)
					   .forEach(article -> {

						   				feedbackEmailMap = new HashMap<>();   
											
										   article.getArticleCoAuthors().getAuthors().stream()
												.filter(author -> author!=null && author.isTargetAuthor() && author.getAffiliation()!=null && !author.getAffiliation().equalsIgnoreCase(""))
												.forEach(author -> {
													
													 int countAccepted = 0;
													 int countRejected = 0;
													 double scoreAll = 0.0;
													 double scoreWithout1Accepted = 0.0;
													 double scoreWithout1Rejected = 0.0;
													 
													 String email = sanitizeAffiliation(author);
													
													if(email!=null && !email.equalsIgnoreCase(""))
													{	
														
														if(emailCountsByArticleStatus!=null && emailCountsByArticleStatus.size() > 0)
														{
															if(emailCountsByArticleStatus.containsKey(email))
															{
																if(emailCountsByArticleStatus.get(email).containsKey(ACCEPTED))
																	countAccepted = Math.toIntExact(emailCountsByArticleStatus.get(email).get(ACCEPTED));
																if(emailCountsByArticleStatus.get(email).containsKey(REJECTED))
																	countRejected = Math.toIntExact(emailCountsByArticleStatus.get(email).get(REJECTED));
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
															String exportedFeedbackScore = decimalFormat.format(feedbackScore);
															
															ReCiterArticleFeedbackScore feedbackEmail = populateArticleFeedbackScore(article.getArticleId(),email,
																	   countAccepted,countRejected,
																	   scoreAll,scoreWithout1Accepted,
																	   scoreWithout1Rejected,article.getGoldStandard(),feedbackScore,exportedFeedbackScore,"Email");
															
															feedbackEmailMap.computeIfAbsent(email, k -> new ArrayList<>()).add(feedbackEmail);

														}
													// Informed absence: email never seen
													else if (countAccepted == 0 && countRejected == 0 && totalAccepted > 0) {
														double penalty = computeInformedAbsencePenalty(totalAccepted);
														String exportedFeedbackScore = decimalFormat.format(penalty);
														ReCiterArticleFeedbackScore feedbackEmail = populateArticleFeedbackScore(article.getArticleId(),email,
																   countAccepted,countRejected,
																   penalty,penalty,
																   penalty,article.getGoldStandard(),penalty,exportedFeedbackScore,"Email");
														feedbackEmailMap.computeIfAbsent(email, k -> new ArrayList<>()).add(feedbackEmail);
													}

													}
												});
										 
										   
										   double totalScore = feedbackEmailMap.values().stream().flatMap(List::stream) // Flatten the lists into a single stream of ReCiterFeedbackScoreCoAuthorName
													.mapToDouble(ReCiterArticleFeedbackScore::getFeedbackScore) // Extract the scores
													.sum(); // Sum all scores
											
											
											// Sort Map Contents before storing into another Map
										   feedbackEmailMap.entrySet().stream()
													.filter(entry -> entry.getKey() != null && entry.getValue() != null)
													.sorted(Map.Entry.comparingByKey())
													.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
															(oldValue, newValue) -> oldValue, // merge function
															LinkedHashMap::new // to maintain insertion order
													));
											article.addArticleFeedbackScoresMap(feedbackEmailMap);
											article.setEmailFeedbackScore(totalScore);
											String exportedEmailFeedbackScore = decimalFormat.format(totalScore); 
											article.setExportedEmailFeedbackScore(exportedEmailFeedbackScore);
					   				});
							
							
				
						} catch (Exception e) {
							e.printStackTrace();
						}
						stopWatchforEmailFeedback.stop();
						slf4jLogger.info(stopWatchforEmailFeedback.getId() + " took "
								+ stopWatchforEmailFeedback.getTotalTimeSeconds() + "s"); 
						return 0;
					}

}
