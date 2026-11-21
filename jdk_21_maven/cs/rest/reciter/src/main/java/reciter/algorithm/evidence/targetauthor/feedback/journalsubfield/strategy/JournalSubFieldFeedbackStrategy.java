package reciter.algorithm.evidence.targetauthor.feedback.journalsubfield.strategy;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reciter.algorithm.evidence.feedback.targetauthor.AbstractTargetAuthorFeedbackStrategy;
import reciter.database.dynamodb.model.ScienceMetrix;
import reciter.engine.EngineParameters;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.identity.Identity;
import reciter.model.pubmed.MedlineCitationJournalISSN;

public class JournalSubFieldFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(JournalSubFieldFeedbackStrategy.class);
	
	private Map<String, List<ReCiterArticleFeedbackScore>> feedbackJournalsSubFieldMap = new HashMap<>();
	private int totalAccepted = 0;
	
	Set<String> filterJournalSubFields = Stream.of("General Science & Technology")
            .collect(Collectors.toSet());
	
	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {

		return 0.0;

	}
	

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		try {
	        slf4jLogger.info("reCiterArticles size: {}", reCiterArticles.size());

	        // Compute total accepted articles for informed absence penalty
	        totalAccepted = (int) reCiterArticles.stream()
	            .filter(a -> a != null && a.getGoldStandard() == ACCEPTED)
	            .count();

	        // Group by gold standard
	        Map<Integer, List<ReCiterArticle>> groupedByGoldStandard = reCiterArticles.stream()
	                .collect(Collectors.groupingBy(ReCiterArticle::getGoldStandard));

	        // Get accepted and rejected articles
	        List<ReCiterArticle> acceptedArticles = groupedByGoldStandard.getOrDefault(1, Collections.emptyList());
	        List<ReCiterArticle> rejectedArticles = groupedByGoldStandard.getOrDefault(-1, Collections.emptyList());

	        // Prepare counts for accepted and rejected articles
	        Map<String, Long> acceptArticlesCountByJournalSubField = countArticlesByJournalSubField(acceptedArticles);
	        Map<String, Long> rejectedArticlesCountByJournalSubField = countArticlesByJournalSubField(rejectedArticles);

	        // Process each article — resolve one subfield per article to avoid double-counting
	        feedbackJournalsSubFieldMap = reCiterArticles.parallelStream()
	                .filter(article -> article != null && article.getJournal() != null && article.getJournal().getJournalIssn() != null && !article.getJournal().getJournalIssn().isEmpty())
	                .map(article -> processArticle(article, acceptArticlesCountByJournalSubField, rejectedArticlesCountByJournalSubField))
	                .filter(Objects::nonNull)
	                .collect(Collectors.groupingBy(ReCiterArticleFeedbackScore::getFeedbackScoreType));
	        
	        
	        // Calculate total scores
	        double totalScore = feedbackJournalsSubFieldMap.values().stream()
	                .flatMap(List::stream)
	                .mapToDouble(ReCiterArticleFeedbackScore::getFeedbackScore)
	                .sum();

	        // Finalize feedback scores
	        reCiterArticles.parallelStream().forEach(article -> {
	            double articleScore = feedbackJournalsSubFieldMap.values().stream()
	                    .flatMap(List::stream)
	                    .filter(feedback -> feedback.getArticleId() == article.getArticleId())
	                    .mapToDouble(ReCiterArticleFeedbackScore::getFeedbackScore)
	                    .sum();

	            article.setJournalSubFieldFeedbackScore(articleScore);
	            article.setExportedJournalSubFieldFeedbackScore(decimalFormat.format(articleScore));
	        });

	        return totalScore;
	    } catch (Exception e) {
	        e.printStackTrace();
	    }
	    return 0;

	}
	private Map<String, Long> countArticlesByJournalSubField(List<ReCiterArticle> articles) {
	    return articles.parallelStream()
	            .filter(article -> article.getJournal() != null && article.getJournal().getJournalIssn() != null && !article.getJournal().getJournalIssn().isEmpty())
	            .map(article -> resolveJournalSubField(article.getJournal().getJournalIssn()))
	            .filter(Objects::nonNull)
	            .filter(subField -> !subField.isBlank())
	            .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
	}

	private ReCiterArticleFeedbackScore processArticle(ReCiterArticle article,
	                                                   Map<String, Long> acceptArticlesCountByJournalSubField,
	                                                   Map<String, Long> rejectedArticlesCountByJournalSubField) {

	    String journalSubField = resolveJournalSubField(article.getJournal().getJournalIssn());
	    if (journalSubField == null || journalSubField.isBlank() || filterJournalSubFields.contains(journalSubField)) {
	        return null; // Skip if no valid subfield
	    }

	    int countAccepted = Math.toIntExact(acceptArticlesCountByJournalSubField.getOrDefault(journalSubField, 0L));
	    int countRejected =  Math.toIntExact(rejectedArticlesCountByJournalSubField.getOrDefault(journalSubField, 0L));

	    // Compute the scores
	    double scoreAll = computeScore(countAccepted, countRejected);
	    double scoreWithout1Accepted = computeScore(countAccepted > 0 ? countAccepted - 1 : countAccepted, countRejected);
	    double scoreWithout1Rejected = computeScore(countAccepted, countRejected > 0 ? countRejected - 1 : countRejected);

	    // Informed absence: journal subfield never seen in accepted or rejected
	    if (countAccepted == 0 && countRejected == 0 && totalAccepted > 0) {
	        double penalty = computeInformedAbsencePenalty(totalAccepted);
	        scoreAll = penalty;
	        scoreWithout1Accepted = penalty;
	        scoreWithout1Rejected = penalty;
	    }

	    double feedbackScore = determineFeedbackScore(article.getGoldStandard(), scoreWithout1Accepted, scoreWithout1Rejected, scoreAll);
	    String exportedFeedbackScore = decimalFormat.format(feedbackScore);

	    // Create the feedback object
	    return populateArticleFeedbackScore(
	            article.getArticleId(),
	            journalSubField,
	            Math.toIntExact(countAccepted),
	            Math.toIntExact(countRejected),
	            scoreAll,
	            scoreWithout1Accepted,
	            scoreWithout1Rejected,
	            article.getGoldStandard(),
	            feedbackScore,
	            exportedFeedbackScore,
	            "Journal SubField"
	    );
	}

	/**
	 * Resolve a single journal subfield from an article's ISSN list using type priority:
	 * Linking > Print > Electronic. For each ISSN, tries both SM.issn and SM.eissn fields.
	 * This prevents double-counting when multiple ISSNs resolve to the same journal.
	 */
	private String resolveJournalSubField(List<MedlineCitationJournalISSN> journalIssns) {
	    if (journalIssns == null || journalIssns.isEmpty()) {
	        return null;
	    }

	    String issnPrint = null;
	    String issnElectronic = null;
	    String issnLinking = null;

	    for (MedlineCitationJournalISSN journalIssn : journalIssns) {
	        if (journalIssn == null || journalIssn.getIssntype() == null || journalIssn.getIssn() == null) {
	            continue;
	        }
	        if (journalIssn.getIssntype().equalsIgnoreCase("Print")) {
	            issnPrint = journalIssn.getIssn().trim();
	        } else if (journalIssn.getIssntype().equalsIgnoreCase("Electronic")) {
	            issnElectronic = journalIssn.getIssn().trim();
	        } else {
	            issnLinking = journalIssn.getIssn().trim();
	        }
	    }

	    // Try ISSNs in priority order: Linking > Print > Electronic
	    ScienceMetrix scienceMetrix = findScienceMetrixByIssn(issnLinking);
	    if (scienceMetrix == null) {
	        scienceMetrix = findScienceMetrixByIssn(issnPrint);
	    }
	    if (scienceMetrix == null) {
	        scienceMetrix = findScienceMetrixByIssn(issnElectronic);
	    }

	    return (scienceMetrix != null && scienceMetrix.getScienceMetrixSubfield() != null && !scienceMetrix.getScienceMetrixSubfield().isBlank())
	            ? scienceMetrix.getScienceMetrixSubfield()
	            : null;
	}

	/**
	 * Look up a single ISSN value against the in-memory ScienceMetrix journal list,
	 * trying both the issn and eissn fields.
	 */
	private ScienceMetrix findScienceMetrixByIssn(String issn) {
	    if (issn == null || issn.isBlank()) {
	        return null;
	    }
	    for (ScienceMetrix smJournal : EngineParameters.getScienceMetrixJournals()) {
	        if (smJournal.getIssn() != null && smJournal.getIssn().equals(issn)) {
	            return smJournal;
	        }
	        if (smJournal.getEissn() != null && smJournal.getEissn().equals(issn)) {
	            return smJournal;
	        }
	    }
	    return null;
	}
}
