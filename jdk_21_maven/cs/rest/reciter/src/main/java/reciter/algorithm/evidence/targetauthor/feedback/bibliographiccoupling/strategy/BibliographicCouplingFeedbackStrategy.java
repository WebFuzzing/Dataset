package reciter.algorithm.evidence.targetauthor.feedback.bibliographiccoupling.strategy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import reciter.algorithm.evidence.feedback.targetauthor.AbstractTargetAuthorFeedbackStrategy;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.identity.Identity;

public class BibliographicCouplingFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(BibliographicCouplingFeedbackStrategy.class);

	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		return 0.0;
	}

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		StopWatch stopWatch = new StopWatch("BibliographicCoupling");
		stopWatch.start("BibliographicCoupling");
		try {
			slf4jLogger.info("reCiterArticles size in bibliographic coupling: {}", reCiterArticles.size());

			// Total accepted articles for informed absence penalty
			final int totalAccepted = (int) reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == ACCEPTED)
				.count();

			// Pre-compute reference sets for all articles with gold standard feedback
			// Key: articleId, Value: set of referenced PMIDs
			Map<Long, Set<Long>> refSets = new HashMap<>();
			for (ReCiterArticle article : reCiterArticles) {
				if (article != null && article.getCommentsCorrectionsPmids() != null
						&& !article.getCommentsCorrectionsPmids().isEmpty()) {
					refSets.put(article.getArticleId(), new HashSet<>(article.getCommentsCorrectionsPmids()));
				}
			}

			// Separate accepted and rejected articles that have references
			List<ReCiterArticle> acceptedWithRefs = reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == ACCEPTED && refSets.containsKey(a.getArticleId()))
				.collect(Collectors.toList());

			List<ReCiterArticle> rejectedWithRefs = reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == REJECTED && refSets.containsKey(a.getArticleId()))
				.collect(Collectors.toList());

			// Score each article
			for (ReCiterArticle article : reCiterArticles) {
				if (article == null) continue;

				Map<String, List<ReCiterArticleFeedbackScore>> feedbackBibCouplingMap = new HashMap<>();

				Set<Long> refsX = refSets.get(article.getArticleId());

				if (refsX == null || refsX.isEmpty()) {
					// No references — apply informed absence penalty if enabled
					double scoreAll;
					if (totalAccepted > 0) {
						scoreAll = computeInformedAbsencePenalty(totalAccepted);
					} else {
						scoreAll = 0.0;
					}

					double feedbackScore = determineFeedbackScore(article.getGoldStandard(), scoreAll, scoreAll, scoreAll);
					String exportedFeedbackScore = decimalFormat.format(feedbackScore);

					article.setBibliographicCouplingFeedbackScore(feedbackScore);
					article.setExportedBibliographicCouplingFeedbackScore(exportedFeedbackScore);
					article.addArticleFeedbackScoresMap(feedbackBibCouplingMap);
					continue;
				}

				// Count accepted/rejected articles sharing at least 1 reference with X
				int countAccepted = 0;
				int countRejected = 0;

				for (ReCiterArticle accepted : acceptedWithRefs) {
					if (accepted.getArticleId() == article.getArticleId()) continue;
					Set<Long> refsY = refSets.get(accepted.getArticleId());
					if (hasIntersection(refsX, refsY)) {
						countAccepted++;
					}
				}

				for (ReCiterArticle rejected : rejectedWithRefs) {
					if (rejected.getArticleId() == article.getArticleId()) continue;
					Set<Long> refsY = refSets.get(rejected.getArticleId());
					if (hasIntersection(refsX, refsY)) {
						countRejected++;
					}
				}

				// Leave-one-out: if article X is accepted, it was counted above — subtract 1
				int adjustedCountAccepted = countAccepted;
				int adjustedCountRejected = countRejected;

				double scoreAll = computeScore(countAccepted, countRejected);
				if (countAccepted == 0 && countRejected == 0 && totalAccepted > 0) {
					scoreAll = computeInformedAbsencePenalty(totalAccepted);
				}

				double scoreWithout1Accepted = computeScore(
					countAccepted > 0 ? countAccepted - 1 : countAccepted,
					countRejected);
				double scoreWithout1Rejected = computeScore(
					countAccepted,
					countRejected > 0 ? countRejected - 1 : countRejected);

				double feedbackScore = determineFeedbackScore(
					article.getGoldStandard(), scoreWithout1Accepted, scoreWithout1Rejected, scoreAll);
				String exportedFeedbackScore = decimalFormat.format(feedbackScore);

				// Populate per-item feedback score record
				ReCiterArticleFeedbackScore bibCouplingScore = populateArticleFeedbackScore(
					article.getArticleId(),
					String.valueOf(countAccepted) + "a/" + String.valueOf(countRejected) + "r",
					countAccepted, countRejected,
					scoreAll, scoreWithout1Accepted, scoreWithout1Rejected,
					article.getGoldStandard(), feedbackScore, exportedFeedbackScore,
					"BibliographicCoupling");

				feedbackBibCouplingMap.merge(
					Long.toString(article.getArticleId()),
					new ArrayList<>(Arrays.asList(bibCouplingScore)),
					(existingList, newList) -> {
						existingList.addAll(newList);
						return existingList;
					});

				article.setBibliographicCouplingFeedbackScore(feedbackScore);
				article.setExportedBibliographicCouplingFeedbackScore(exportedFeedbackScore);
				article.addArticleFeedbackScoresMap(feedbackBibCouplingMap);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		stopWatch.stop();
		slf4jLogger.info(stopWatch.getId() + " took " + stopWatch.getTotalTimeSeconds() + "s");
		return 0;
	}

	/**
	 * Check whether two sets share at least one element, without allocating an intersection set.
	 */
	private boolean hasIntersection(Set<Long> a, Set<Long> b) {
		// Iterate over the smaller set for efficiency
		Set<Long> smaller = a.size() <= b.size() ? a : b;
		Set<Long> larger = a.size() <= b.size() ? b : a;
		for (Long id : smaller) {
			if (larger.contains(id)) {
				return true;
			}
		}
		return false;
	}
}
