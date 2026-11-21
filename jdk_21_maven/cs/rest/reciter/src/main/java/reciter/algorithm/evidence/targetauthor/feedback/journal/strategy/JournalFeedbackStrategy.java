package reciter.algorithm.evidence.targetauthor.feedback.journal.strategy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import reciter.algorithm.evidence.feedback.targetauthor.AbstractTargetAuthorFeedbackStrategy;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.identity.Identity;

public class JournalFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(JournalFeedbackStrategy.class);
	Map<String, List<ReCiterArticleFeedbackScore>> feedbackJournalsListMap = null;

	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {

		return 0.0;

	}

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {

		StopWatch stopWatchforArticleFeedbackScore = new StopWatch("Journal");
		stopWatchforArticleFeedbackScore.start("Journal");
		try {
			slf4jLogger.info("reCiterArticles size:", reCiterArticles.size());

			// Compute total accepted articles for informed absence penalty
			final int totalAccepted = (int) reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == ACCEPTED)
				.count();

			// Count articles based on status per journal title
	        Map<String, Map<Integer, Long>> journalTitleCountByArticleStatus = reCiterArticles.stream()
	        	.filter(article -> article!=null && article.getJournal()!=null  && article.getJournal().getJournalTitle()!=null && !article.getJournal().getJournalTitle().isBlank())	
	            .collect(Collectors.groupingBy(
	                article -> article.getJournal().getJournalTitle(),  // Group by journal title
	                Collectors.groupingBy(
	                    ReCiterArticle::getGoldStandard,    // Group by status
	                    Collectors.counting()   // Count articles
	                )
	            ));

	        reCiterArticles.stream()
	        .filter(article -> article!=null && article.getJournal()!=null  && article.getJournal().getJournalTitle()!=null && !article.getJournal().getJournalTitle().isBlank())	
			 	.forEach(article -> {
			 	
			 	int countAccepted = 0;
			 	int countRejected = 0;
				double scoreAll = 0.0;
				double scoreWithout1Accepted = 0.0;
				double scoreWithout1Rejected = 0.0;
				
				feedbackJournalsListMap = new HashMap<>();
				
				     if(journalTitleCountByArticleStatus !=null && journalTitleCountByArticleStatus.size() > 0 &&journalTitleCountByArticleStatus.containsKey(article.getJournal().getJournalTitle()) && journalTitleCountByArticleStatus.get(article.getJournal().getJournalTitle().trim()).containsKey(ACCEPTED))
				    	 countAccepted = Math.toIntExact(journalTitleCountByArticleStatus.get(article.getJournal().getJournalTitle().trim()).get(ACCEPTED).longValue());
					 if(journalTitleCountByArticleStatus!=null && journalTitleCountByArticleStatus.size()>0 && journalTitleCountByArticleStatus.containsKey(article.getJournal().getJournalTitle()) && journalTitleCountByArticleStatus.get(article.getJournal().getJournalTitle().trim()).containsKey(REJECTED))
						 countRejected = Math.toIntExact(journalTitleCountByArticleStatus.get(article.getJournal().getJournalTitle().trim()).get(REJECTED).longValue());
							
					if(countAccepted > 0 || countRejected > 0)
					{	
						scoreAll = computeScore(countAccepted, countRejected);
						scoreWithout1Accepted = computeScore(countAccepted > 0 ? countAccepted - 1 : countAccepted,
								countRejected);
						scoreWithout1Rejected = computeScore(countAccepted,
								countRejected > 0 ? countRejected - 1 : countRejected);
	
						
						double feedbackScore= determineFeedbackScore(article.getGoldStandard(),scoreWithout1Accepted, scoreWithout1Rejected, scoreAll);
						String exportedFeedbackScore = decimalFormat.format(feedbackScore);
						
						ReCiterArticleFeedbackScore feedbackJournal = populateArticleFeedbackScore(article.getArticleId(),article.getJournal().getJournalTitle(),
																								   countAccepted,countRejected,
																								   scoreAll,scoreWithout1Accepted,
																								   scoreWithout1Rejected,article.getGoldStandard(),feedbackScore,exportedFeedbackScore,"Journal");	
	
						feedbackJournalsListMap.computeIfAbsent(article.getJournal().getJournalTitle().trim(), k -> new ArrayList<>()).add(feedbackJournal);
						
						feedbackJournalsListMap.entrySet().stream()
								.filter(entry -> entry.getKey() != null && entry.getValue() != null)
								.sorted(Map.Entry.comparingByKey())
								.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
										(oldValue, newValue) -> oldValue, // merge function
										LinkedHashMap::new // to maintain insertion order
								));
		
						article.addArticleFeedbackScoresMap(feedbackJournalsListMap);
						article.setJournalFeedackScore(feedbackJournal.getFeedbackScore());	
						String exportedJournalFeedackScore = decimalFormat.format(feedbackJournal.getFeedbackScore()); 
						article.setExportedJournalFeedackScore(exportedJournalFeedackScore);
				 }
				// Informed absence: journal never seen in accepted or rejected
				else if (countAccepted == 0 && countRejected == 0 && totalAccepted > 0) {
					double penalty = computeInformedAbsencePenalty(totalAccepted);
					String exportedFeedbackScore = decimalFormat.format(penalty);
					ReCiterArticleFeedbackScore feedbackJournal = populateArticleFeedbackScore(article.getArticleId(),article.getJournal().getJournalTitle(),
																							   countAccepted,countRejected,
																							   penalty,penalty,
																							   penalty,article.getGoldStandard(),penalty,exportedFeedbackScore,"Journal");
					feedbackJournalsListMap.computeIfAbsent(article.getJournal().getJournalTitle().trim(), k -> new ArrayList<>()).add(feedbackJournal);
					article.addArticleFeedbackScoresMap(feedbackJournalsListMap);
					article.setJournalFeedackScore(feedbackJournal.getFeedbackScore());
					String exportedJournalFeedackScore = decimalFormat.format(feedbackJournal.getFeedbackScore());
					article.setExportedJournalFeedackScore(exportedJournalFeedackScore);
			 }
			});
			} catch (Exception e) {
				e.printStackTrace();
			}
		stopWatchforArticleFeedbackScore.stop();
			slf4jLogger.info(stopWatchforArticleFeedbackScore.getId() + " took "
					+ stopWatchforArticleFeedbackScore.getTotalTimeSeconds() + "s"); 

		return 0.0;
	}

	
}
