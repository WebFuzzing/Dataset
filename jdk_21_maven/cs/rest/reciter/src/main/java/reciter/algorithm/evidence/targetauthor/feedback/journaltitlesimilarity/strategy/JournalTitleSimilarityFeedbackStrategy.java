package reciter.algorithm.evidence.targetauthor.feedback.journaltitlesimilarity.strategy;

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
import reciter.engine.StrategyParameters;
import reciter.model.article.ReCiterArticle;
import reciter.model.article.ReCiterArticleFeedbackScore;
import reciter.model.identity.Identity;

public class JournalTitleSimilarityFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(JournalTitleSimilarityFeedbackStrategy.class);

	// Stopwords for journal titles — common words that don't carry topical signal
	private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
		"a", "an", "and", "the", "of", "for", "in", "on", "to", "at", "by",
		"or", "its", "is", "are", "was", "with", "from", "as", "into",
		// Common journal title filler words
		"journal", "international", "annals", "archives", "proceedings",
		"transactions", "bulletin", "reports", "letters", "reviews", "review",
		"current", "new", "american", "european", "british", "canadian",
		"world", "global", "general", "annual", "quarterly", "monthly",
		"open", "access", "online", "research", "science", "sciences",
		"studies", "advances", "frontiers", "trends", "progress", "acta"
	));

	private final StrategyParameters strategyParameters;

	public JournalTitleSimilarityFeedbackStrategy(StrategyParameters strategyParameters) {
		this.strategyParameters = strategyParameters;
	}

	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		return 0.0;
	}

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		StopWatch stopWatch = new StopWatch("journalTitleSimilarity");
		stopWatch.start("journalTitleSimilarity");
		try {
			// 1. Collect journal titles for all articles
			Map<Long, String> articleJournalTitles = new HashMap<>();
			for (ReCiterArticle article : reCiterArticles) {
				if (article == null) continue;
				String journalTitle = getJournalTitle(article);
				if (journalTitle != null && !journalTitle.isBlank()) {
					articleJournalTitles.put(article.getArticleId(), journalTitle);
				}
			}

			// 2. Tokenize all journal titles
			Map<Long, List<String>> articleTokens = new HashMap<>();
			for (Map.Entry<Long, String> entry : articleJournalTitles.entrySet()) {
				List<String> tokens = tokenize(entry.getValue());
				if (!tokens.isEmpty()) {
					articleTokens.put(entry.getKey(), tokens);
				}
			}

			// 3. Identify accepted articles with journal title tokens
			List<ReCiterArticle> acceptedWithTokens = reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == ACCEPTED && articleTokens.containsKey(a.getArticleId()))
				.collect(Collectors.toList());

			if (acceptedWithTokens.isEmpty()) {
				for (ReCiterArticle article : reCiterArticles) {
					if (article == null) continue;
					article.setJournalTitleSimilarityFeedbackScore(0.0);
					article.setExportedJournalTitleSimilarityFeedbackScore("0");
				}
				return 0.0;
			}

			// 4. Build IDF from distinct accepted journal titles
			//    Deduplicate: if multiple accepted articles share the same journal,
			//    count that journal title once for IDF purposes
			Set<String> seenJournalTitles = new HashSet<>();
			List<List<String>> distinctAcceptedTokenLists = new ArrayList<>();
			Map<String, List<String>> journalTitleToTokens = new HashMap<>();

			for (ReCiterArticle accepted : acceptedWithTokens) {
				String jTitle = articleJournalTitles.get(accepted.getArticleId());
				String normalizedTitle = jTitle.toLowerCase().trim();
				if (!seenJournalTitles.contains(normalizedTitle)) {
					seenJournalTitles.add(normalizedTitle);
					List<String> tokens = articleTokens.get(accepted.getArticleId());
					distinctAcceptedTokenLists.add(tokens);
					journalTitleToTokens.put(normalizedTitle, tokens);
				}
			}

			int nDistinctJournals = distinctAcceptedTokenLists.size();
			Map<String, Integer> docFrequency = new HashMap<>();
			for (List<String> tokenList : distinctAcceptedTokenLists) {
				Set<String> uniqueTerms = new HashSet<>(tokenList);
				for (String term : uniqueTerms) {
					docFrequency.merge(term, 1, Integer::sum);
				}
			}

			Map<String, Double> idf = new HashMap<>();
			for (Map.Entry<String, Integer> entry : docFrequency.entrySet()) {
				idf.put(entry.getKey(), Math.log((nDistinctJournals + 1.0) / (entry.getValue() + 1.0)));
			}

			// 5. Compute TF-IDF vectors for all articles
			Map<Long, Map<String, Double>> tfidfVectors = new HashMap<>();
			for (Map.Entry<Long, List<String>> entry : articleTokens.entrySet()) {
				tfidfVectors.put(entry.getKey(), computeTfIdf(entry.getValue(), idf));
			}

			// 6. Precompute accepted portfolio centroid (using all distinct journal TF-IDF vectors)
			List<Map<String, Double>> allAcceptedVectors = acceptedWithTokens.stream()
				.map(a -> tfidfVectors.get(a.getArticleId()))
				.filter(v -> v != null)
				.collect(Collectors.toList());
			Map<String, Double> fullCentroid = computeCentroid(allAcceptedVectors);

			// 7. Score each article
			for (ReCiterArticle article : reCiterArticles) {
				if (article == null) continue;

				Map<String, Double> articleVector = tfidfVectors.get(article.getArticleId());
				if (articleVector == null || articleVector.isEmpty()) {
					article.setJournalTitleSimilarityFeedbackScore(0.0);
					article.setExportedJournalTitleSimilarityFeedbackScore("0");
					continue;
				}

				double cosineSim;
				if (article.getGoldStandard() == ACCEPTED) {
					// Leave-one-out: exclude this article from centroid
					Map<String, Double> looCentroid = computeCentroidExcluding(
						acceptedWithTokens, tfidfVectors, article.getArticleId());
					if (looCentroid == null || looCentroid.isEmpty()) {
						cosineSim = 0.0;
					} else {
						cosineSim = cosineSimilarity(articleVector, looCentroid);
					}
				} else {
					cosineSim = cosineSimilarity(articleVector, fullCentroid);
				}

				double score = cosineSim;

				article.setJournalTitleSimilarityFeedbackScore(score);
				String exportedScore = decimalFormat.format(score);
				article.setExportedJournalTitleSimilarityFeedbackScore(exportedScore);

				// Build item-level feedback map
				Map<String, List<ReCiterArticleFeedbackScore>> feedbackMap = new HashMap<>();
				String journalTitle = articleJournalTitles.getOrDefault(article.getArticleId(), "");
				ReCiterArticleFeedbackScore feedbackScore = populateArticleFeedbackScore(
					article.getArticleId(),
					String.format("%s (cosine=%.4f)", journalTitle, cosineSim),
					0, 0,
					score, score, score,
					article.getGoldStandard(),
					score, exportedScore,
					"JournalTitleSimilarity"
				);
				List<ReCiterArticleFeedbackScore> scoreList = new ArrayList<>();
				scoreList.add(feedbackScore);
				feedbackMap.put("JournalTitleSimilarity", scoreList);
				article.addArticleFeedbackScoresMap(feedbackMap);
			}

		} catch (Exception e) {
			slf4jLogger.error("Error in JournalTitleSimilarityFeedbackStrategy", e);
		}
		stopWatch.stop();
		slf4jLogger.info(stopWatch.getId() + " took " + stopWatch.getTotalTimeSeconds() + "s");
		return 0;
	}

	private String getJournalTitle(ReCiterArticle article) {
		if (article.getJournal() != null && article.getJournal().getJournalTitle() != null) {
			return article.getJournal().getJournalTitle().trim();
		}
		return null;
	}

	private List<String> tokenize(String text) {
		String lower = text.toLowerCase();
		String[] tokens = lower.split("[^a-z0-9]+");
		List<String> result = new ArrayList<>();
		for (String token : tokens) {
			if (token.length() > 1 && !STOP_WORDS.contains(token)) {
				result.add(token);
			}
		}
		return result;
	}

	private Map<String, Double> computeTfIdf(List<String> tokens, Map<String, Double> idf) {
		if (tokens.isEmpty()) return new HashMap<>();

		Map<String, Integer> tf = new HashMap<>();
		for (String token : tokens) {
			tf.merge(token, 1, Integer::sum);
		}

		double totalTerms = tokens.size();
		Map<String, Double> tfidf = new HashMap<>();
		for (Map.Entry<String, Integer> entry : tf.entrySet()) {
			String term = entry.getKey();
			double termFreq = entry.getValue() / totalTerms;
			double idfValue = idf.getOrDefault(term, Math.log(2.0));
			tfidf.put(term, termFreq * idfValue);
		}
		return tfidf;
	}

	private Map<String, Double> computeCentroid(List<Map<String, Double>> vectors) {
		if (vectors.isEmpty()) return new HashMap<>();

		Map<String, Double> centroid = new HashMap<>();
		for (Map<String, Double> vec : vectors) {
			for (Map.Entry<String, Double> entry : vec.entrySet()) {
				centroid.merge(entry.getKey(), entry.getValue(), Double::sum);
			}
		}
		double n = vectors.size();
		centroid.replaceAll((k, v) -> v / n);
		return centroid;
	}

	private Map<String, Double> computeCentroidExcluding(
			List<ReCiterArticle> acceptedArticles,
			Map<Long, Map<String, Double>> tfidfVectors,
			long excludeArticleId) {

		List<Map<String, Double>> vectors = acceptedArticles.stream()
			.filter(a -> a.getArticleId() != excludeArticleId)
			.map(a -> tfidfVectors.get(a.getArticleId()))
			.filter(v -> v != null)
			.collect(Collectors.toList());

		return computeCentroid(vectors);
	}

	private double cosineSimilarity(Map<String, Double> a, Map<String, Double> b) {
		double dotProduct = 0.0;
		double normA = 0.0;
		double normB = 0.0;

		Map<String, Double> smaller = a.size() <= b.size() ? a : b;
		Map<String, Double> larger = a.size() <= b.size() ? b : a;

		for (Map.Entry<String, Double> entry : smaller.entrySet()) {
			Double bVal = larger.get(entry.getKey());
			if (bVal != null) {
				dotProduct += entry.getValue() * bVal;
			}
		}

		for (double val : a.values()) normA += val * val;
		for (double val : b.values()) normB += val * val;

		if (normA == 0.0 || normB == 0.0) return 0.0;
		return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
	}
}
