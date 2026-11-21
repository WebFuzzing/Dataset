package reciter.algorithm.evidence.targetauthor.feedback.textsimilarity.strategy;

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

public class TextSimilarityFeedbackStrategy extends AbstractTargetAuthorFeedbackStrategy {

	private static final Logger slf4jLogger = LoggerFactory.getLogger(TextSimilarityFeedbackStrategy.class);

	private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
		"a", "about", "above", "after", "again", "against", "all", "am", "an", "and", "any", "are", "aren", "arent",
		"as", "at", "be", "because", "been", "before", "being", "below", "between", "both", "but", "by",
		"can", "cannot", "could", "couldn", "couldnt", "d", "did", "didn", "didnt", "do", "does", "doesn", "doesnt",
		"doing", "don", "dont", "down", "during", "each", "few", "for", "from", "further",
		"get", "got", "had", "hadn", "hadnt", "has", "hasn", "hasnt", "have", "haven", "havent", "having",
		"he", "her", "here", "hers", "herself", "him", "himself", "his", "how",
		"i", "if", "in", "into", "is", "isn", "isnt", "it", "its", "itself",
		"just", "ll", "m", "ma", "may", "me", "might", "mightn", "mightnt", "more", "most", "must", "mustn", "mustnt",
		"my", "myself", "need", "needn", "neednt", "no", "nor", "not", "now",
		"o", "of", "off", "on", "once", "only", "or", "other", "our", "ours", "ourselves", "out", "over", "own",
		"re", "s", "same", "shall", "shan", "shant", "she", "shes", "should", "shouldn", "shouldnt", "so", "some",
		"such", "t", "than", "that", "the", "their", "theirs", "them", "themselves", "then", "there", "these",
		"they", "this", "those", "through", "to", "too", "under", "until", "up", "upon",
		"us", "ve", "very", "was", "wasn", "wasnt", "we", "were", "weren", "werent",
		"what", "when", "where", "which", "while", "who", "whom", "why", "will", "with",
		"won", "wont", "would", "wouldn", "wouldnt", "y", "you", "your", "yours", "yourself", "yourselves",
		// Common biomedical stopwords
		"also", "however", "although", "using", "used", "use", "within", "without", "et", "al",
		"study", "studies", "result", "results", "conclusion", "conclusions", "method", "methods",
		"background", "objective", "objectives", "purpose", "aim", "aims",
		"patients", "patient", "group", "groups", "compared", "associated", "significantly",
		"analysis", "data", "based", "well", "whether", "among", "found", "showed", "observed"
	));

	private final StrategyParameters strategyParameters;

	public TextSimilarityFeedbackStrategy(StrategyParameters strategyParameters) {
		this.strategyParameters = strategyParameters;
	}

	@Override
	public double executeFeedbackStrategy(ReCiterArticle reCiterArticle, Identity identity) {
		return 0.0;
	}

	@Override
	public double executeFeedbackStrategy(List<ReCiterArticle> reCiterArticles, Identity identity) {
		StopWatch stopWatch = new StopWatch("textSimilarity");
		stopWatch.start("textSimilarity");
		try {
			// 1. Collect text for all articles
			Map<Long, List<String>> articleTokens = new HashMap<>();
			for (ReCiterArticle article : reCiterArticles) {
				if (article == null) continue;
				String text = getArticleText(article);
				if (text != null && !text.isBlank()) {
					articleTokens.put(article.getArticleId(), tokenize(text));
				}
			}

			// 2. Identify accepted articles that have text
			List<ReCiterArticle> acceptedWithText = reCiterArticles.stream()
				.filter(a -> a != null && a.getGoldStandard() == ACCEPTED && articleTokens.containsKey(a.getArticleId()))
				.collect(Collectors.toList());

			if (acceptedWithText.isEmpty()) {
				// No accepted articles with text — set all scores to 0
				for (ReCiterArticle article : reCiterArticles) {
					if (article == null) continue;
					article.setTextSimilarityFeedbackScore(0.0);
					article.setExportedTextSimilarityFeedbackScore("0");
				}
				return 0.0;
			}

			// 3. Build IDF from accepted articles only
			int nAccepted = acceptedWithText.size();
			Map<String, Integer> docFrequency = new HashMap<>();
			for (ReCiterArticle accepted : acceptedWithText) {
				Set<String> uniqueTerms = new HashSet<>(articleTokens.get(accepted.getArticleId()));
				for (String term : uniqueTerms) {
					docFrequency.merge(term, 1, Integer::sum);
				}
			}

			Map<String, Double> idf = new HashMap<>();
			for (Map.Entry<String, Integer> entry : docFrequency.entrySet()) {
				idf.put(entry.getKey(), Math.log((nAccepted + 1.0) / (entry.getValue() + 1.0)));
			}

			// 4. Compute TF-IDF vectors for all articles
			Map<Long, Map<String, Double>> tfidfVectors = new HashMap<>();
			for (Map.Entry<Long, List<String>> entry : articleTokens.entrySet()) {
				tfidfVectors.put(entry.getKey(), computeTfIdf(entry.getValue(), idf));
			}

			// 5. Precompute accepted portfolio centroid (full)
			Map<String, Double> fullCentroid = computeCentroid(
				acceptedWithText.stream()
					.map(a -> tfidfVectors.get(a.getArticleId()))
					.collect(Collectors.toList())
			);

			// 6. Score each article
			for (ReCiterArticle article : reCiterArticles) {
				if (article == null) continue;

				Map<String, Double> articleVector = tfidfVectors.get(article.getArticleId());
				if (articleVector == null || articleVector.isEmpty()) {
					article.setTextSimilarityFeedbackScore(0.0);
					article.setExportedTextSimilarityFeedbackScore("0");
					continue;
				}

				double cosineSim;
				if (article.getGoldStandard() == ACCEPTED) {
					// Leave-one-out: exclude this article from centroid
					Map<String, Double> loocentroid = computeCentroidExcluding(
						acceptedWithText, tfidfVectors, article.getArticleId());
					if (loocentroid == null || loocentroid.isEmpty()) {
						// Only one accepted article — no reference to compare against
						cosineSim = 0.0;
					} else {
						cosineSim = cosineSimilarity(articleVector, loocentroid);
					}
				} else {
					cosineSim = cosineSimilarity(articleVector, fullCentroid);
				}

				// Use raw cosine similarity — XGBoost learns the threshold
				double score = cosineSim;

				article.setTextSimilarityFeedbackScore(score);
				String exportedScore = decimalFormat.format(score);
				article.setExportedTextSimilarityFeedbackScore(exportedScore);

				// Build item-level feedback map
				Map<String, List<ReCiterArticleFeedbackScore>> feedbackMap = new HashMap<>();
				ReCiterArticleFeedbackScore feedbackScore = populateArticleFeedbackScore(
					article.getArticleId(),
					String.format("cosine=%.4f", cosineSim),
					0, 0,  // countAccepted/countRejected not applicable for similarity
					score, score, score,
					article.getGoldStandard(),
					score, exportedScore,
					"TextSimilarity"
				);
				List<ReCiterArticleFeedbackScore> scoreList = new ArrayList<>();
				scoreList.add(feedbackScore);
				feedbackMap.put("TextSimilarity", scoreList);
				article.addArticleFeedbackScoresMap(feedbackMap);
			}

		} catch (Exception e) {
			slf4jLogger.error("Error in TextSimilarityFeedbackStrategy", e);
		}
		stopWatch.stop();
		slf4jLogger.info(stopWatch.getId() + " took " + stopWatch.getTotalTimeSeconds() + "s");
		return 0;
	}

	private String getArticleText(ReCiterArticle article) {
		StringBuilder sb = new StringBuilder();
		if (article.getArticleTitle() != null) {
			sb.append(article.getArticleTitle());
		}
		if (article.getPublicationAbstract() != null) {
			if (sb.length() > 0) sb.append(" ");
			sb.append(article.getPublicationAbstract());
		}
		return sb.toString();
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

		// Term frequency
		Map<String, Integer> tf = new HashMap<>();
		for (String token : tokens) {
			tf.merge(token, 1, Integer::sum);
		}

		double totalTerms = tokens.size();
		Map<String, Double> tfidf = new HashMap<>();
		for (Map.Entry<String, Integer> entry : tf.entrySet()) {
			String term = entry.getKey();
			double termFreq = entry.getValue() / totalTerms;
			double idfValue = idf.getOrDefault(term, Math.log(2.0)); // default IDF for unseen terms
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

		// Iterate over the smaller map for efficiency
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
