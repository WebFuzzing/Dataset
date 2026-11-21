package reciter.algorithm.evidence.targetauthor.feedback.orcid.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import reciter.algorithm.evidence.feedback.targetauthor.AbstractTargetAuthorFeedbackStrategy;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleAuthors;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.article.ReCiterAuthor;
import reciter.model.identity.Identity;

public class OrcidFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(OrcidFeedbackStrategy.class);
	Map<String, List<ReCiterArticleFeedbackScore>> feedbackOrcidMap = null;
	List<ReCiterAuthor> listOfAuthors = null;
	

	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {

		return 0.0;

	}
	
	public static Map<String, Long> countCoAuthorsByOrcid(List<ReCiterArticle> articles, int articleStatus) {
        return articles.stream()
                .filter(article -> article!=null && article.getGoldStandard() == articleStatus)
                .map(ReCiterArticle::getArticleCoAuthors)
                .filter(Objects::nonNull) 
                .map(ReCiterArticleAuthors::getAuthors)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .filter(author -> author != null && author.getOrcid() != null && !author.getOrcid().isBlank() && author.isTargetAuthor())
                .collect(Collectors.groupingBy(
                        ReCiterAuthor::getOrcid,
                        Collectors.counting()
                ));
    }

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		StopWatch stopWatchforOrcidFeedback = new StopWatch("Orcid");
		stopWatchforOrcidFeedback.start("Orcid");
		try {
			slf4jLogger.info("reCiterArticles size: ", reCiterArticles.size());

			// Compute total accepted articles for informed absence penalty
			final int totalAccepted = (int) reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == ACCEPTED)
				.count();

			List<ReCiterArticle> filteredArticles = reCiterArticles.stream()
					.filter(article -> article.getGoldStandard() != 0)
					 .collect(Collectors.toList());
		
			//Count co-authors grouped by ORCID for accepted articles
			Map<String, Long> acceptedCounts = countCoAuthorsByOrcid(filteredArticles, ACCEPTED);
	        
	        // Count co-authors grouped by ORCID for rejected articles
	        Map<String, Long> rejectedCounts = countCoAuthorsByOrcid(filteredArticles, REJECTED);
			
	        reCiterArticles.stream()
			.filter(article-> article!=null && article.getArticleCoAuthors()!=null && article.getArticleCoAuthors().getAuthors()!=null && article.getArticleCoAuthors().getAuthors().size()>0)
					.forEach(article->{
						listOfAuthors  = article.getArticleCoAuthors().getAuthors();
						feedbackOrcidMap = new HashMap<>();
					
				listOfAuthors.stream()
							.filter(author->author != null && author.isTargetAuthor() && author.getOrcid() != null && !author.getOrcid().isBlank() )
							.forEach(author -> {

					 int countAccepted = 0;
					 int countRejected = 0;
					 double scoreAll = 0.0;
					 double scoreWithout1Accepted = 0.0;
					 double scoreWithout1Rejected = 0.0;
					
					if(acceptedCounts!=null && acceptedCounts.size() >0 && acceptedCounts.containsKey(author.getOrcid()))
						countAccepted = Math.toIntExact(acceptedCounts.get(author.getOrcid()));
	
					if(rejectedCounts!=null && rejectedCounts.size() > 0 && rejectedCounts.containsKey(author.getOrcid()))
						countRejected = Math.toIntExact(rejectedCounts.get(author.getOrcid()));
					
							scoreAll = computeScore(countAccepted, countRejected);
							scoreWithout1Accepted = computeScore(countAccepted > 0 ? countAccepted - 1 : countAccepted,
									countRejected);
							scoreWithout1Rejected = computeScore(countAccepted,
									countRejected > 0 ? countRejected - 1 : countRejected);

							// Informed absence: orcid never seen in accepted or rejected
							if (countAccepted == 0 && countRejected == 0 && totalAccepted > 0) {
								double penalty = computeInformedAbsencePenalty(totalAccepted);
								scoreAll = penalty;
								scoreWithout1Accepted = penalty;
								scoreWithout1Rejected = penalty;
							}

							double feedbackScore= determineFeedbackScore(article.getGoldStandard(),scoreWithout1Accepted, scoreWithout1Rejected, scoreAll);
							String exportedFeedbackScore = decimalFormat.format(feedbackScore);
							
							ReCiterArticleFeedbackScore feedbackScoreOrcid = populateArticleFeedbackScore(article.getArticleId(),author.getOrcid(),
									   countAccepted,countRejected,
									   scoreAll,scoreWithout1Accepted,
									   scoreWithout1Rejected,article.getGoldStandard(),feedbackScore,exportedFeedbackScore, "Orcid");	
		
							feedbackOrcidMap.computeIfAbsent(author.getOrcid(), k -> new ArrayList<>()).add(feedbackScoreOrcid);	
							

				});
				double totalScore = feedbackOrcidMap.values().stream().flatMap(List::stream) // Flatten the lists into a single stream of ReCiterFeedbackScoreCoAuthorName
						.mapToDouble(score-> score.getFeedbackScore()) // Extract the scores
						.sum(); // Sum all scores*/

				// Sort Map Contents before storing into another Map
				feedbackOrcidMap.entrySet().stream()
						.filter(entry -> entry.getKey() != null && entry.getValue() != null)
						.sorted(Map.Entry.comparingByKey())
						.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
								(oldValue, newValue) -> oldValue, // merge function
								LinkedHashMap::new // to maintain insertion order
						));
				article.addArticleFeedbackScoresMap(feedbackOrcidMap);
				article.setOrcidFeedbackScore(totalScore);
				String exportedOrcidFeedbackScore = decimalFormat.format(totalScore);
				article.setExportedOrcidFeedbackScore(exportedOrcidFeedbackScore);
			});

		} catch (Exception e) {
			e.printStackTrace();
		}
		stopWatchforOrcidFeedback.stop();
		slf4jLogger.info(stopWatchforOrcidFeedback.getId() + " took "
				+ stopWatchforOrcidFeedback.getTotalTimeSeconds() + "s"); 
		return 0;
	}
}
