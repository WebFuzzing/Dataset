package reciter.algorithm.evidence.targetauthor.feedback.institution.strategy;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
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
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;

public class InstitutionFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(InstitutionFeedbackStrategy.class);
	private static final List<String> targetKeywords = Arrays.asList("Center", "Centre", "Centro", "Clinic", "Colegio",
			"College", "Corporation", "Foundation", "Health System", "Hospital", "Institut", "Institute", "Institution",
			"Istituto", "Klinik", "LLC", "New York Presbyterian", "New York-Presbyterian", "NewYork Presbyterian",
			"NewYork-Presbyterian", "NYU ", "Pharmaceuticals", "School ", "Sloan Kettering", "Universidad",
			"Université", "University", "Weill");

	Map<String, List<ReCiterArticleFeedbackScore>> feedbackInstitutionMap = null;
	List<ReCiterAuthor> listOfAuthors = null;
	List<ReCiterArticle> validInstitutions = null;

	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {

		return 0.0;

	}

	private static int getMinPosition(int... positions) {
		int min = Integer.MAX_VALUE;
		for (int pos : positions) {
			if (pos != -1 && pos < min) {
				min = pos;
			}
		}
		return min == Integer.MAX_VALUE ? 600 : min;
	}

	private static boolean isValidInstitution(String component, int maxCharLength) {
		if (component.length() >= maxCharLength) {
			return false;
		}
		if (component.matches("^[0-9].*") || component.matches("and .*")) {
			return false;
		}
		if (component.startsWith("Department of") || component.startsWith("Division") || component.startsWith(")")) {
			return false;
		}
		for (String keyword : targetKeywords) {
			if (component.contains(keyword)) {
				return true;
			}
		}
		return false;
	}
	
	private List<String> sanitizeAffiliation(String affiliation) {

		List<String> institutionList = new ArrayList<>();
	    // Remove leading numbers and spaces
	    affiliation = affiliation.replaceAll("^[0-9 ]+", "");

	    // Count occurrences of patterns
	    Pattern capitalPattern = Pattern.compile("^[A-Z]\\.[A-Z]\\.");
	    Matcher capitalMatcher = capitalPattern.matcher(affiliation);
	    int capitalPatternCount = 0;

	    // Iterate over all matches for capital pattern
	    while (capitalMatcher.find()) {
	        capitalPatternCount++;
	    }

	    // Count occurrences of lowercase followed by uppercase
	    Pattern lowercaseUppercasePattern = Pattern.compile("[a-z][A-Z]");
	    Matcher lowercaseUppercaseMatcher = lowercaseUppercasePattern.matcher(affiliation);
	    int lowercaseUppercaseCount = 0;

	    // Iterate over all matches for lowercase-uppercase pattern
	    while (lowercaseUppercaseMatcher.find()) {
	        lowercaseUppercaseCount++;
	    }

	    // If the patterns are below the threshold, process the string
	    if (capitalPatternCount < 4 && lowercaseUppercaseCount < 3) {
	        int curPos = 0;
	        int charLength = affiliation.length();
	        int maxCharLength = 120;

	        while (curPos <= charLength) {
	            // Find the next delimiter position
	            int nextComma = affiliation.indexOf(',', curPos);
	            int nextPeriod = affiliation.indexOf('.', curPos);
	            int nextSemicolon = affiliation.indexOf(';', curPos);
	            int nextParenthesis = affiliation.indexOf('(', curPos);

	            int minPos = getMinPosition(nextComma, nextPeriod, nextSemicolon, nextParenthesis, charLength + 1);

	            String outerArticleInstitution;
	            if (minPos != charLength + 1) {
	                outerArticleInstitution = affiliation.substring(curPos, minPos).trim();
	                curPos = minPos + 1;
	            } else {
	                outerArticleInstitution = affiliation.substring(curPos).trim();
	                curPos = charLength + 1;
	            }

	            // Check if the extracted institution is valid
	            if (isValidInstitution(outerArticleInstitution, maxCharLength)) 
	            	institutionList.add(outerArticleInstitution);


	            // Exit loop if end of string is reached
	            if (curPos > charLength) {
	                break;
	            }
	        }
	    }

	    return institutionList;
	}

	

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {

		try {

			// Compute total accepted articles for informed absence penalty
			final int totalAccepted = (int) reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == ACCEPTED)
				.count();

			Map<String, Map<Integer, Long>> instCountsByArticleStatus = reCiterArticles.stream()
	        		 .filter(article -> article!=null && article.getArticleCoAuthors() !=null && article.getArticleCoAuthors().getAuthors()!=null && article.getArticleCoAuthors().getAuthors().size() > 0)
	                .flatMap(article -> article.getArticleCoAuthors().getAuthors().stream()
	                	 .filter(author-> author!=null && author.getAffiliation()!=null && !author.getAffiliation().isBlank() && author.isTargetAuthor() && author.getAffiliation().length() < 850)	
	                	 .flatMap(author -> {
	                    	List<String> institutionList = sanitizeAffiliation(author.getAffiliation());
		                        // Create a stream of (keyword, status) pairs
	                        return institutionList.stream()
	                            .map(institutionEntry -> new AbstractMap.SimpleEntry<>(institutionEntry, article.getGoldStandard()));
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

				   			feedbackInstitutionMap = new HashMap<>();   
									
								   article.getArticleCoAuthors().getAuthors().stream()
										.filter(author -> author!=null && author.isTargetAuthor() && author.getAffiliation()!=null && !author.getAffiliation().isBlank() && author.getAffiliation().length() < 850)
										.forEach(author -> {
											
											
											 
											 List<String> institutionList = sanitizeAffiliation(author.getAffiliation());
											
											institutionList.stream()
														   .filter(institution -> institution!=null && !institution.isBlank())
														   .forEach(institution -> {
															   
													 int countAccepted = 0;
													 int countRejected = 0;
													 double scoreAll = 0.0;
													 double scoreWithout1Accepted = 0.0;
													 double scoreWithout1Rejected = 0.0;		   
													
													if(instCountsByArticleStatus!=null && instCountsByArticleStatus.size() > 0)
													{
														if(instCountsByArticleStatus.containsKey(institution))
														{
															if(instCountsByArticleStatus.get(institution).containsKey(ACCEPTED))
																countAccepted = Math.toIntExact(instCountsByArticleStatus.get(institution).get(ACCEPTED));
															if(instCountsByArticleStatus.get(institution).containsKey(REJECTED))
																countRejected = Math.toIntExact(instCountsByArticleStatus.get(institution).get(REJECTED));
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
														
														ReCiterArticleFeedbackScore feedbackInst = populateArticleFeedbackScore(article.getArticleId(),institution,
																   countAccepted,countRejected,
																   scoreAll,scoreWithout1Accepted,
																   scoreWithout1Rejected,article.getGoldStandard(),feedbackScore,exportedFeedbackScore,"Institution");
														
														List<ReCiterArticleFeedbackScore> feedbackList = feedbackInstitutionMap.computeIfAbsent(institution, k -> new ArrayList<>());

														// Check if the list is empty or if the item does not exist in the list
														if (feedbackList.isEmpty()) {
														    feedbackList.add(feedbackInst);
														} else {
														    for (ReCiterArticleFeedbackScore feedbackInstitution : feedbackList) {
														        if (feedbackInstitution != null && !feedbackInstitution.getFeedbackScoreFieldValue().equalsIgnoreCase(feedbackInst.getFeedbackScoreFieldValue())) {
														            // If no match found, add the feedbackOrg
														            feedbackList.add(feedbackInst);
														            break; // Exit loop once item is added
														        }
														    }
														}

														

													}
												// Informed absence: institution never seen
												else if (countAccepted == 0 && countRejected == 0 && totalAccepted > 0) {
													double penalty = computeInformedAbsencePenalty(totalAccepted);
													String exportedFeedbackScore = decimalFormat.format(penalty);
													ReCiterArticleFeedbackScore feedbackInst = populateArticleFeedbackScore(article.getArticleId(),institution,
															   countAccepted,countRejected,
															   penalty,penalty,
															   penalty,article.getGoldStandard(),penalty,exportedFeedbackScore,"Institution");
													feedbackInstitutionMap.computeIfAbsent(institution, k -> new ArrayList<>()).add(feedbackInst);
												}

											});
										});
								   
								   double totalScore = feedbackInstitutionMap.values().stream().flatMap(List::stream) // Flatten the lists into a single stream of ReCiterFeedbackScoreCoAuthorName
											.mapToDouble(ReCiterArticleFeedbackScore::getFeedbackScore) // Extract the scores
											.sum(); // Sum all scores
									
									
									// Sort Map Contents before storing into another Map
								   feedbackInstitutionMap.entrySet().stream()
											.filter(entry -> entry.getKey() != null && entry.getValue() != null)
											.sorted(Map.Entry.comparingByKey())
											.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
													(oldValue, newValue) -> oldValue, // merge function
													LinkedHashMap::new // to maintain insertion order
											));
								   article.addArticleFeedbackScoresMap(feedbackInstitutionMap);
									article.setInstitutionFeedbackScore(totalScore);
									String exportedInstFeedbackScore = decimalFormat.format(totalScore); 
									article.setExportedInstitutionFeedbackScore(exportedInstFeedbackScore);
			   				});
					 
		

		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}
	
}
