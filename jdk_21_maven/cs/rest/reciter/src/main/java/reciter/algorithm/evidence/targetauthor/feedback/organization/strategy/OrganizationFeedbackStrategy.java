package reciter.algorithm.evidence.targetauthor.feedback.organization.strategy;

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

import reciter.algorithm.evidence.feedback.targetauthor.AbstractTargetAuthorFeedbackStrategy;
import reciter.engine.EngineParameters;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;

public class OrganizationFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(OrganizationFeedbackStrategy.class);
	Map<String, List<ReCiterArticleFeedbackScore>> feedbackOrganizationMap = null;
	List<ReCiterAuthor> listOfAuthors = null;
	List<ReCiterArticle> validOrganizations = null;

	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {

		return 0.0;

	}
	
	private List<String> sanitizeAffiliation(String affiliation)
	{
		Pattern divisionPattern = Pattern.compile("Division of [^,;\\.\\(]+");
		Pattern departmentPattern = Pattern.compile("Department of [^,;\\.\\(]+");
		Pattern programPattern = Pattern.compile("[^,;\\.\\(]+ Program");
		Pattern generalDepartmentPattern = Pattern.compile("[^,;\\.\\(]+ Department");
		
		List<String> organizationList = new ArrayList<>();
		
		if(affiliation!=null)
		{
			Matcher matcher;
			if ((matcher = divisionPattern.matcher(affiliation)).find()) {
				affiliation = matcher.group().replace("Division of ", "").split(",")[0];
			} else if ((matcher = departmentPattern.matcher(affiliation)).find()) {
				affiliation = matcher.group().replace("Department of ", "").split(",")[0];
			} else if ((matcher = programPattern.matcher(affiliation)).find()) {
				affiliation = matcher.group().replace(" Program", "").split(",")[0];
			} else if ((matcher = generalDepartmentPattern.matcher(affiliation)).find()) {
				affiliation = matcher.group().split(" Department")[0];
			}
			else if(affiliation.indexOf(",") > -1 || affiliation.indexOf(".") > -1) 
				affiliation = "";

			if(affiliation!=null && !affiliation.equalsIgnoreCase(""))
			{	
				affiliation = affiliation.trim();
				affiliation = affiliation.replaceFirst("^and ", "").replaceFirst(" and$", "");
				affiliation = affiliation.replace(" & ", " and");

				if (!affiliation.isBlank() && !affiliation.contains("Division")
						&& !affiliation.contains("Center") && !affiliation.contains("Centre")
						&& !affiliation.contains("University") && !affiliation.contains("USA")
						&& affiliation.length() > 2 && !affiliation.equals("From the")
						&& !affiliation.equals("and")) {
					
					affiliation = affiliation.replaceAll(EngineParameters.getRegexForStopWords(),"");
					organizationList.add(affiliation);
				}
				
			}
		}

		return organizationList;
	}
	

	//Collect the orginization and store in a Separate List of Object then use that to filter records.
	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		try {

			// Compute total accepted articles for informed absence penalty
			final int totalAccepted = (int) reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == ACCEPTED)
				.count();

			 Map<String, Map<Integer, Long>> orgCountsByArticleStatus = reCiterArticles.stream()
	        		 .filter(article -> article!=null && article.getArticleCoAuthors() !=null && article.getArticleCoAuthors().getAuthors()!=null && article.getArticleCoAuthors().getAuthors().size() > 0)
	                .flatMap(article -> article.getArticleCoAuthors().getAuthors().stream()
	                	 .filter(author-> author!=null && author.getAffiliation()!=null && !author.getAffiliation().isBlank() && author.isTargetAuthor())	
	                	 .flatMap(author -> {
	                    	List<String> organizationList = sanitizeAffiliation(author.getAffiliation());
		                        // Create a stream of (keyword, status) pairs
	                        return organizationList.stream()
	                            .map(organizationEntry -> new AbstractMap.SimpleEntry<>(organizationEntry, article.getGoldStandard()));
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

				   			feedbackOrganizationMap = new HashMap<>();   
									
								   article.getArticleCoAuthors().getAuthors().stream()
										.filter(author -> author!=null && author.isTargetAuthor() && author.getAffiliation()!=null && !author.getAffiliation().equalsIgnoreCase(""))
										.forEach(author -> {
											
											 
											 
											 List<String> organizationList = sanitizeAffiliation(author.getAffiliation());
											 organizationList.stream()
											 				.filter(organization -> organization!=null && !organization.isBlank())
											 				.forEach(organization-> {	
												
							 					int countAccepted = 0;
												 int countRejected = 0;
												 double scoreAll = 0.0;
												 double scoreWithout1Accepted = 0.0;
												 double scoreWithout1Rejected = 0.0;				
												if(orgCountsByArticleStatus!=null && orgCountsByArticleStatus.size() > 0)
												{
													if(orgCountsByArticleStatus.containsKey(organization))
													{
														if(orgCountsByArticleStatus.get(organization).containsKey(ACCEPTED))
															countAccepted = Math.toIntExact(orgCountsByArticleStatus.get(organization).get(ACCEPTED));
														if(orgCountsByArticleStatus.get(organization).containsKey(REJECTED))
															countRejected = Math.toIntExact(orgCountsByArticleStatus.get(organization).get(REJECTED));
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
													
													ReCiterArticleFeedbackScore feedbackOrg = populateArticleFeedbackScore(article.getArticleId(),organization,
															   countAccepted,countRejected,
															   scoreAll,scoreWithout1Accepted,
															   scoreWithout1Rejected,article.getGoldStandard(),feedbackScore,exportedFeedbackScore, "Organization");
													
													List<ReCiterArticleFeedbackScore> feedbackList = feedbackOrganizationMap.computeIfAbsent(organization, k -> new ArrayList<>());

	
													// Check if the list is empty or if the item does not exist in the list
													if (feedbackList.isEmpty()) {
													    feedbackList.add(feedbackOrg);
													} else {
													    for (ReCiterArticleFeedbackScore feedbackOrganization : feedbackList) {
													        if (feedbackOrganization != null && !feedbackOrganization.getFeedbackScoreFieldValue().equalsIgnoreCase(feedbackOrg.getFeedbackScoreFieldValue())) {
													            // If no match found, add the feedbackOrg
													            feedbackList.add(feedbackOrg);
													            break; // Exit loop once item is added
													        }
													    }
													}

												}
											// Informed absence: organization never seen
											else if (countAccepted == 0 && countRejected == 0 && totalAccepted > 0) {
												double penalty = computeInformedAbsencePenalty(totalAccepted);
												String exportedFeedbackScore = decimalFormat.format(penalty);
												ReCiterArticleFeedbackScore feedbackOrg = populateArticleFeedbackScore(article.getArticleId(),organization,
														   countAccepted,countRejected,
														   penalty,penalty,
														   penalty,article.getGoldStandard(),penalty,exportedFeedbackScore, "Organization");
												feedbackOrganizationMap.computeIfAbsent(organization, k -> new ArrayList<>()).add(feedbackOrg);
											}

											});
										});
								   
								   
								   double totalScore = feedbackOrganizationMap.values().stream().flatMap(List::stream) // Flatten the lists into a single stream of ReCiterFeedbackScoreCoAuthorName
											.mapToDouble(ReCiterArticleFeedbackScore::getFeedbackScore) // Extract the scores
											.sum(); // Sum all scores
									
									
									// Sort Map Contents before storing into another Map
								   feedbackOrganizationMap.entrySet().stream()
											.filter(entry -> entry.getKey() != null && entry.getValue() != null)
											.sorted(Map.Entry.comparingByKey())
											.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
													(oldValue, newValue) -> oldValue, // merge function
													LinkedHashMap::new // to maintain insertion order
											));
								   article.addArticleFeedbackScoresMap(feedbackOrganizationMap);
									article.setOrganizationFeedbackScore(totalScore);
									String exportedOrgFeedbackScore = decimalFormat.format(totalScore); 
									article.setExportedOrganizationFeedbackScore(exportedOrgFeedbackScore);
			   				});
					
			

		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0.0;
	}

}
